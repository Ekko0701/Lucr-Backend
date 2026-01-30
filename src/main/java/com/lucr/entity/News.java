package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * News Entity - 금융 뉴스 정보
 * 
 * JPA 주요 어노테이션:
 * - @Entity: JPA가 관리하는 엔티티 클래스
 * - @Table: 데이터베이스 테이블 이름 및 인덱스 설정
 * - @Id: 기본키(Primary Key)
 * - @GeneratedValue: 자동 생성 전략 (UUID)
 * - @Column: 컬럼 속성 정의
 * 
 * Lombok 어노테이션:
 * - @Getter/@Setter: getter/setter 자동 생성
 * - @NoArgsConstructor: 기본 생성자
 * - @AllArgsConstructor: 모든 필드 생성자
 * - @Builder: 빌더 패턴
 * 
 * @author Kim Dongjoo
 * @since 2026-01-26
 */
@Entity
@Table(name = "news", indexes = {
    @Index(name = "idx_news_view_count", columnList = "view_count DESC"),
    @Index(name = "idx_news_published_at", columnList = "published_at DESC"),
    @Index(name = "idx_news_sentiment", columnList = "sentiment_score"),
    @Index(name = "idx_news_source", columnList = "source")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {
    
    /**
     * 뉴스 고유 ID (UUID)
     * 
     * UUID를 사용하는 이유:
     * - 분산 시스템에서 ID 충돌 방지
     * - 보안 향상 (순차적 ID는 예측 가능)
     * - 마이크로서비스 아키텍처에 적합
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * 뉴스 제목
     * 
     * - nullable = false: NULL 불가 (필수 항목)
     * - length = 500: 최대 500자
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    /**
     * 뉴스 본문
     * 
     * - columnDefinition = "TEXT": 긴 텍스트 저장
     * - PostgreSQL의 TEXT 타입 사용
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    /**
     * 뉴스 출처 (NAVER_FINANCE, DAUM_FINANCE 등)
     */
    @Column(name = "source", nullable = false, length = 100)
    private String source;
    
    /**
     * 뉴스 URL (중복 방지를 위해 unique 설정)
     * 
     * - unique = true: 중복 URL 방지
     * - 같은 뉴스가 여러 번 크롤링되는 것 방지
     */
    @Column(name = "url", nullable = false, unique = true, columnDefinition = "TEXT")
    private String url;
    
    /**
     * 조회수
     * 
     * - @Builder.Default: Builder 사용 시 기본값 0
     */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;
    
    /**
     * 뉴스 발행 시간
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    /**
     * 크롤링된 시간
     * 
     * - @CreationTimestamp: INSERT 시 자동으로 현재 시간 저장
     */
    @Column(name = "crawled_at")
    @CreationTimestamp
    private LocalDateTime crawledAt;
    
    /**
     * AI 감정 분석 점수 (-1.0 ~ 1.0)
     * 
     * - -1.0: 매우 부정적
     * -  0.0: 중립
     * -  1.0: 매우 긍정적
     * 
     * - precision = 3: 전체 자릿수 (예: 0.75)
     * - scale = 2: 소수점 자릿수 (예: .75)
     */
    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private BigDecimal sentimentScore;
    
    /**
     * 고조회수 뉴스 여부 (1000+ 조회수)
     * 
     * - 비즈니스 로직: 조회수가 1000 이상이면 true
     */
    @Column(name = "is_high_view")
    @Builder.Default
    private Boolean isHighView = false;
    
    /**
     * 생성 시간 (자동 생성)
     *
     * - updatable = false: UPDATE 시 변경 불가
     * - @CreationTimestamp: INSERT 시 자동 저장
     */
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 수정 시간 (자동 업데이트)
     *
     * - @UpdateTimestamp: UPDATE 시 자동 갱신
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * JPA Lifecycle 콜백 - INSERT 전 실행
     * createdAt과 updatedAt을 현재 시간으로 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * JPA Lifecycle 콜백 - UPDATE 전 실행
     * updatedAt을 현재 시간으로 갱신
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 조회수 증가 메서드
     * 
     * 비즈니스 로직을 Entity에 포함하여 응집도 향상
     * - 조회수 증가
     * - 1000 이상이면 isHighView를 true로 업데이트
     */
    public void incrementViewCount() {
        this.viewCount++;
        updateHighViewStatus();
    }
    
    /**
     * 고조회수 상태 업데이트
     * 
     * private 메서드로 내부에서만 사용
     */
    private void updateHighViewStatus() {
        this.isHighView = this.viewCount >= 1000;
    }
    
    /**
     * 감정 점수 설정 (유효성 검사 포함)
     * 
     * @param score 감정 점수 (-1.0 ~ 1.0)
     * @throws IllegalArgumentException 범위를 벗어난 경우
     */
    public void setSentimentScore(BigDecimal score) {
        if (score != null && (score.compareTo(BigDecimal.valueOf(-1.0)) < 0 
                || score.compareTo(BigDecimal.valueOf(1.0)) > 0)) {
            throw new IllegalArgumentException("감정 점수는 -1.0과 1.0 사이여야 합니다.");
        }
        this.sentimentScore = score;
    }
}
