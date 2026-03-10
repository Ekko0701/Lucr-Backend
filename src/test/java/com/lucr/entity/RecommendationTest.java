package com.lucr.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recommendation 엔티티 테스트")
class RecommendationTest {

    @Test
    @DisplayName("엣지 — Builder 기본값으로 relatedNewsCount/totalMentions는 0")
    void builder_DefaultValues_AreZero() {
        Stock stock = Stock.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();

        Recommendation recommendation = Recommendation.builder()
                .stock(stock)
                .score(new BigDecimal("0.500"))
                .confidence(new BigDecimal("0.20"))
                .reason("[]")
                .build();

        assertThat(recommendation.getRelatedNewsCount()).isEqualTo(0);
        assertThat(recommendation.getTotalMentions()).isEqualTo(0);
    }

    @Test
    @DisplayName("updateScore() — 기존 추천 데이터를 새 값으로 모두 갱신")
    void updateScore_UpdatesAllFields() {
        Stock stock = Stock.builder()
                .code("AAPL")
                .name("Apple Inc.")
                .market(Market.NASDAQ)
                .build();

        Recommendation recommendation = Recommendation.builder()
                .stock(stock)
                .score(new BigDecimal("0.400"))
                .confidence(new BigDecimal("0.10"))
                .reason("[\"old\"]")
                .relatedNewsCount(1)
                .avgSentiment(new BigDecimal("0.300"))
                .totalMentions(3)
                .expiresAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .build();

        LocalDateTime newExpiresAt = LocalDateTime.of(2026, 3, 11, 10, 0);
        recommendation.updateScore(
                new BigDecimal("0.773"),
                new BigDecimal("1.00"),
                "[\"긍정적 뉴스 감정\",\"높은 언급 빈도\"]",
                12,
                new BigDecimal("0.600"),
                180,
                newExpiresAt
        );

        assertThat(recommendation.getScore()).isEqualByComparingTo("0.773");
        assertThat(recommendation.getConfidence()).isEqualByComparingTo("1.00");
        assertThat(recommendation.getReason()).contains("긍정적 뉴스 감정");
        assertThat(recommendation.getRelatedNewsCount()).isEqualTo(12);
        assertThat(recommendation.getAvgSentiment()).isEqualByComparingTo("0.600");
        assertThat(recommendation.getTotalMentions()).isEqualTo(180);
        assertThat(recommendation.getExpiresAt()).isEqualTo(newExpiresAt);
    }
}
