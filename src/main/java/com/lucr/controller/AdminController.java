package com.lucr.controller;

import com.lucr.common.ApiResponse;
import com.lucr.dto.response.CrawlJobResponse;
import com.lucr.entity.CrawlJob;
import com.lucr.messaging.CrawlJobPublisher;
import com.lucr.service.CrawlJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @GetMapping("/crawl/jobs/{jobId}")
    public ResponseEntity<ApiResponse<CrawlJobResponse>> getJobStatus(@PathVariable UUID jobId) {
        log.info("크롤링 작업 상태 조회: jobId={}", jobId);

        CrawlJob job = crawlJobService.getJobById(jobId);
        CrawlJobResponse response = CrawlJobResponse.from(job);

        log.info("크롤링 작업 상태 조회 완료: jobId={}, status={}", jobId, job.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
