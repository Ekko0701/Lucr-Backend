package com.lucr.repository;

import com.lucr.entity.Market;
import com.lucr.entity.News;
import com.lucr.entity.NewsStock;
import com.lucr.entity.Stock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("NewsStockRepository 테스트")
class NewsStockRepositoryTest {

    @Autowired
    private NewsStockRepository newsStockRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private EntityManager entityManager;

    private Stock samsung;
    private Stock apple;

    @BeforeEach
    void setUp() {
        samsung = stockRepository.save(
                Stock.builder()
                        .code("005930")
                        .name("삼성전자")
                        .market(Market.KOSPI)
                        .build()
        );

        apple = stockRepository.save(
                Stock.builder()
                        .code("AAPL")
                        .name("Apple Inc.")
                        .market(Market.NASDAQ)
                        .build()
        );
    }

    private News saveNews(String url, BigDecimal sentiment, LocalDateTime publishedAt) {
        return newsRepository.save(
                News.builder()
                        .url(url)
                        .title("테스트 뉴스")
                        .content("본문")
                        .source("TEST")
                        .sentimentScore(sentiment)
                        .publishedAt(publishedAt)
                        .build()
        );
    }

    @Nested
    @DisplayName("sumMentionCountByStockCode()")
    class SumMentionTests {

        @Test
        @DisplayName("엣지 — 데이터가 없으면 0 반환 (COALESCE)")
        void sumMentionCount_NoRows_ReturnsZero() {
            int sum = newsStockRepository.sumMentionCountByStockCode("NOPE");
            assertThat(sum).isZero();
        }

        @Test
        @DisplayName("여러 뉴스의 mentionCount 합계를 정확히 반환")
        void sumMentionCount_SumsAllRows() {
            News n1 = saveNews("https://example.com/news1", new BigDecimal("0.80"), LocalDateTime.now().minusHours(2));
            News n2 = saveNews("https://example.com/news2", new BigDecimal("0.10"), LocalDateTime.now().minusHours(5));

            newsStockRepository.save(NewsStock.create(n1, samsung, 3));
            newsStockRepository.save(NewsStock.create(n2, samsung, 7));

            int sum = newsStockRepository.sumMentionCountByStockCode("005930");

            assertThat(sum).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("avgSentimentByStockCode()")
    class AvgSentimentTests {

        @Test
        @DisplayName("엣지 — 서로 다른 감정 점수 평균 계산")
        void avgSentiment_AveragesRelatedNewsScores() {
            News n1 = saveNews("https://example.com/avg1", new BigDecimal("0.80"), LocalDateTime.now().minusHours(2));
            News n2 = saveNews("https://example.com/avg2", new BigDecimal("-0.40"), LocalDateTime.now().minusHours(4));
            newsStockRepository.save(NewsStock.create(n1, samsung, 1));
            newsStockRepository.save(NewsStock.create(n2, samsung, 1));

            BigDecimal avg = newsStockRepository.avgSentimentByStockCode("005930");

            // (0.80 + -0.40) / 2 = 0.20
            assertThat(avg).isNotNull();
            assertThat(avg).isEqualByComparingTo("0.20");
        }
    }

    @Nested
    @DisplayName("countRecentNewsByStockCode()")
    class CountRecentNewsTests {

        @Test
        @DisplayName("엣지 — 경계값 포함(>= since)으로 최근 뉴스 수 계산")
        void countRecentNews_IncludesBoundaryTimestamp() {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            News recentBoundary = saveNews(
                    "https://example.com/recent-boundary",
                    new BigDecimal("0.10"),
                    since
            );
            News old = saveNews(
                    "https://example.com/old",
                    new BigDecimal("0.10"),
                    since.minusSeconds(1)
            );
            News otherStockRecent = saveNews(
                    "https://example.com/apple-recent",
                    new BigDecimal("0.30"),
                    LocalDateTime.now().minusHours(1)
            );

            newsStockRepository.save(NewsStock.create(recentBoundary, samsung, 1));
            newsStockRepository.save(NewsStock.create(old, samsung, 1));
            newsStockRepository.save(NewsStock.create(otherStockRecent, apple, 1));

            entityManager.flush();
            entityManager.clear();

            int count = newsStockRepository.countRecentNewsByStockCode("005930", since);

            // 삼성 기준: boundary 1건 포함, old 1건 제외
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countByStock_Code()")
    class CountByStockCodeTests {

        @Test
        @DisplayName("종목별 연관 뉴스 수를 정확히 반환")
        void countByStockCode_ReturnsRelationCount() {
            News n1 = saveNews("https://example.com/count1", new BigDecimal("0.20"), LocalDateTime.now().minusHours(2));
            News n2 = saveNews("https://example.com/count2", new BigDecimal("0.40"), LocalDateTime.now().minusHours(6));
            News n3 = saveNews("https://example.com/count3", new BigDecimal("0.50"), LocalDateTime.now().minusHours(8));

            newsStockRepository.save(NewsStock.create(n1, samsung, 1));
            newsStockRepository.save(NewsStock.create(n2, samsung, 1));
            newsStockRepository.save(NewsStock.create(n3, apple, 1));

            assertThat(newsStockRepository.countByStock_Code("005930")).isEqualTo(2);
            assertThat(newsStockRepository.countByStock_Code("AAPL")).isEqualTo(1);
            assertThat(newsStockRepository.countByStock_Code("NOPE")).isZero();
        }
    }
}
