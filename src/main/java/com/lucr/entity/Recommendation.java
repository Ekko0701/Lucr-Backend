package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recommendation Entity - 종목별 투자 추천 정보
 *
 * <h3>테이블 설계</h3>
 * <pre>
 * recommendations
 * ├── id (PK, UUID)               - 추천 고유 ID
 * ├── stock_code (FK, UNIQUE)     - 종목코드 (stocks.code 참조)
 * ├── score (NOT NULL)            - 추천 점수 (0.000 ~ 1.000)
 * ├── confidence (NOT NULL)       - 신뢰도 (0.00 ~ 1.00)
 * ├── reason                      - 추천 이유 (JSON 배열 문자열)
 * ├── related_news_count          - 관련 뉴스 수
 * ├── avg_sentiment               - 정규화된 평균 감정 점수 (0 ~ 1)
 * ├── total_mentions              - 종목 총 언급 수
 * ├── created_at                  - 생성일시
 * ├── updated_at                  - 수정일시
 * └── expires_at                  - 추천 만료일시
 * </pre>
 *
 * <h3>핵심 설계</h3>
 * <ul>
 *   <li><b>글로벌 추천</b>: user_id 없이 종목 단위로 추천 (stock_code UNIQUE)</li>
 *   <li><b>UPSERT 패턴</b>: 크롤링 완료 시 기존 레코드를 갱신, 없으면 새로 생성</li>
 *   <li><b>만료 관리</b>: expires_at 기준으로 유효한 추천만 조회</li>
 * </ul>
 *
 * <h3>추천 점수 계산 공식</h3>
 * <pre>
 * score = 0.35 * avg_sentiment      (감정 점수, 0~1 정규화)
 *       + 0.30 * mention_frequency  (언급 빈도, 0~1 정규화)
 *       + 0.20 * news_volume        (뉴스 건수, 0~1 정규화)
 *       + 0.15 * recency_boost      (최근 24시간 뉴스 비율)
 * </pre>
 *
 * <h3>신뢰도(confidence) 계산</h3>
 * <pre>
 * confidence = min(related_news_count / 10, 1.0)
 *   - 관련 뉴스 1~2건: 0.1~0.2 (낮음)
 *   - 관련 뉴스 5건:   0.5 (보통)
 *   - 관련 뉴스 10건+: 1.0 (높음)
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-03-09
 * @see Stock
 * @see com.lucr.service.RecommendationServiceImpl
 */
@Entity
@Table(name = "recommendations", indexes = {
        @Index(name = "idx_recommendations_score", columnList = "score DESC"),
        @Index(name = "idx_recommendations_stock_code", columnList = "stock_code"),
        @Index(name = "idx_recommendations_expires_at", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Recommendation {

    /**
     * 추천 고유 ID (UUID, 자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 추천 대상 종목
     *
     * <ul>
     *   <li>{@code ManyToOne}: 하나의 Stock에 하나의 Recommendation (UNIQUE 제약)</li>
     *   <li>{@code FetchType.LAZY}: 추천 조회 시 종목 정보를 즉시 로드하지 않음</li>
     *   <li>{@code ON DELETE CASCADE}: 종목 삭제 시 추천도 함께 삭제</li>
     * </ul>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_recommendation_stock"))
    private Stock stock;

    /**
     * 추천 점수 (0.000 ~ 1.000)
     *
     * <p>4가지 지표의 가중 합산 결과.</p>
     * <ul>
     *   <li>0.0에 가까울수록 추천 강도 낮음</li>
     *   <li>1.0에 가까울수록 추천 강도 높음</li>
     * </ul>
     *
     * <p>precision = 4, scale = 3 → 최대 "1.000"까지 표현</p>
     */
    @Column(name = "score", nullable = false, precision = 4, scale = 3)
    private BigDecimal score;

    /**
     * 신뢰도 (0.00 ~ 1.00)
     *
     * <p>추천 근거가 되는 뉴스 데이터가 충분한지를 나타낸다.</p>
     * <p>관련 뉴스가 적으면 점수가 높아도 신뢰도가 낮다.</p>
     * <p>계산: {@code min(related_news_count / 10, 1.0)}</p>
     */
    @Column(name = "confidence", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    /**
     * 추천 이유 (JSON 배열 문자열)
     *
     * <p>예: {@code ["긍정적 뉴스 감정", "높은 언급 빈도", "최근 뉴스 활발"]}</p>
     * <p>클라이언트에서 파싱하여 사용자에게 추천 근거를 보여준다.</p>
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * 추천 근거가 된 관련 뉴스 수
     *
     * <p>news_stocks 테이블에서 이 종목을 언급한 뉴스의 수.</p>
     * <p>신뢰도 계산과 사용자 정보 표시에 사용된다.</p>
     */
    @Column(name = "related_news_count", nullable = false)
    @Builder.Default
    private Integer relatedNewsCount = 0;

    /**
     * 정규화된 평균 감정 점수 (0.000 ~ 1.000)
     *
     * <p>원본 감정 점수(-1~1)를 0~1로 변환한 값.</p>
     * <p>변환 공식: {@code (원본 + 1) / 2}</p>
     * <ul>
     *   <li>0.0 = 매우 부정적</li>
     *   <li>0.5 = 중립</li>
     *   <li>1.0 = 매우 긍정적</li>
     * </ul>
     */
    @Column(name = "avg_sentiment", precision = 4, scale = 3)
    private BigDecimal avgSentiment;

    /**
     * 종목 총 언급 수
     *
     * <p>news_stocks 테이블의 mention_count 합계.</p>
     * <p>하나의 뉴스에서 여러 번 언급될 수 있으므로 related_news_count와 다를 수 있다.</p>
     */
    @Column(name = "total_mentions", nullable = false)
    @Builder.Default
    private Integer totalMentions = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 추천 만료일시
     *
     * <p>크롤링 완료 시 현재 시간 + 24시간으로 설정된다.</p>
     * <p>만료된 추천은 스케줄러가 주기적으로 삭제한다.</p>
     * <p>NULL이면 만료되지 않는 추천으로 취급한다.</p>
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 추천 점수와 관련 데이터를 갱신한다.
     *
     * <p>UPSERT 패턴에서 기존 레코드를 업데이트할 때 사용한다.</p>
     * <p>JPA Dirty Checking에 의해 트랜잭션 커밋 시 자동으로 UPDATE 쿼리가 실행된다.</p>
     *
     * @param score            새 추천 점수
     * @param confidence       새 신뢰도
     * @param reason           새 추천 이유 (JSON)
     * @param relatedNewsCount 관련 뉴스 수
     * @param avgSentiment     정규화된 평균 감정 점수
     * @param totalMentions    총 언급 수
     * @param expiresAt        새 만료일시
     */
    public void updateScore(BigDecimal score, BigDecimal confidence, String reason,
                            int relatedNewsCount, BigDecimal avgSentiment,
                            int totalMentions, LocalDateTime expiresAt) {
        this.score = score;
        this.confidence = confidence;
        this.reason = reason;
        this.relatedNewsCount = relatedNewsCount;
        this.avgSentiment = avgSentiment;
        this.totalMentions = totalMentions;
        this.expiresAt = expiresAt;
    }
}
