package com.lucr.repository;

import com.lucr.entity.Keyword;
import com.lucr.entity.News;
import com.lucr.entity.NewsKeyword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NewsKeywordRepository 통합 테스트
 *
 * 검증 포인트:
 * - findByNewsIdWithKeyword(): 특정 뉴스의 키워드를 TF-IDF 내림차순으로 조회하는지
 * - findNewsIdsByKeywordId(): 특정 키워드가 연결된 뉴스 ID를 반환하는지
 * - deleteByNews_Id(): 특정 뉴스의 관계만 삭제되는지
 */
@DataJpaTest
@DisplayName("NewsKeywordRepository 테스트")
class NewsKeywordRepositoryTest {

    @Autowired
    private NewsKeywordRepository newsKeywordRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    private News newsA;
    private News newsB;
    private Keyword keywordSamsung;
    private Keyword keywordSemiconductor;

    @BeforeEach
    void setUp() {
        newsA = newsRepository.save(
                News.builder()
                        .title("삼성전자 실적 발표")
                        .source("hankyung")
                        .url("https://example.com/news-a")
                        .content("news-a")
                        .build()
        );

        newsB = newsRepository.save(
                News.builder()
                        .title("반도체 업황 개선")
                        .source("mk")
                        .url("https://example.com/news-b")
                        .content("news-b")
                        .build()
        );

        keywordSamsung = keywordRepository.save(
                Keyword.builder().word("삼성전자").frequency(10).build()
        );
        keywordSemiconductor = keywordRepository.save(
                Keyword.builder().word("반도체").frequency(20).build()
        );

        newsKeywordRepository.save(
                NewsKeyword.create(newsA, keywordSamsung, new BigDecimal("0.95"))
        );
        newsKeywordRepository.save(
                NewsKeyword.create(newsA, keywordSemiconductor, new BigDecimal("0.60"))
        );
        newsKeywordRepository.save(
                NewsKeyword.create(newsB, keywordSamsung, new BigDecimal("0.70"))
        );
    }

    @Nested
    @DisplayName("조회 쿼리")
    class QueryTests {

        @Test
        @DisplayName("뉴스 ID로 조회 시 TF-IDF 내림차순")
        void findByNewsIdWithKeyword_SortedByTfidfDesc() {
            List<NewsKeyword> result = newsKeywordRepository.findByNewsIdWithKeyword(newsA.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getKeyword().getWord()).isEqualTo("삼성전자");
            assertThat(result.get(0).getTfidfScore()).isEqualByComparingTo("0.95");
            assertThat(result.get(1).getKeyword().getWord()).isEqualTo("반도체");
            assertThat(result.get(1).getTfidfScore()).isEqualByComparingTo("0.60");
        }

        @Test
        @DisplayName("키워드 ID로 뉴스 ID 목록 조회")
        void findNewsIdsByKeywordId_ReturnsRelatedNewsIds() {
            List<UUID> newsIds = newsKeywordRepository.findNewsIdsByKeywordId(keywordSamsung.getId());

            assertThat(newsIds).hasSize(2);
            assertThat(newsIds).containsExactlyInAnyOrder(newsA.getId(), newsB.getId());
        }
    }

    @Nested
    @DisplayName("삭제 쿼리")
    class DeleteTests {

        @Test
        @DisplayName("특정 뉴스의 키워드 관계만 삭제")
        void deleteByNewsId_RemovesOnlyTargetNewsRelations() {
            newsKeywordRepository.deleteByNews_Id(newsA.getId());

            List<NewsKeyword> remainingForNewsA = newsKeywordRepository.findByNewsIdWithKeyword(newsA.getId());
            List<NewsKeyword> remainingForNewsB = newsKeywordRepository.findByNewsIdWithKeyword(newsB.getId());

            assertThat(remainingForNewsA).isEmpty();
            assertThat(remainingForNewsB).hasSize(1);
            assertThat(remainingForNewsB.getFirst().getKeyword().getWord()).isEqualTo("삼성전자");
        }
    }
}
