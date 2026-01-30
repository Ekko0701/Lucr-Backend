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

    /**
     * 새로운 뉴스 생성
     */
    @Override
    @Transactional
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
     */
    @Override
    public NewsDetailResponse getNewsById(UUID id) {
        log.debug("뉴스 조회 요청: id={}", id);

        News news = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("뉴스를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.newsNotFound(id.toString());
                });

        return newsMapper.toDetailResponse(news);
    }

    /**
     * 뉴스 목록 조회 (페이징)
     */
    @Override
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
     */
    @Override
    @Transactional
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
     */
    @Override
    @Transactional
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
    @Transactional
    public NewsDetailResponse incrementViewCount(UUID id) {
        log.debug("조회수 증가 요청: id={}", id);

        News news = newsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("뉴스를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.newsNotFound(id.toString());
                });

        // 조회수 증가 (Entity의 비즈니스 로직 메서드 사용)
        news.incrementViewCount();
        log.debug("조회수 증가 완료: id={}, viewCount={}", id, news.getViewCount());

        return newsMapper.toDetailResponse(news);
    }

    /**
     * 인기 뉴스 목록 조회 (조회수 높은 순)
     */
    @Override
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
     */
    @Override
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
