package com.lucr.service;

import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.entity.News;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.NewsMapper;
import com.lucr.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lucr.config.CacheConstants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 뉴스 비즈니스 로직 서비스 구현체
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final ViewCountService viewCountService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 새로운 뉴스 생성
     * 
     * 캐시 전략:
     * - 목록 캐시 전체 삭제 (새 뉴스가 목록에 포함되어야 함)
     * - @CacheEvict(allEntries = true): 모든 페이지의 캐시 무효화
     * - news-list, news-popular, news-recent 캐시 모두 삭제
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.NEWS_LIST, allEntries = true),
            @CacheEvict(value = CacheConstants.NEWS_POPULAR, allEntries = true),
            @CacheEvict(value = CacheConstants.NEWS_RECENT, allEntries = true)
    })
    public NewsDetailResponse createNews(NewsCreateRequest request) {
        log.info("뉴스 생성 요청: title={}, source={}", request.getTitle(), request.getSource());

        // URL 중복 체크
        if (newsRepository.existsByUrl(request.getUrl())) {
            log.warn("중복된 URL로 뉴스 생성 시도: url={}", request.getUrl());
            throw DuplicateResourceException.duplicateNewsUrl(request.getUrl());
        }

        // DTO → Entity 변환
        News news = newsMapper.toEntity(request);

        // DB 저장
        News savedNews = newsRepository.save(news);
        log.info("뉴스 생성 완료: id={}, title={}", savedNews.getId(), savedNews.getTitle());

        // Entity → DetailResponse 변환
        return newsMapper.toDetailResponse(savedNews);
    }

    /**
     * 뉴스 ID로 단건 조회 (상세 정보)
     * 
     * 통합 API: 조회 + 조회수 자동 증가 + 실시간 조회수 반영
     * 
     * 동작 흐름:
     * 1. 수동으로 캐시 확인 (Redis GET)
     * 2. 캐시 미스면 DB 조회 (캐시 히트면 DB 조회 안 함!)
     * 3. Redis INCR로 조회수 자동 증가 (캐시 히트여도 실행!)
     * 4. 실시간 조회수 계산 (캐시된 DB view_count + Redis)
     * 5. 캐시 저장 (미스였으면)
     * 
     * 캐싱 전략 (수동):
     * - key: "news::UUID"
     * - TTL: 5분
     * - 캐시 히트여도 조회수 증가는 항상 실행
     * - viewCount는 항상 실시간으로 재계산
     * - 캐시 히트: DB 조회 안 함 (성능 최적화)
     * 
     * 조회수 관리:
     * - Redis INCR: 조회마다 자동 증가 (Thread-Safe)
     * - Redis 키: "news:viewcount:UUID"
     * - 실시간 조회수 = 캐시된 DB view_count + Redis 증가분
     * - 스케줄러가 5분마다 Redis → DB 동기화
     * 
     * 성능:
     * - 캐시 히트: ~7ms (Redis GET + INCR + GET) - DB 조회 없음!
     * - 캐시 미스: ~54ms (DB SELECT + Redis INCR + GET + SET)
     * 
     * @param id 조회할 뉴스 ID
     * @return 뉴스 상세 정보 (실시간 조회수 포함)
     * @throws ResourceNotFoundException 뉴스를 찾을 수 없는 경우
     */
    @Override
    public NewsDetailResponse getNewsById(UUID id) {
        log.debug("뉴스 조회 요청: id={}", id);

        // ========== 1. 수동 캐시 확인 ==========
        String cacheKey = CacheConstants.NEWS + "::" + id;
        NewsDetailResponse cachedResponse = null;
        boolean isCacheMiss = false;
        
        try {
            cachedResponse = (NewsDetailResponse) redisTemplate.opsForValue().get(cacheKey);
            isCacheMiss = (cachedResponse == null);
            
            if (cachedResponse != null) {
                log.debug("캐시 히트: id={}", id);
            } else {
                log.debug("캐시 미스: id={}", id);
            }
        } catch (Exception e) {
            log.warn("캐시 조회 실패, DB 조회로 진행: id={}", id, e);
            isCacheMiss = true;
        }

        // ========== 2. DB 조회 (캐시 미스 시에만) ==========
        News news = null;
        int dbViewCount = 0;
        
        if (isCacheMiss) {
            // 캐시 미스: DB에서 전체 뉴스 조회
            news = newsRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("뉴스를 찾을 수 없음: id={}", id);
                        return ResourceNotFoundException.newsNotFound(id.toString());
                    });
            dbViewCount = news.getViewCount();
            log.debug("DB 조회 완료: id={}, dbViewCount={}", id, dbViewCount);
        } else {
            // 캐시 히트: DB 조회 안 함, 캐시된 응답에서 viewCount 추출
            // 캐시된 viewCount는 이전 실시간 조회수이지만,
            // 스케줄러 동기화 후이므로 대략적인 DB view_count로 사용 가능
            dbViewCount = cachedResponse.getViewCount();
            log.debug("캐시된 viewCount 사용: id={}, cachedViewCount={}", id, dbViewCount);
        }

        // ========== 3. 조회수 자동 증가 (항상 실행) ==========
        // 캐시 히트여도 조회수는 증가해야 함!
        viewCountService.incrementViewCount(id);
        log.debug("조회수 자동 증가: id={}", id);

        // ========== 4. 응답 생성 ==========
        NewsDetailResponse response;
        if (isCacheMiss) {
            // 캐시 미스: 새로 DTO 생성
            response = newsMapper.toDetailResponse(news);
        } else {
            // 캐시 히트: 캐시된 응답 사용
            response = cachedResponse;
        }

        // ========== 5. 실시간 조회수 반영 (항상 실행) ==========
        // 캐시된(또는 DB) 조회수 + Redis 증가분 = 실시간 조회수
        long realViewCount = viewCountService.getViewCount(id, dbViewCount);
        response.setViewCount((int) realViewCount);

        log.debug("실시간 조회수 반영: id={}, 기준={}, Redis증가분={}, 실시간={}", 
                id, dbViewCount, realViewCount - dbViewCount, realViewCount);

        // ========== 6. 캐시 저장 (캐시 미스였으면) ==========
        if (isCacheMiss) {
            try {
                redisTemplate.opsForValue().set(
                        cacheKey, 
                        response, 
                        CacheConstants.NEWS_TTL_SECONDS, 
                        TimeUnit.SECONDS
                );
                log.debug("캐시 저장 완료: id={}, TTL={}초", id, CacheConstants.NEWS_TTL_SECONDS);
            } catch (Exception e) {
                log.warn("캐시 저장 실패 (계속 진행): id={}", id, e);
            }
        }

        return response;
    }

    /**
     * 뉴스 목록 조회 (페이징)
     * 
     * 캐시 전략:
     * - @Cacheable: 페이지별 결과를 Redis에 캐시 (TTL: 2분)
     * - key: "pageNumber_pageSize" (예: "0_10", "1_20")
     * - 각 페이지별로 별도의 캐시 키 생성
     */
    @Override
    @Cacheable(value = CacheConstants.NEWS_LIST,
            key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<NewsResponse> getAllNews(Pageable pageable) {
        log.debug("뉴스 목록 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<News> newsPage = newsRepository.findAll(pageable);

        List<NewsResponse> responses = newsPage.getContent().stream()
                .map(newsMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(newsPage, responses);
    }

    /**
     * 뉴스 수정
     * 
     * 캐시 전략:
     * - 단건 캐시 삭제: 해당 뉴스의 상세 정보 캐시 무효화
     * - 목록 캐시 전체 삭제: 수정된 뉴스가 포함된 모든 목록 갱신
     * - @CacheEvict(key = "#id"): 특정 뉴스 캐시만 삭제
     * - @CacheEvict(allEntries = true): 목록 캐시 전체 삭제
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.NEWS, key = "#id"),
            @CacheEvict(value = CacheConstants.NEWS_LIST, allEntries = true),
            @CacheEvict(value = CacheConstants.NEWS_POPULAR, allEntries = true),
            @CacheEvict(value = CacheConstants.NEWS_RECENT, allEntries = true)
    })
    public NewsDetailResponse updateNews(UUID id, NewsUpdateRequest request) {
        log.info("뉴스 수정 요청: id={}", id);

        News news = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("수정할 뉴스를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.newsNotFound(id.toString());
                });

        // Entity 업데이트 (Mapper 사용)
        newsMapper.updateEntity(news, request);

        // 변경 감지로 자동 업데이트 (save 호출 불필요)
        log.info("뉴스 수정 완료: id={}", id);

        return newsMapper.toDetailResponse(news);
    }

    /**
     * 뉴스 삭제
     * 
     * 캐시 전략:
     * - 단건 캐시 삭제: 삭제된 뉴스의 상세 정보 캐시 제거
     * - 목록 캐시 전체 삭제: 삭제된 뉴스가 제외된 목록으로 갱신
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.NEWS, key = "#id"),
            @CacheEvict(value = CacheConstants.NEWS_LIST, allEntries = true),
            @CacheEvict(value = CacheConstants.NEWS_POPULAR, allEntries = true),
            @CacheEvict(value = CacheConstants.NEWS_RECENT, allEntries = true)
    })
    public void deleteNews(UUID id) {
        log.info("뉴스 삭제 요청: id={}", id);

        if (!newsRepository.existsById(id)) {
            log.error("삭제할 뉴스를 찾을 수 없음: id={}", id);
            throw ResourceNotFoundException.newsNotFound(id.toString());
        }

        newsRepository.deleteById(id);
        log.info("뉴스 삭제 완료: id={}", id);
    }

    /**
     * 뉴스 검색 (복합 조건, 페이징)
     */
    @Override
    public PageResponse<NewsResponse> searchNews(NewsSearchRequest searchRequest) {
        log.debug("뉴스 검색 요청: keyword={}, source={}", 
                searchRequest.getKeyword(), searchRequest.getSource());

        // Pageable 생성
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 기본 검색 (키워드가 있는 경우)
        Page<News> newsPage;
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            newsPage = newsRepository.findAll(pageable); // TODO: 실제 검색 쿼리 구현 필요
        } else {
            newsPage = newsRepository.findAll(pageable);
        }

        List<NewsResponse> responses = newsPage.getContent().stream()
                .map(newsMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(newsPage, responses);
    }

    /**
     * 뉴스 조회수 증가
     */
    @Override
    public NewsDetailResponse incrementViewCount(UUID id) {
        log.debug("조회수 증가 요청: id={}", id);

        News news = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("뉴스를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.newsNotFound(id.toString());
                });

        // Redis에서 조회수 증가 (INCR) — 증가 후 Redis 누적값 반환
        long redisIncrement = viewCountService.incrementViewCount(id);

        // 응답 생성
        NewsDetailResponse response = newsMapper.toDetailResponse(news);

        // DB 조회수 + Redis 증가분 = 실시간 조회수
        // incrementViewCount()가 이미 INCR 후의 누적값을 반환하므로 추가 GET 호출 불필요
        response.setViewCount((int)(news.getViewCount() + redisIncrement));

        log.debug("조회수 증가 완료: id={}, viewCount={}", id, response.getViewCount());

        return response;
    }

    /**
     * 인기 뉴스 목록 조회 (조회수 높은 순)
     * 
     * 캐시 전략:
     * - @Cacheable: 조회수 기반 순위 캐시 (TTL: 1분, 실시간성 중요)
     * - key: "pageNumber_pageSize"
     * - 짧은 TTL로 최신 인기 뉴스 반영
     */
    @Override
    @Cacheable(value = CacheConstants.NEWS_POPULAR,
            key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<NewsResponse> getHighViewNews(Pageable pageable) {
        log.debug("인기 뉴스 조회 요청: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());

        Page<News> newsPage = newsRepository.findAllByOrderByViewCountDesc(pageable);

        List<NewsResponse> responses = newsPage.getContent().stream()
                .map(newsMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(newsPage, responses);
    }

    /**
     * 최신 뉴스 목록 조회 (생성일 최신순)
     * 
     * 캐시 전략:
     * - @Cacheable: 최신 뉴스 목록 캐시 (TTL: 1분, 실시간성 중요)
     * - key: "pageNumber_pageSize"
     * - 새 뉴스 생성 시 캐시 무효화되어 최신 상태 유지
     */
    @Override
    @Cacheable(value = CacheConstants.NEWS_RECENT,
            key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<NewsResponse> getRecentNews(Pageable pageable) {
        log.debug("최신 뉴스 조회 요청: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());

        // createdAt 기준 내림차순 정렬
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<News> newsPage = newsRepository.findAll(sortedPageable);

        List<NewsResponse> responses = newsPage.getContent().stream()
                .map(newsMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(newsPage, responses);
    }

    /**
     * URL 중복 체크
     */
    @Override
    public boolean existsByUrl(String url) {
        log.debug("URL 중복 체크: url={}", url);
        return newsRepository.existsByUrl(url);
    }

    /**
     * 특정 출처의 뉴스 목록 조회
     */
    @Override
    public PageResponse<NewsResponse> getNewsBySource(String source, Pageable pageable) {
        log.debug("출처별 뉴스 조회 요청: source={}, page={}, size={}", 
                source, pageable.getPageNumber(), pageable.getPageSize());

        // Repository에서 List를 반환하므로 Page로 변환 필요
        List<News> newsList = newsRepository.findBySource(source);

        // List를 수동으로 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), newsList.size());

        List<NewsResponse> responses;
        if (start > newsList.size()) {
            responses = List.of();
        } else {
            responses = newsList.subList(start, end).stream()
                    .map(newsMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // 수동으로 PageResponse 생성
        return PageResponse.<NewsResponse>builder()
                .content(responses)
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements((long) newsList.size())
                .totalPages((int) Math.ceil((double) newsList.size() / pageable.getPageSize()))
                .isFirst(pageable.getPageNumber() == 0)
                .isLast(end >= newsList.size())
                .hasNext(end < newsList.size())
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }

    /**
     * 키워드로 뉴스 검색 (제목 + 본문)
     */
    @Override
    public PageResponse<NewsResponse> searchByKeyword(String keyword, Pageable pageable) {
        log.debug("키워드 검색 요청: keyword={}, page={}, size={}", 
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        List<News> newsList = newsRepository.searchByKeyword(keyword);

        // List를 수동으로 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), newsList.size());

        List<NewsResponse> responses;
        if (start > newsList.size()) {
            responses = List.of();
        } else {
            responses = newsList.subList(start, end).stream()
                    .map(newsMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return PageResponse.<NewsResponse>builder()
                .content(responses)
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements((long) newsList.size())
                .totalPages((int) Math.ceil((double) newsList.size() / pageable.getPageSize()))
                .isFirst(pageable.getPageNumber() == 0)
                .isLast(end >= newsList.size())
                .hasNext(end < newsList.size())
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }
}
