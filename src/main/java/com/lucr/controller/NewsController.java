package com.lucr.controller;

import static com.lucr.config.openapi.OpenApiConstants.FORBIDDEN_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.INVALID_TYPE_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.MISSING_PARAMETER_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.UNAUTHORIZED_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.VALIDATION_ERROR_RESPONSE_REF;

import com.lucr.common.ApiResponse;
import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.exception.ErrorResponse;
import com.lucr.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 뉴스 REST API 컨트롤러
 *
 * @author kimdongjoo
 * @since 2026-01-28
 */
@Tag(name = "뉴스", description = "뉴스 CRUD, 검색, 인기/최신 조회")
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
    @Operation(summary = "뉴스 생성", description = "새 뉴스 기사를 등록합니다. Python 크롤러에서 호출됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "뉴스 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = VALIDATION_ERROR_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복된 URL (E409002)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
     * 뉴스 단건 조회 (상세 정보 + 조회수 자동 기록)
     *
     * @param id      뉴스 ID
     * @param request HTTP 요청 (IP, 인증 정보 추출용)
     * @return 200 OK + 뉴스 상세 정보
     */
    @Operation(summary = "뉴스 단건 조회", description = "뉴스 ID로 상세 정보를 조회합니다. 조회수가 자동으로 기록됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "뉴스를 찾을 수 없음 (E404002)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNews(
            @PathVariable UUID id,
            HttpServletRequest request) {
        log.info("뉴스 조회 요청: id={}", id);

        String viewerKey = extractViewerKey(request);
        NewsDetailResponse data = newsService.getNewsById(id, viewerKey);

        log.info("뉴스 조회 완료: id={}, title={}", data.getId(), data.getTitle());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 뉴스 목록 조회 (페이징)
     *
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 200 OK + 뉴스 목록 (페이징)
     */
    @Operation(summary = "뉴스 목록 조회", description = "전체 뉴스를 페이징하여 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF)
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getAllNews(
            @ParameterObject
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
    @Operation(summary = "뉴스 수정", description = "뉴스 ID로 기사를 수정합니다. null이 아닌 필드만 업데이트됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 또는 뉴스 ID 타입 오류 (E400001, E400002)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "뉴스를 찾을 수 없음 (E404002)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
    @Operation(summary = "뉴스 삭제", description = "뉴스 ID로 기사를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "뉴스를 찾을 수 없음 (E404002)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
    @Operation(summary = "인기 뉴스 조회", description = "조회수가 높은 순으로 뉴스를 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF)
    })
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getPopularNews(
            @ParameterObject
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
    @Operation(summary = "최신 뉴스 조회", description = "생성일 기준 최신순으로 뉴스를 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF)
    })
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getRecentNews(
            @ParameterObject
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
    @Operation(summary = "키워드 검색", description = "키워드로 뉴스를 검색합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = MISSING_PARAMETER_RESPONSE_REF)
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> searchByKeyword(
            @RequestParam String keyword,
            @ParameterObject
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
    @Operation(summary = "고급 검색", description = "키워드, 출처, 최소 조회수, 감정 점수, 날짜 등 복합 조건으로 뉴스를 검색합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = VALIDATION_ERROR_RESPONSE_REF)
    })
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
    @Operation(summary = "출처별 뉴스 조회", description = "뉴스 출처(예: NAVER_FINANCE, DAUM_FINANCE)별로 뉴스를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF)
    })
    @GetMapping("/source/{source}")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getNewsBySource(
            @PathVariable String source,
            @ParameterObject
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("출처별 뉴스 조회 요청: source={}", source);

        PageResponse<NewsResponse> data = newsService.getNewsBySource(source, pageable);

        log.info("출처별 뉴스 조회 완료: source={}, totalElements={}", source, data.getTotalElements());
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
    @Operation(summary = "URL 중복 확인", description = "뉴스 URL이 이미 등록되어 있는지 확인합니다. 크롤러 중복 체크용입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = MISSING_PARAMETER_RESPONSE_REF)
    })
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkUrlExists(@RequestParam String url) {
        log.info("URL 중복 확인 요청: url={}", url);

        boolean exists = newsService.existsByUrl(url);
        String message = exists ?
                "이미 존재하는 URL입니다." : "사용 가능한 URL입니다.";

        log.info("URL 중복 확인 완료: url={}, exists={}", url, exists);
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }

    // ========== 내부 헬퍼 ==========

    /**
     * 조회 주체 키 추출
     *
     * <p>로그인 사용자면 "user:{username}", 비로그인이면 "ip:{IP}"를 반환한다.</p>
     */
    private String extractViewerKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }
}
