package com.lucr.mapper;

import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.entity.News;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * NewsMapper 테스트
 * 
 * DTO ↔ Entity 변환 로직 검증
 * - Request → Entity 변환
 * - Entity → Response 변환
 * - Null 안전성
 * - Helper 메서드 검증
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@DisplayName("NewsMapper 테스트")
class NewsMapperTest {

    private NewsMapper newsMapper;
    
    private News testNews;
    private NewsCreateRequest testCreateRequest;
    private NewsUpdateRequest testUpdateRequest;

    @BeforeEach
    void setUp() {
        newsMapper = new NewsMapper();
        
        // 테스트용 Entity
        testNews = News.builder()
                .id(UUID.randomUUID())
                .title("삼성전자 주가 상승")
                .content("삼성전자의 주가가 오늘 5% 상승했습니다. 시장 전문가들은 이번 상승이 반도체 시장의 회복 신호라고 분석하고 있습니다.")
                .source("NAVER_FINANCE")
                .url("https://example.com/news1")
                .viewCount(1500)
                .isHighView(true)
                .sentimentScore(BigDecimal.valueOf(0.8))
                .publishedAt(LocalDateTime.of(2026, 1, 28, 10, 0))
                .crawledAt(LocalDateTime.of(2026, 1, 28, 10, 5))
                .createdAt(LocalDateTime.of(2026, 1, 28, 10, 5))
                .updatedAt(LocalDateTime.of(2026, 1, 28, 10, 5))
                .build();
        
        // 테스트용 CreateRequest
        testCreateRequest = NewsCreateRequest.builder()
                .title("애플 신제품 출시")
                .content("애플이 새로운 아이폰을 출시했습니다.")
                .source("YAHOO_FINANCE")
                .url("https://example.com/news2")
                .publishedAt(LocalDateTime.of(2026, 1, 28, 12, 0))
                .build();
        
        // 테스트용 UpdateRequest
        testUpdateRequest = NewsUpdateRequest.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .source("BLOOMBERG")
                .sentimentScore(BigDecimal.valueOf(0.5))
                .build();
    }

    // ========== 1. Request → Entity 변환 테스트 ==========

    @Nested
    @DisplayName("toEntity() - NewsCreateRequest → News Entity 변환")
    class ToEntityTests {

