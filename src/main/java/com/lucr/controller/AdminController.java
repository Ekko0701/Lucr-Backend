package com.lucr.controller;

import static com.lucr.config.openapi.OpenApiConstants.FORBIDDEN_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.INVALID_TYPE_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.UNAUTHORIZED_RESPONSE_REF;

import com.lucr.common.ApiResponse;
import com.lucr.dto.response.CrawlJobResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import com.lucr.exception.ErrorResponse;
import com.lucr.messaging.CrawlJobPublisher;
import com.lucr.service.CrawlJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리자 API 컨트롤러 - 크롤링 작업 관리
 *
 * 역할:
 *   관리자가 크롤링을 트리거하고, 작업 상태를 조회하는 엔드포인트 제공
 *
 * 흐름:
 *   POST /admin/crawl/trigger 호출
 *     → CrawlJobService.createJob()     : DB에 PENDING 작업 생성
 *     → CrawlJobPublisher.publish()     : RabbitMQ에 요청 메시지 발행
 *     → 클라이언트에 jobId 즉시 반환     : 비동기 처리이므로 바로 응답
 *
 * @author Ekko0701
 * @since 2026-02-06
 */
@Tag(name = "관리자", description = "크롤링 트리거, 작업 상태/이력 조회")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CrawlJobService crawlJobService;
    private final CrawlJobPublisher crawlJobPublisher;

    // ========== 크롤링 트리거 ==========

    /**
     * 크롤링 작업 트리거
     *
     * 1. DB에 CrawlJob 생성 (PENDING)
     * 2. RabbitMQ에 크롤링 요청 메시지 발행
     * 3. jobId를 즉시 반환 (비동기 처리)
     *
     * @param maxArticles 언론사당 최대 수집 기사 수 (기본값: 50)
     * @return 201 Created + 생성된 작업 정보
     */
    @Operation(summary = "크롤링 트리거", description = "크롤링 작업을 생성하고 RabbitMQ에 요청을 발행합니다. ADMIN 권한이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "크롤링 작업 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 실행 중인 크롤링 작업 존재 (E409003)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/crawl/trigger")
    public ResponseEntity<ApiResponse<CrawlJobResponse>> triggerCrawl(
            @RequestParam(defaultValue = "50") int maxArticles
    ) {
        log.info("크롤링 트리거 요청: maxArticles={}", maxArticles);

        // 1. DB에 크롤링 작업 생성 (PENDING 상태)
        CrawlJob job = crawlJobService.createJob();

        // 2. RabbitMQ에 크롤링 요청 메시지 발행
        crawlJobPublisher.publish(job.getId(), maxArticles);

        // 3. Entity → DTO 변환 후 응답
        CrawlJobResponse response = CrawlJobResponse.from(job);

        log.info("크롤링 트리거 완료: jobId={}, status={}", job.getId(), job.getStatus());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("크롤링 작업이 시작되었습니다.", response));
    }

    // ========== 작업 상태 조회 ==========

    /**
     * 크롤링 작업 상태 조회
     *
     * 클라이언트가 트리거 후 반환받은 jobId로 진행 상태를 폴링
     *
     * @param jobId 작업 UUID
     * @return 200 OK + 작업 상태 정보
     */
    @Operation(summary = "작업 상태 조회", description = "크롤링 작업 ID로 진행 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "작업을 찾을 수 없음 (E404003)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/crawl/jobs/{jobId}")
    public ResponseEntity<ApiResponse<CrawlJobResponse>> getJobStatus(@PathVariable UUID jobId) {
        log.info("크롤링 작업 상태 조회: jobId={}", jobId);

        CrawlJob job = crawlJobService.getJobById(jobId);
        CrawlJobResponse response = CrawlJobResponse.from(job);

        log.info("크롤링 작업 상태 조회 완료: jobId={}, status={}", jobId, job.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 작업 이력 조회 ==========

    /**
     * 크롤링 작업 이력 조회 (전체 또는 상태별 필터, 페이징)
     *
     * 사용 예시:
     *   GET /api/v1/admin/crawl/jobs                    → 전체 이력 (최신순)
     *   GET /api/v1/admin/crawl/jobs?status=COMPLETED   → 완료된 작업만
     *   GET /api/v1/admin/crawl/jobs?status=completed   → 대소문자 무관
     *   GET /api/v1/admin/crawl/jobs?status=FAILED      → 실패한 작업만
     *   GET /api/v1/admin/crawl/jobs?page=1&size=10     → 2페이지, 10개씩
     *
     * @param status   상태 필터 (선택, PENDING/RUNNING/COMPLETED/FAILED, 대소문자 무관)
     * @param pageable 페이징 정보 (기본 size=20, 최신순)
     * @return 200 OK + 페이징된 작업 이력
     */
    @Operation(summary = "작업 이력 조회", description = "크롤링 작업 이력을 조회합니다. status 파라미터로 상태별 필터링이 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF)
    })
    @GetMapping("/crawl/jobs")
    public ResponseEntity<ApiResponse<PageResponse<CrawlJobResponse>>> getCrawlJobs(
            @RequestParam(required = false) CrawlJobStatus status,
            @ParameterObject
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("크롤링 작업 이력 조회: status={}, page={}, size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<CrawlJobResponse> response;
        if (status != null) {
            response = crawlJobService.getJobsByStatus(status, pageable);
        } else {
            response = crawlJobService.getAllJobs(pageable);
        }

        log.info("크롤링 작업 이력 조회 완료: totalElements={}", response.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
