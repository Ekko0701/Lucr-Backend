package com.lucr.repository;

import com.lucr.entity.Market;
import com.lucr.entity.Recommendation;
import com.lucr.entity.Stock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("RecommendationRepository 테스트")
class RecommendationRepositoryTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private EntityManager entityManager;

    private Stock saveStock(String code, String name, Market market) {
        return stockRepository.save(
                Stock.builder()
                        .code(code)
                        .name(name)
                        .market(market)
                        .build()
        );
    }

    private Recommendation saveRecommendation(
            Stock stock,
            String score,
            String confidence,
            LocalDateTime expiresAt
    ) {
        return recommendationRepository.save(
                Recommendation.builder()
                        .stock(stock)
                        .score(new BigDecimal(score))
                        .confidence(new BigDecimal(confidence))
                        .reason("[\"테스트\"]")
                        .relatedNewsCount(3)
                        .avgSentiment(new BigDecimal("0.500"))
                        .totalMentions(10)
                        .expiresAt(expiresAt)
                        .build()
        );
    }

    @Nested
    @DisplayName("findValidRecommendations()")
    class FindValidRecommendationsTests {

        @Test
        @DisplayName("엣지 — 만료된 추천은 제외하고 score 내림차순 정렬")
        void findValidRecommendations_ExcludesExpired_OrdersByScoreDesc() {
            LocalDateTime now = LocalDateTime.now();

            Stock s1 = saveStock("005930", "삼성전자", Market.KOSPI);
            Stock s2 = saveStock("035720", "카카오", Market.KOSDAQ);
            Stock s3 = saveStock("AAPL", "Apple", Market.NASDAQ);

            saveRecommendation(s1, "0.900", "0.90", now.plusHours(1));   // 유효
            saveRecommendation(s2, "0.700", "0.80", null);                // 유효(null 만료)
            saveRecommendation(s3, "0.950", "0.95", now.minusMinutes(1)); // 만료

            entityManager.flush();
            entityManager.clear();

            Page<Recommendation> page = recommendationRepository.findValidRecommendations(
                    now, PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(r -> r.getStock().getCode())
                    .containsExactly("005930", "035720");
        }
    }

    @Nested
    @DisplayName("findByMinConfidence()")
    class FindByMinConfidenceTests {

        @Test
        @DisplayName("엣지 — 신뢰도 필터 + 유효기간 필터를 동시에 적용")
        void findByMinConfidence_AppliesConfidenceAndValidityFilters() {
            LocalDateTime now = LocalDateTime.now();

            Stock s1 = saveStock("005930", "삼성전자", Market.KOSPI);
            Stock s2 = saveStock("035720", "카카오", Market.KOSDAQ);
            Stock s3 = saveStock("AAPL", "Apple", Market.NASDAQ);

            saveRecommendation(s1, "0.800", "0.85", now.plusHours(1));   // 통과
            saveRecommendation(s2, "0.700", "0.65", now.plusHours(1));   // confidence 미달
            saveRecommendation(s3, "0.900", "0.95", now.minusHours(1));  // 만료

            entityManager.flush();
            entityManager.clear();

            Page<Recommendation> page = recommendationRepository.findByMinConfidence(
                    new BigDecimal("0.70"),
                    now,
                    PageRequest.of(0, 10)
            );

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().getStock().getCode()).isEqualTo("005930");
        }
    }

    @Nested
    @DisplayName("deleteExpired()")
    class DeleteExpiredTests {

        @Test
        @DisplayName("엣지 — expiresAt이 null인 레코드는 삭제하지 않음")
        void deleteExpired_DeletesOnlyPastRows() {
            LocalDateTime now = LocalDateTime.now();

            Stock s1 = saveStock("005930", "삼성전자", Market.KOSPI);
            Stock s2 = saveStock("035720", "카카오", Market.KOSDAQ);
            Stock s3 = saveStock("AAPL", "Apple", Market.NASDAQ);

            saveRecommendation(s1, "0.800", "0.80", now.minusMinutes(1)); // 삭제 대상
            saveRecommendation(s2, "0.700", "0.70", now.plusMinutes(30)); // 유지
            saveRecommendation(s3, "0.600", "0.60", null);                // 유지(null)

            entityManager.flush();
            entityManager.clear();

            int deleted = recommendationRepository.deleteExpired(now);

            assertThat(deleted).isEqualTo(1);
            assertThat(recommendationRepository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("기타 메서드")
    class OtherMethodsTests {

        @Test
        @DisplayName("countValidRecommendations — 미래/NULL 만료만 카운트")
        void countValidRecommendations_CountsOnlyValid() {
            LocalDateTime now = LocalDateTime.now();

            Stock s1 = saveStock("005930", "삼성전자", Market.KOSPI);
            Stock s2 = saveStock("035720", "카카오", Market.KOSDAQ);
            Stock s3 = saveStock("AAPL", "Apple", Market.NASDAQ);

            saveRecommendation(s1, "0.800", "0.80", now.plusMinutes(1)); // 유효
            saveRecommendation(s2, "0.700", "0.70", null);               // 유효
            saveRecommendation(s3, "0.600", "0.60", now.minusMinutes(1)); // 만료

            long validCount = recommendationRepository.countValidRecommendations(now);

            assertThat(validCount).isEqualTo(2);
        }

        @Test
        @DisplayName("findByStock_Code — 존재/미존재 케이스")
        void findByStockCode_ExistsAndNotExists() {
            Stock samsung = saveStock("005930", "삼성전자", Market.KOSPI);
            saveRecommendation(samsung, "0.800", "0.80", LocalDateTime.now().plusHours(1));

            Optional<Recommendation> exists = recommendationRepository.findByStock_Code("005930");
            Optional<Recommendation> notExists = recommendationRepository.findByStock_Code("NOPE");

            assertThat(exists).isPresent();
            assertThat(notExists).isEmpty();
        }
    }
}