        @Test
        @DisplayName("정상 변환 - 모든 필드 매핑")
        void toEntity_AllFields_Success() {
            // when
            News entity = newsMapper.toEntity(testCreateRequest);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getTitle()).isEqualTo("애플 신제품 출시");
            assertThat(entity.getContent()).isEqualTo("애플이 새로운 아이폰을 출시했습니다.");
            assertThat(entity.getSource()).isEqualTo("YAHOO_FINANCE");
            assertThat(entity.getUrl()).isEqualTo("https://example.com/news2");
            assertThat(entity.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 1, 28, 12, 0));
        }

        @Test
        @DisplayName("publishedAt null - 현재 시간으로 자동 설정")
        void toEntity_NullPublishedAt_UsesCurrent() {
            // given
            NewsCreateRequest requestWithoutPublishedAt = NewsCreateRequest.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .url("https://example.com/test")
                    .publishedAt(null)  // null
                    .build();

            // when
            News entity = newsMapper.toEntity(requestWithoutPublishedAt);

            // then
            assertThat(entity.getPublishedAt()).isNotNull();
            assertThat(entity.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("기본값 필드 - Entity의 @Builder.Default 사용")
        void toEntity_DefaultFields_UsesEntityDefaults() {
            // when
            News entity = newsMapper.toEntity(testCreateRequest);

            // then
            // viewCount, isHighView는 Entity의 기본값 사용
            assertThat(entity.getViewCount()).isEqualTo(0);
            assertThat(entity.getIsHighView()).isFalse();
            
            // id, createdAt, updatedAt는 null (JPA가 save 시 생성)
            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
        }
    }

    // ========== 2. Entity 업데이트 테스트 ==========

    @Nested
    @DisplayName("updateEntity() - NewsUpdateRequest로 Entity 업데이트")
    class UpdateEntityTests {

        @Test
        @DisplayName("모든 필드 업데이트 - 성공")
        void updateEntity_AllFields_Success() {
            // given
            News entity = News.builder()
                    .title("원래 제목")
                    .content("원래 내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.2))
                    .build();

            // when
            newsMapper.updateEntity(entity, testUpdateRequest);

            // then
            assertThat(entity.getTitle()).isEqualTo("수정된 제목");
            assertThat(entity.getContent()).isEqualTo("수정된 내용");
            assertThat(entity.getSource()).isEqualTo("BLOOMBERG");
            assertThat(entity.getSentimentScore()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        }

        @Test
        @DisplayName("부분 업데이트 - null 필드는 기존 값 유지")
        void updateEntity_PartialUpdate_KeepsExistingValues() {
            // given
            News entity = News.builder()
                    .title("원래 제목")
                    .content("원래 내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.2))
                    .build();

            NewsUpdateRequest partialRequest = NewsUpdateRequest.builder()
                    .title("수정된 제목만")
                    .content(null)  // null - 업데이트 안 함
                    .source(null)   // null - 업데이트 안 함
                    .sentimentScore(null)  // null - 업데이트 안 함
                    .build();

            // when
            newsMapper.updateEntity(entity, partialRequest);

            // then
            assertThat(entity.getTitle()).isEqualTo("수정된 제목만");
            assertThat(entity.getContent()).isEqualTo("원래 내용");  // 유지
            assertThat(entity.getSource()).isEqualTo("NAVER_FINANCE");  // 유지
            assertThat(entity.getSentimentScore()).isEqualByComparingTo(BigDecimal.valueOf(0.2));  // 유지
        }

        @Test
        @DisplayName("모든 필드 null - 기존 값 전부 유지")
        void updateEntity_AllNullFields_KeepsAllValues() {
            // given
            News entity = News.builder()
                    .title("원래 제목")
                    .content("원래 내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.2))
                    .build();

            NewsUpdateRequest emptyRequest = NewsUpdateRequest.builder().build();

            // when
            newsMapper.updateEntity(entity, emptyRequest);

            // then - 모든 값 유지
            assertThat(entity.getTitle()).isEqualTo("원래 제목");
            assertThat(entity.getContent()).isEqualTo("원래 내용");
            assertThat(entity.getSource()).isEqualTo("NAVER_FINANCE");
            assertThat(entity.getSentimentScore()).isEqualByComparingTo(BigDecimal.valueOf(0.2));
        }
    }

    // ========== 3. Entity → Response 변환 테스트 ==========

    @Nested
    @DisplayName("toResponse() - Entity → NewsResponse 변환")
    class ToResponseTests {

        @Test
        @DisplayName("정상 변환 - 모든 필드 매핑")
        void toResponse_AllFields_Success() {
            // when
            NewsResponse response = newsMapper.toResponse(testNews);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testNews.getId());
            assertThat(response.getTitle()).isEqualTo("삼성전자 주가 상승");
            assertThat(response.getSource()).isEqualTo("NAVER_FINANCE");
            assertThat(response.getUrl()).isEqualTo("https://example.com/news1");
            assertThat(response.getViewCount()).isEqualTo(1500);
            assertThat(response.getIsHighView()).isTrue();
            assertThat(response.getSentimentScore()).isEqualByComparingTo(BigDecimal.valueOf(0.8));
            assertThat(response.getPublishedAt()).isEqualTo(testNews.getPublishedAt());
            assertThat(response.getCreatedAt()).isEqualTo(testNews.getCreatedAt());
        }

        @Test
        @DisplayName("본문 요약 - 100자 이하는 그대로")
        void toResponse_ShortContent_NoTruncation() {
            // given
            News shortNews = News.builder()
                    .title("제목")
                    .content("짧은 내용입니다.")  // 9자
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(shortNews);

            // then
            assertThat(response.getContentSummary()).isEqualTo("짧은 내용입니다.");
        }

        @Test
        @DisplayName("본문 요약 - 100자 초과는 자르고 ... 추가")
        void toResponse_LongContent_Truncated() {
            // given
            String longContent = "가".repeat(150);  // 150자
            News longNews = News.builder()
                    .title("제목")
                    .content(longContent)
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(longNews);

            // then
            assertThat(response.getContentSummary()).hasSize(103);  // 100자 + "..."
            assertThat(response.getContentSummary()).startsWith("가".repeat(100));
            assertThat(response.getContentSummary()).endsWith("...");
        }

        @Test
        @DisplayName("본문 요약 - 정확히 100자는 그대로")
        void toResponse_ExactlyHundredChars_NoTruncation() {
            // given
            String exactContent = "나".repeat(100);  // 정확히 100자
            News exactNews = News.builder()
                    .title("제목")
                    .content(exactContent)
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(exactNews);

            // then
            assertThat(response.getContentSummary()).hasSize(100);
            assertThat(response.getContentSummary()).doesNotContain("...");
        }

        @Test
        @DisplayName("null 본문 - 빈 문자열 반환")
        void toResponse_NullContent_ReturnsEmptyString() {
            // given
            News nullContentNews = News.builder()
                    .title("제목")
                    .content(null)
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(nullContentNews);

            // then
            assertThat(response.getContentSummary()).isEmpty();
        }

        @Test
        @DisplayName("빈 본문 - 빈 문자열 반환")
        void toResponse_EmptyContent_ReturnsEmptyString() {
            // given
            News emptyContentNews = News.builder()
                    .title("제목")
                    .content("")
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(emptyContentNews);

            // then
            assertThat(response.getContentSummary()).isEmpty();
        }
    }

    // ========== 4. Entity → DetailResponse 변환 테스트 ==========

    @Nested
    @DisplayName("toDetailResponse() - Entity → NewsDetailResponse 변환")
    class ToDetailResponseTests {

        @Test
        @DisplayName("정상 변환 - 모든 필드 매핑")
        void toDetailResponse_AllFields_Success() {
            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(testNews);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testNews.getId());
            assertThat(response.getTitle()).isEqualTo("삼성전자 주가 상승");
            assertThat(response.getContent()).isEqualTo(testNews.getContent());
            assertThat(response.getSource()).isEqualTo("NAVER_FINANCE");
            assertThat(response.getUrl()).isEqualTo("https://example.com/news1");
            assertThat(response.getViewCount()).isEqualTo(1500);
            assertThat(response.getIsHighView()).isTrue();
            assertThat(response.getSentimentScore()).isEqualByComparingTo(BigDecimal.valueOf(0.8));
            assertThat(response.getPublishedAt()).isEqualTo(testNews.getPublishedAt());
            assertThat(response.getCrawledAt()).isEqualTo(testNews.getCrawledAt());
            assertThat(response.getCreatedAt()).isEqualTo(testNews.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testNews.getUpdatedAt());
        }

        @Test
        @DisplayName("본문 길이 계산 - 정상")
        void toDetailResponse_ContentLength_Calculated() {
            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(testNews);

            // then
            assertThat(response.getContentLength()).isEqualTo(testNews.getContent().length());
        }

        @Test
        @DisplayName("본문 길이 계산 - null 본문은 0")
        void toDetailResponse_NullContent_LengthZero() {
            // given
            News nullContentNews = News.builder()
                    .title("제목")
                    .content(null)
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(nullContentNews);

            // then
            assertThat(response.getContentLength()).isEqualTo(0);
        }

        @Test
        @DisplayName("감정 라벨 - 매우 긍정적 (>= 0.7)")
        void toDetailResponse_SentimentLabel_VeryPositive() {
            // given
            News positiveNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.85))
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(positiveNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("매우 긍정적");
        }

        @Test
        @DisplayName("감정 라벨 - 긍정적 (0.3 ~ 0.7)")
        void toDetailResponse_SentimentLabel_Positive() {
            // given
            News positiveNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.5))
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(positiveNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("긍정적");
        }

        @Test
        @DisplayName("감정 라벨 - 중립 (-0.3 ~ 0.3)")
        void toDetailResponse_SentimentLabel_Neutral() {
            // given
            News neutralNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.0))
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(neutralNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("중립");
        }

        @Test
        @DisplayName("감정 라벨 - 부정적 (-0.7 ~ -0.3)")
        void toDetailResponse_SentimentLabel_Negative() {
            // given
            News negativeNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(-0.5))
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(negativeNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("부정적");
        }

        @Test
        @DisplayName("감정 라벨 - 매우 부정적 (< -0.7)")
        void toDetailResponse_SentimentLabel_VeryNegative() {
            // given
            News veryNegativeNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(-0.85))
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(veryNegativeNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("매우 부정적");
        }

        @Test
        @DisplayName("감정 라벨 - null인 경우 '분석 전'")
        void toDetailResponse_SentimentLabel_NullIsUnanalyzed() {
            // given
            News unanalyzedNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(null)
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(unanalyzedNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("분석 전");
        }

        @Test
        @DisplayName("감정 라벨 경계값 - 정확히 0.7")
        void toDetailResponse_SentimentLabel_BoundaryExactly07() {
            // given
            News boundaryNews = News.builder()
                    .title("제목")
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .sentimentScore(BigDecimal.valueOf(0.7))
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(boundaryNews);

            // then
            assertThat(response.getSentimentLabel()).isEqualTo("매우 긍정적");
        }

        @Test
        @DisplayName("읽기 시간 계산 - 200자 = 1분")
        void toDetailResponse_ReadingTime_TwoHundredChars() {
            // given
            News news = News.builder()
                    .title("제목")
                    .content("가".repeat(200))  // 200자
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(news);

            // then
            assertThat(response.getEstimatedReadingTime()).isEqualTo(1);
        }

        @Test
        @DisplayName("읽기 시간 계산 - 400자 = 2분")
        void toDetailResponse_ReadingTime_FourHundredChars() {
            // given
            News news = News.builder()
                    .title("제목")
                    .content("나".repeat(400))  // 400자
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(news);

            // then
            assertThat(response.getEstimatedReadingTime()).isEqualTo(2);
        }

        @Test
        @DisplayName("읽기 시간 계산 - 100자도 최소 1분")
        void toDetailResponse_ReadingTime_MinimumOneMinute() {
            // given
            News shortNews = News.builder()
                    .title("제목")
                    .content("짧은 내용")  // 5자
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(shortNews);

            // then
            assertThat(response.getEstimatedReadingTime()).isEqualTo(1);
        }

        @Test
        @DisplayName("읽기 시간 계산 - null 본문은 0분")
        void toDetailResponse_ReadingTime_NullContent() {
            // given
            News nullContentNews = News.builder()
                    .title("제목")
                    .content(null)
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(nullContentNews);

            // then
            assertThat(response.getEstimatedReadingTime()).isEqualTo(0);
        }

        @Test
        @DisplayName("읽기 시간 계산 - 빈 본문은 0분")
        void toDetailResponse_ReadingTime_EmptyContent() {
            // given
            News emptyContentNews = News.builder()
                    .title("제목")
                    .content("")
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(emptyContentNews);

            // then
            assertThat(response.getEstimatedReadingTime()).isEqualTo(0);
        }
    }

    // ========== 5. Edge Cases 테스트 ==========

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("매우 긴 제목 - 변환 성공")
        void toResponse_VeryLongTitle_Success() {
            // given
            String veryLongTitle = "가".repeat(500);
            News longTitleNews = News.builder()
                    .title(veryLongTitle)
                    .content("내용")
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(longTitleNews);

            // then
            assertThat(response.getTitle()).hasSize(500);
        }

        @Test
        @DisplayName("특수문자 포함 - 변환 성공")
        void toResponse_SpecialCharacters_Success() {
            // given
            News specialNews = News.builder()
                    .title("제목!@#$%^&*()")
                    .content("내용<>{}[]")
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(specialNews);

            // then
            assertThat(response.getTitle()).isEqualTo("제목!@#$%^&*()");
            assertThat(response.getContentSummary()).isEqualTo("내용<>{}[]");
        }

        @Test
        @DisplayName("모든 선택 필드 null - 변환 성공")
        void toDetailResponse_AllOptionalFieldsNull_Success() {
            // given
            News minimalNews = News.builder()
                    .title("최소 필드만")
                    .content(null)
                    .source("NAVER_FINANCE")
                    .url(null)
                    .viewCount(null)
                    .sentimentScore(null)
                    .publishedAt(null)
                    .build();

            // when
            NewsDetailResponse response = newsMapper.toDetailResponse(minimalNews);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("최소 필드만");
            assertThat(response.getContentLength()).isEqualTo(0);
            assertThat(response.getSentimentLabel()).isEqualTo("분석 전");
            assertThat(response.getEstimatedReadingTime()).isEqualTo(0);
        }

        @Test
        @DisplayName("공백 문자열 - 변환 성공")
        void toResponse_BlankStrings_Success() {
            // given
            News blankNews = News.builder()
                    .title("   ")
                    .content("   ")
                    .source("NAVER_FINANCE")
                    .build();

            // when
            NewsResponse response = newsMapper.toResponse(blankNews);

            // then
            assertThat(response.getTitle()).isEqualTo("   ");
            assertThat(response.getContentSummary()).isEqualTo("   ");
        }
    }
}
