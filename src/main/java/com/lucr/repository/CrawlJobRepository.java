package com.lucr.repository;

import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * CrawlJob Repository - 크롤링 작업 데이터 접근 계층
 *
 * 기본 CRUD (JpaRepository 자동 제공):
 * - save(crawlJob)    : INSERT/UPDATE
 * - findById(id)      : SELECT by ID (jobId로 작업 상태 조회)
 * - findAll()         : SELECT ALL
 * - delete(crawlJob)  : DELETE
 *
 * @author Ekko0701
 * @since 2026-02-06
 */
@Repository
public interface CrawlJobRepository extends JpaRepository<CrawlJob, UUID> {

    /**
     * 상태별 작업 조회
     *
     * 생성되는 SQL:
     * SELECT * FROM crawl_jobs WHERE status = ?
     *
     * 예시: findByStatus(CrawlJobStatus.RUNNING) → 실행 중인 작업 확인
     */
    List<CrawlJob> findByStatus(CrawlJobStatus status);

    /**
     * 상태별 작업 존재 여부 확인
     *
     * 생성되는 SQL:
     * SELECT COUNT(*) > 0 FROM crawl_jobs WHERE status = ?
     *
     * 용도: 이미 실행 중인 작업이 있는지 확인 (중복 실행 방지)
     */
    boolean existsByStatus(CrawlJobStatus status);
}
