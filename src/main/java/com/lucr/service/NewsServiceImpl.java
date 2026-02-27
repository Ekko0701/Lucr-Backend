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

import static com.lucr.config.CacheConstants.VIEW_COUNT_DB_PREFIX;

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
     * 뉴스 ID로 단건 조회 (상세 정보 + 조회수 자동 기록)
     *
     * <p>콘텐츠 캐시와 조회수를 분리하여 관리한다.</p>
     * <ol>
     *   <li>콘텐츠 캐시 조회 (news::UUID)</li>
     *   <li>캐시 미스 시 DB 조회 + dbcount 초기화 (SET NX) + 콘텐츠 캐시 저장</li>
     *   <li>중복 방지 + 조회수 증가 (항상 실행)</li>
     *   <li>실시간 조회수 반영 (dbcount + Redis 증가분)</li>
     * </ol>
     *
     * @param id        조회할 뉴스 ID
     * @param viewerKey 로그인 시 "user:{userId}", 비로그인 시 "ip:{IP}"
     * @return 뉴스 상세 정보 (실시간 조회수 포함)
     * @throws ResourceNotFoundException 뉴스를 찾을 수 없는 경우
     */
    @Override
    public NewsDetailResponse getNewsById(UUID id, String viewerKey) {
        log.debug("뉴스 조회 요청: id={}", id);

        // ① 콘텐츠 캐시 조회
        String cacheKey = CacheConstants.NEWS + "::" + id;
        NewsDetailResponse response = null;
        try {
            response = (NewsDetailResponse) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("캐시 조회 실패, DB 조회로 진행: id={}", id, e);
        }

        if (response == null) {
            // ② 캐시 미스: DB 조회
            News news = newsRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.newsNotFound(id.toString()));
            response = newsMapper.toDetailResponse(news);

            // ③ dbcount 초기화 (최초 1회, SET NX — 동시 요청 안전)
            redisTemplate.opsForValue()
                    .setIfAbsent(VIEW_COUNT_DB_PREFIX + id, (long) news.getViewCount());

            // ④ 콘텐츠 캐시 저장 (viewCount가 포함되어 저장되지만, ⑥번에서 항상 실시간 값으로 덮어쓰므로 무시됨)
            try {
                redisTemplate.opsForValue().set(cacheKey, response,
                        CacheConstants.NEWS_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("캐시 저장 실패: id={}", id, e);
            }
        }

        // ⑤ 중복 방지 + 조회수 증가 (항상 실행, 캐시 히트여도)
        viewCountService.recordView(id, viewerKey);

        // ⑥ 실시간 조회수 반영 (dbcount + Redis 증가분)
        long realViewCount = viewCountService.getViewCount(id);
        response.setViewCount((int) realViewCount);

        log.debug("실시간 조회수 반영: id={}, realViewCount={}", id, realViewCount);

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
