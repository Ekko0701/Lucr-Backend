package com.lucr.specification;

import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.entity.News;
import com.lucr.repository.NewsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("NewsSpecification 테스트")
class NewsSpecificationTest {

    @Autowired
    private NewsRepository newsRepository;

    private News createAndSaveNews(String title, String source,
                                    BigDecimal sentiment, int viewCount) {
        News news = News.builder()
                .title(title)
                .content(title + " 본문 내용")
                .source(source)
                .url("https://test.com/" + System.nanoTime())
                .sentimentScore(sentiment)
                .viewCount(viewCount)
                .publishedAt(LocalDateTime.now())
                .build();
        return newsRepository.save(news);
    }

    @Nested
    @DisplayName("단일 필터 테스트")
    class SingleFilterTests {

        @Test
        @DisplayName("키워드 검색 - 제목에 포함된 뉴스만 반환")
        void keyword_MatchesTitle() {
            createAndSaveNews("삼성전자 주가 급등", "NAVER", BigDecimal.ZERO, 100);
            createAndSaveNews("SK하이닉스 실적 발표", "NAVER", BigDecimal.ZERO, 50);

            NewsSearchRequest request = NewsSearchRequest.builder()
                    .keyword("삼성전자")
                    .build();
            Specification<News> spec = NewsSpecification.fromSearchRequest(request);

            Page<News> result = newsRepository.findAll(spec, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("삼성전자");
        }

        @Test
        @DisplayName("감정 점수 범위 필터 - 긍정 뉴스만 반환")
        void sentimentScore_FiltersPositive() {
            createAndSaveNews("호재 뉴스", "NAVER", new BigDecimal("0.80"), 100);
            createAndSaveNews("악재 뉴스", "NAVER", new BigDecimal("-0.50"), 100);
            createAndSaveNews("중립 뉴스", "NAVER", new BigDecimal("0.10"), 100);

            NewsSearchRequest request = NewsSearchRequest.builder()
                    .minSentimentScore(new BigDecimal("0.50"))
                    .build();
            Specification<News> spec = NewsSpecification.fromSearchRequest(request);

            Page<News> result = newsRepository.findAll(spec, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("호재 뉴스");
        }

        @Test
        @DisplayName("출처 필터 - 특정 출처만 반환")
        void source_FiltersCorrectly() {
            createAndSaveNews("네이버 뉴스", "NAVER_FINANCE", BigDecimal.ZERO, 100);
            createAndSaveNews("다음 뉴스", "DAUM_FINANCE", BigDecimal.ZERO, 100);

            NewsSearchRequest request = NewsSearchRequest.builder()
                    .source("NAVER_FINANCE")
                    .build();
            Specification<News> spec = NewsSpecification.fromSearchRequest(request);

            Page<News> result = newsRepository.findAll(spec, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSource()).isEqualTo("NAVER_FINANCE");
        }

        @Test
        @DisplayName("조건 없음 - 전체 뉴스 반환")
        void noFilters_ReturnsAll() {
            createAndSaveNews("뉴스1", "NAVER", BigDecimal.ZERO, 100);
            createAndSaveNews("뉴스2", "DAUM", BigDecimal.ZERO, 200);

            NewsSearchRequest request = NewsSearchRequest.builder().build();
            Specification<News> spec = NewsSpecification.fromSearchRequest(request);

            Page<News> result = newsRepository.findAll(spec, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("복합 필터 테스트")
    class CombinedFilterTests {

        @Test
        @DisplayName("키워드 + 감정 점수 + 조회수 복합 필터")
        void combinedFilters_WorkTogether() {
            createAndSaveNews("삼성전자 호재", "NAVER", new BigDecimal("0.80"), 1500);
            createAndSaveNews("삼성전자 악재", "NAVER", new BigDecimal("-0.60"), 2000);
            createAndSaveNews("SK하이닉스 호재", "NAVER", new BigDecimal("0.70"), 500);

            NewsSearchRequest request = NewsSearchRequest.builder()
                    .keyword("삼성전자")
                    .minSentimentScore(new BigDecimal("0.50"))
                    .minViewCount(1000)
                    .build();
            Specification<News> spec = NewsSpecification.fromSearchRequest(request);

            Page<News> result = newsRepository.findAll(spec, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("삼성전자 호재");
        }

        @Test
        @DisplayName("날짜 범위 + 출처 복합 필터")
        void dateRange_WithSource() {
            News recent = News.builder()
                    .title("최신 뉴스")
                    .content("최신 뉴스 본문")
                    .source("NAVER_FINANCE")
                    .url("https://test.com/" + System.nanoTime())
                    .publishedAt(LocalDateTime.now().minusHours(1))
                    .build();
            newsRepository.save(recent);

            News old = News.builder()
                    .title("오래된 뉴스")
                    .content("오래된 뉴스 본문")
                    .source("NAVER_FINANCE")
                    .url("https://test.com/" + System.nanoTime())
                    .publishedAt(LocalDateTime.now().minusDays(30))
                    .build();
            newsRepository.save(old);

            NewsSearchRequest request = NewsSearchRequest.builder()
                    .source("NAVER_FINANCE")
                    .startDate(LocalDateTime.now().minusDays(7))
                    .build();
            Specification<News> spec = NewsSpecification.fromSearchRequest(request);

            Page<News> result = newsRepository.findAll(spec, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("최신 뉴스");
        }
    }
}
