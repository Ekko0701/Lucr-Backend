package com.lucr.service;

import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.repository.CrawlJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CrawlJob Service - 크롤링 작업 비즈니스 로직
 *
 * 역할:
 * - 크롤링 작업 생성 (PENDING 상태)
 * - 작업 상태 조회 (jobId로 추적)
 * - 작업 상태 업데이트 (RUNNING → COMPLETED / FAILED)
 * - 중복 실행 방지 (이미 RUNNING 상태인 작업이 있는지 확인)
 *
 * @author Ekko0701
 * @since 2026-02-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlJobService {

    private final CrawlJobRepository crawlJobRepository;

    /**
     * 새로운 크롤링 작업 생성
     *
     * @return 생성된 CrawlJob (status = PENDING, id = 자동 생성 UUID)
     * @throws IllegalStateException 이미 실행 중인 작업이 있는 경우
     */
    @Transactional
    public CrawlJob createJob() {
        log.info("크롤링 작업 생성 요청");

        // 이미 실행 중인 작업이 있는지 확인 (중복 실행 방지)
        if (crawlJobRepository.existsByStatus(CrawlJobStatus.RUNNING)) {
            log.warn("이미 실행 중인 크롤링 작업이 있습니다.");
            throw new IllegalStateException("이미 실행 중인 크롤링 작업이 있습니다. 완료 후 다시 시도해주세요.");
        }

        CrawlJob job = CrawlJob.builder().build();
        CrawlJob savedJob = crawlJobRepository.save(job);

        log.info("크롤링 작업 생성 완료: jobId={}, status={}", savedJob.getId(), savedJob.getStatus());
        return savedJob;
    }

    /**
     * 작업 ID로 조회
     *
     * @param jobId 작업 UUID
     * @return CrawlJob 엔티티
     * @throws ResourceNotFoundException 작업을 찾을 수 없는 경우
     */
    public CrawlJob getJobById(UUID jobId) {
        log.debug("크롤링 작업 조회: jobId={}", jobId);

        return crawlJobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.error("크롤링 작업을 찾을 수 없음: jobId={}", jobId);
                    return ResourceNotFoundException.crawlJobNotFound(jobId.toString());
                });
    }

    /**
     * 상태별 작업 목록 조회
     *
     * @param status 조회할 상태
     * @return 해당 상태의 작업 목록
     */
    public List<CrawlJob> getJobsByStatus(CrawlJobStatus status) {
        log.debug("상태별 크롤링 작업 조회: status={}", status);
        return crawlJobRepository.findByStatus(status);
    }

    /**
     * 작업 상태를 RUNNING으로 변경
     *
     * @param jobId 작업 UUID
     * @return 업데이트된 CrawlJob
     */
    @Transactional
    public CrawlJob markRunning(UUID jobId) {
        CrawlJob job = getJobById(jobId);
        job.markRunning();

        log.info("크롤링 작업 실행 중: jobId={}", jobId);
        return job;
    }

    /**
     * 작업 상태를 COMPLETED로 변경
     *
     * @param jobId         작업 UUID
     * @param totalArticles 수집된 총 기사 수
     * @param mediaResults  언론사별 수집 결과 JSON
     * @return 업데이트된 CrawlJob
     */
    @Transactional
    public CrawlJob markCompleted(UUID jobId, int totalArticles, String mediaResults) {
        CrawlJob job = getJobById(jobId);
        job.markCompleted(totalArticles, mediaResults);

        log.info("크롤링 작업 완료: jobId={}, totalArticles={}", jobId, totalArticles);
        return job;
    }

    /**
     * 작업 상태를 FAILED로 변경
     *
     * @param jobId        작업 UUID
     * @param errorMessage 에러 메시지
     * @return 업데이트된 CrawlJob
     */
    @Transactional
    public CrawlJob markFailed(UUID jobId, String errorMessage) {
        CrawlJob job = getJobById(jobId);
        job.markFailed(errorMessage);

        log.error("크롤링 작업 실패: jobId={}, error={}", jobId, errorMessage);
        return job;
    }
}
