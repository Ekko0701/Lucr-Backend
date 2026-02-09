package com.lucr.service;

import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;

import java.util.List;
import java.util.UUID;

/**
 * CrawlJob Service 인터페이스 - 크롤링 작업 비즈니스 로직
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
public interface CrawlJobService {

    /**
     * 새로운 크롤링 작업 생성
     *
     * @return 생성된 CrawlJob (status = PENDING, id = 자동 생성 UUID)
     * @throws com.lucr.exception.BusinessException 이미 실행 중인 작업이 있는 경우 (409)
     */
    CrawlJob createJob();

    /**
     * 작업 ID로 조회
     *
     * @param jobId 작업 UUID
     * @return CrawlJob 엔티티
     * @throws com.lucr.exception.ResourceNotFoundException 작업을 찾을 수 없는 경우
     */
    CrawlJob getJobById(UUID jobId);

    /**
     * 상태별 작업 목록 조회
     *
     * @param status 조회할 상태
     * @return 해당 상태의 작업 목록
     */
    List<CrawlJob> getJobsByStatus(CrawlJobStatus status);

    /**
     * 작업 상태를 RUNNING으로 변경
     *
     * @param jobId 작업 UUID
     * @return 업데이트된 CrawlJob
     */
    CrawlJob markRunning(UUID jobId);

    /**
     * 작업 상태를 COMPLETED로 변경
     *
     * @param jobId         작업 UUID
     * @param totalArticles 수집된 총 기사 수
     * @param mediaResults  언론사별 수집 결과 JSON
     * @return 업데이트된 CrawlJob
     */
    CrawlJob markCompleted(UUID jobId, int totalArticles, String mediaResults);

    /**
     * 작업 상태를 FAILED로 변경
     *
     * @param jobId        작업 UUID
     * @param errorMessage 에러 메시지
     * @return 업데이트된 CrawlJob
     */
    CrawlJob markFailed(UUID jobId, String errorMessage);
}
