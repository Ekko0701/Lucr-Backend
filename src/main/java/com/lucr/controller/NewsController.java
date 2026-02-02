package com.lucr.controller;

import com.lucr.common.ApiResponse;
import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 뉴스 REST API 컨트롤러
 *
 * @author kimdongjoo
 * @since 2026-01-28
 */
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    // ========== CRUD 엔드포인트 ==========

    /**
     * 뉴스 생성 (Python FastAPI → Spring)
     *
     * @param request 뉴스 생성 요청
     * @return 201 Created + 생성된 뉴스 상세 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NewsDetailResponse>> createNews(
            @Valid @RequestBody NewsCreateRequest request
    ) {
        log.info("뉴스 생성 요청: title={}, source={}, url={}",
                request.getTitle(), request.getSource(), request.getUrl());

        NewsDetailResponse data = newsService.createNews(request);

        log.info("뉴스 생성 완료: id={}", data.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("뉴스가 성공적으로 생성되었습니다.", data));
    }

    /**
     * 뉴스 단건 조회 (상세 정보)
     *
     * @param id 뉴스 ID
     * @return 200 OK + 뉴스 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNews(@PathVariable UUID id) {
        log.info("뉴스 조회 요청: id={}", id);

        NewsDetailResponse data = newsService.getNewsById(id);

        log.info("뉴스 조회 완료: id={}, title={}", data.getId(), data.getTitle());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 뉴스 목록 조회 (페이징)
     *
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 200 OK + 뉴스 목록 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getAllNews(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("뉴스 목록 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<NewsResponse> data = newsService.getAllNews(pageable);

        log.info("뉴스 목록 조회 완료: totalElements={}, totalPages={}",
                data.getTotalElements(), data.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 뉴스 수정
     *
     * @param id 수정할 뉴스 ID
     * @param request 수정 요청 (null이 아닌 필드만 업데이트)
     * @return 200 OK + 수정된 뉴스 상세 정보
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> updateNews(
            @PathVariable UUID id,
            @Valid @RequestBody NewsUpdateRequest request
    ) {
        log.info("뉴스 수정 요청: id={}", id);

        NewsDetailResponse data = newsService.updateNews(id, request);

        log.info("뉴스 수정 완료: id={}", data.getId());
        return ResponseEntity.ok(ApiResponse.success("뉴스가 성공적으로 수정되었습니다.", data));
    }

    /**
     * 뉴스 삭제
     *
     * @param id 삭제할 뉴스 ID
     * @return 200 OK + 삭제 성공 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable UUID id) {
        log.info("뉴스 삭제 요청: id={}", id);

        newsService.deleteNews(id);

        log.info("뉴스 삭제 완료: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("뉴스가 성공적으로 삭제되었습니다."));
    }

    // ========== 목록 조회 엔드포인트 ==========

    /**
     * 인기 뉴스 목록 조회 (조회수 높은 순)
     *
     * @param pageable 페이징 정보
     * @return 200 OK + 인기 뉴스 목록 (페이징)
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getPopularNews(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("인기 뉴스 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<NewsResponse> data = newsService.getHighViewNews(pageable);

        log.info("인기 뉴스 조회 완료: totalElements={}", data.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 최신 뉴스 목록 조회 (생성일 최신순)
     *
     * @param pageable 페이징 정보
     * @return 200 OK + 최신 뉴스 목록 (페이징)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getRecentNews(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("최신 뉴스 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<NewsResponse> data = newsService.getRecentNews(pageable);

        log.info("최신 뉴스 조회 완료: totalElements={}", data.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ========== 검색 엔드포인트 ==========

    /**
     * 키워드 검색 (간단)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 200 OK + 검색 결과 (페이징)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> searchByKeyword(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("키워드 검색 요청: keyword={}", keyword);

        PageResponse<NewsResponse> data = newsService.searchByKeyword(keyword, pageable);

        log.info("키워드 검색 완료: keyword={}, totalElements={}", keyword, data.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 고급 검색 (복합 조건)
     *
     * @param searchRequest 검색 조건 (keyword, source, minViewCount, sentimentScore, date 등)
     * @return 200 OK + 검색 결과 (페이징)
     */
    @PostMapping("/search/advanced")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> advancedSearch(
            @Valid @RequestBody NewsSearchRequest searchRequest
    ) {
        log.info("고급 검색 요청: keyword={}, source={}, minViewCount={}",
                searchRequest.getKeyword(), searchRequest.getSource(), searchRequest.getMinViewCount());

        PageResponse<NewsResponse> data = newsService.searchNews(searchRequest);

        log.info("고급 검색 완료: totalElements={}", data.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 출처별 뉴스 목록 조회
     *
     * @param source 뉴스 출처 (예: NAVER_FINANCE, DAUM_FINANCE)
     * @param pageable 페이징 정보
     * @return 200 OK + 해당 출처의 뉴스 목록 (페이징)
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getNewsBySource(
            @PathVariable String source,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("출처별 뉴스 조회 요청: source={}", source);

        PageResponse<NewsResponse> data = newsService.getNewsBySource(source, pageable);

        log.info("출처별 뉴스 조회 완료: source={}, totalElements={}", source, data.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ========== 특수 기능 엔드포인트 ==========

    /**
     * 조회수 증가
     *
     * 클라이언트가 뉴스 클릭 시 호출
     * - 조회수 증가 + 상세 정보 반환 (1번의 요청으로 처리)
     *
     * @param id 뉴스 ID
     * @return 200 OK + 조회수가 증가된 뉴스 상세 정보
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> incrementViewCount(@PathVariable UUID id) {
        log.info("조회수 증가 요청: id={}", id);

        NewsDetailResponse data = newsService.incrementViewCount(id);

        log.info("조회수 증가 완료: id={}, newCount={}", data.getId(), data.getViewCount());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * URL 중복 확인
     *
     * Python 크롤러가 뉴스 저장 전 중복 체크
     *
     * @param url 확인할 URL
     * @return 200 OK + 중복 여부
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkUrlExists(@RequestParam String url) {
        log.info("URL 중복 확인 요청: url={}", url);

        boolean exists = newsService.existsByUrl(url);
        String message = exists ?
                "이미 존재하는 URL입니다." : "사용 가능한 URL입니다.";

        log.info("URL 중복 확인 완료: url={}, exists={}", url, exists);
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }
}
