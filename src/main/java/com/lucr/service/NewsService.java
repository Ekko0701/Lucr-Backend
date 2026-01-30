package com.lucr.service;

import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 뉴스 비즈니스 로직 서비스 인터페이스
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
public interface NewsService {

    /**
     * 새로운 뉴스 생성
     *
     * @param request 뉴스 생성 요청 DTO
     * @return 생성된 뉴스의 상세 정보
     */
    NewsDetailResponse createNews(NewsCreateRequest request);

    /**
     * 뉴스 ID로 단건 조회 (상세 정보)
     *
     * @param id 뉴스 ID
     * @return 뉴스 상세 정보
     * @throws RuntimeException 뉴스를 찾을 수 없는 경우
     */
    NewsDetailResponse getNewsById(UUID id);

    /**
     * 뉴스 목록 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 페이징된 뉴스 목록
     */
    PageResponse<NewsResponse> getAllNews(Pageable pageable);

    /**
     * 뉴스 수정
     *
     * @param id 수정할 뉴스 ID
     * @param request 수정 요청 DTO
     * @return 수정된 뉴스의 상세 정보
     * @throws RuntimeException 뉴스를 찾을 수 없는 경우
     */
    NewsDetailResponse updateNews(UUID id, NewsUpdateRequest request);

    /**
     * 뉴스 삭제
     *
     * @param id 삭제할 뉴스 ID
     * @throws RuntimeException 뉴스를 찾을 수 없는 경우
     */
    void deleteNews(UUID id);

    /**
     * 뉴스 검색 (복합 조건, 페이징)
     *
     * @param searchRequest 검색 조건 DTO
     * @return 검색 결과 페이징 목록
     */
    PageResponse<NewsResponse> searchNews(NewsSearchRequest searchRequest);

    /**
     * 뉴스 조회수 증가
     *
     * @param id 뉴스 ID
     * @return 조회수가 증가된 뉴스 상세 정보
     * @throws RuntimeException 뉴스를 찾을 수 없는 경우
     */
    NewsDetailResponse incrementViewCount(UUID id);

    /**
     * 인기 뉴스 목록 조회 (조회수 높은 순)
     *
     * @param pageable 페이징 정보
     * @return 인기 뉴스 페이징 목록
     */
    PageResponse<NewsResponse> getHighViewNews(Pageable pageable);

    /**
     * 최신 뉴스 목록 조회 (생성일 최신순)
     *
     * @param pageable 페이징 정보
     * @return 최신 뉴스 페이징 목록
     */
    PageResponse<NewsResponse> getRecentNews(Pageable pageable);

    /**
     * URL 중복 체크
     *
     * @param url 확인할 URL
     * @return 중복 여부 (true: 중복됨, false: 중복 안됨)
     */
    boolean existsByUrl(String url);

    /**
     * 특정 출처의 뉴스 목록 조회
     *
     * @param source 뉴스 출처
     * @param pageable 페이징 정보
     * @return 해당 출처의 뉴스 페이징 목록
     */
    PageResponse<NewsResponse> getNewsBySource(String source, Pageable pageable);

    /**
     * 키워드로 뉴스 검색 (제목 + 본문)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 페이징 목록
     */
    PageResponse<NewsResponse> searchByKeyword(String keyword, Pageable pageable);
}
