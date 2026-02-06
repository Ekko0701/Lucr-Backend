package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CrawlJob Entity - 크롤링 작업 상태 관리
 *
 * Spring이 Python Crawler에 크롤링을 요청할 때마다 하나의 CrawlJob이 생성되며,
 * 작업의 생명주기(PENDING → RUNNING → COMPLETED/FAILED)를 추적합니다.
 *
 * @author Ekko0701
 * @since 2026-02-06
 */
@Entity
@Table(name = "crawl_jobs", indexes = {
        @Index(name = "idx_crawl_job_status", columnList = "status"),
        @Index(name = "idx_crawl_job_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlJob {

    /**
     * 작업 고유 ID (UUID)
     * RabbitMQ 메시지에 포함되어 Python과 작업을 식별하는 키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 작업 상태
     * PENDING → RUNNING → COMPLETED / FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CrawlJobStatus status = CrawlJobStatus.PENDING;

    /**
     * 수집된 총 기사 수
     */
    @Column(name = "total_articles")
    @Builder.Default
    private Integer totalArticles = 0;

    /**
     * 언론사별 수집 결과 (JSON 형태로 저장)
     * 예: {"hankyung": 50, "maekyung": 48, "edaily": 45}
     */
    @Column(name = "media_results", columnDefinition = "TEXT")
    private String mediaResults;

    /**
     * 실패 시 에러 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 작업 생성 시간 (자동)
     */
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 작업 수정 시간 (자동)
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 작업 완료 시간
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 크롤링 작업 상태 Enum
     */
    public enum CrawlJobStatus {
        PENDING,    // 작업 생성됨, 큐에서 대기 중
        RUNNING,    // Python Worker가 크롤링 실행 중
        COMPLETED,  // 크롤링 성공
        FAILED      // 크롤링 실패
    }

    /**
     * 작업을 실행 중 상태로 변경
     */
    public void markRunning() {
        this.status = CrawlJobStatus.RUNNING;
    }

    /**
     * 작업을 완료 상태로 변경
     *
     * @param totalArticles 수집된 총 기사 수
     * @param mediaResults  언론사별 결과 JSON
     */
    public void markCompleted(int totalArticles, String mediaResults) {
        this.status = CrawlJobStatus.COMPLETED;
        this.totalArticles = totalArticles;
        this.mediaResults = mediaResults;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 작업을 실패 상태로 변경
     *
     * @param errorMessage 에러 메시지
     */
    public void markFailed(String errorMessage) {
        this.status = CrawlJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}