package com.lucr.mapper;

import com.lucr.dto.response.RecommendationResponse;
import com.lucr.entity.Market;
import com.lucr.entity.Recommendation;
import com.lucr.entity.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecommendationMapper 테스트")
class RecommendationMapperTest {

    private final RecommendationMapper recommendationMapper = new RecommendationMapper();

    private Recommendation baseRecommendation(String reason) {
        Stock stock = Stock.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();

        return Recommendation.builder()
                .id(UUID.randomUUID())
                .stock(stock)
                .score(new BigDecimal("0.773"))
                .confidence(new BigDecimal("1.00"))
                .reason(reason)
                .relatedNewsCount(12)
                .avgSentiment(new BigDecimal("0.600"))
                .totalMentions(180)
                .updatedAt(LocalDateTime.of(2026, 3, 10, 10, 0, 0))
                .expiresAt(LocalDateTime.of(2026, 3, 11, 10, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponseTests {

        @Test
        @DisplayName("성공 — 모든 필드가 정확하게 매핑된다")
        void toResponse_MapsAllFieldsCorrectly() {
            Recommendation entity = baseRecommendation("[\"긍정적 뉴스 감정\",\"최근 뉴스 활발\"]");

            RecommendationResponse response = recommendationMapper.toResponse(entity);

            assertThat(response.getId()).isEqualTo(entity.getId());
            assertThat(response.getStockCode()).isEqualTo("005930");
            assertThat(response.getStockName()).isEqualTo("삼성전자");
            assertThat(response.getMarket()).isEqualTo("KOSPI");
            assertThat(response.getScore()).isEqualByComparingTo("0.773");
            assertThat(response.getConfidence()).isEqualByComparingTo("1.00");
            assertThat(response.getReasons()).containsExactly("긍정적 뉴스 감정", "최근 뉴스 활발");
            assertThat(response.getRelatedNewsCount()).isEqualTo(12);
            assertThat(response.getAvgSentiment()).isEqualByComparingTo("0.600");
            assertThat(response.getTotalMentions()).isEqualTo(180);
            assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 10, 10, 0, 0));
            assertThat(response.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 3, 11, 10, 0, 0));
        }

        @Test
        @DisplayName("엣지 — reason이 null이면 빈 리스트 반환")
        void toResponse_NullReason_ReturnsEmptyList() {
            Recommendation entity = baseRecommendation(null);

            RecommendationResponse response = recommendationMapper.toResponse(entity);

            assertThat(response.getReasons()).isEmpty();
        }

        @Test
        @DisplayName("엣지 — reason이 공백이면 빈 리스트 반환")
        void toResponse_BlankReason_ReturnsEmptyList() {
            Recommendation entity = baseRecommendation("   ");

            RecommendationResponse response = recommendationMapper.toResponse(entity);

            assertThat(response.getReasons()).isEmpty();
        }

        @Test
        @DisplayName("엣지 — reason이 빈 JSON 배열이면 빈 리스트 반환")
        void toResponse_EmptyJsonArray_ReturnsEmptyList() {
            Recommendation entity = baseRecommendation("[]");

            RecommendationResponse response = recommendationMapper.toResponse(entity);

            assertThat(response.getReasons()).isEmpty();
        }

        @Test
        @DisplayName("엣지 — reason이 잘못된 JSON이면 원문 1개 리스트로 fallback")
        void toResponse_InvalidJsonReason_FallbackToRawString() {
            Recommendation entity = baseRecommendation("this-is-not-json");

            RecommendationResponse response = recommendationMapper.toResponse(entity);

            assertThat(response.getReasons()).containsExactly("this-is-not-json");
        }

        @Test
        @DisplayName("실패 — entity가 null이면 NullPointerException")
        void toResponse_NullEntity_ThrowsNpe() {
            assertThatThrownBy(() -> recommendationMapper.toResponse(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
