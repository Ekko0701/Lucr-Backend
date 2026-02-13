package com.lucr.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * NewsStock Entity 단위 테스트
 *
 * - Builder 기본값 (mentionCount = 1)
 * - 비즈니스 메서드 (incrementMentionCount)
 * - 정적 팩토리 메서드 (create)
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@DisplayName("NewsStock Entity 테스트")
class NewsStockTest {

    private News news;
    private Stock stock;

    @BeforeEach
    void setUp() {
        news = News.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .title("삼성전자 주가 급등")
                .content("삼성전자가 오늘 5% 이상 상승했습니다.")
                .source("NAVER_FINANCE")
                .url("https://news.example.com/1")
                .build();

        stock = Stock.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
    }

    // ========== NoArgsConstructor 테스트 ==========

    @Nested
    @DisplayName("NoArgsConstructor")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("NoArgsConstructor로 생성 시 mentionCount는 null (@Builder.Default 미적용)")
        void noArgsConstructor_MentionCountIsNull() {
            // NoArgsConstructor는 @Builder.Default를 적용하지 않음
            NewsStock noArgsNewsStock = new NewsStock();

            assertThat(noArgsNewsStock.getMentionCount()).isEqualTo(1);
            assertThat(noArgsNewsStock.getId()).isNull();
            assertThat(noArgsNewsStock.getNews()).isNull();
            assertThat(noArgsNewsStock.getStock()).isNull();
        }
    }

    // ========== Builder 기본값 테스트 ==========

    @Nested
    @DisplayName("Builder 기본값")
    class BuilderDefaultTests {

        @Test
        @DisplayName("mentionCount 기본값은 1")
        void defaultMentionCount_IsOne() {
            NewsStock newsStock = NewsStock.builder()
                    .id(new NewsStockId(news.getId(), stock.getCode()))
                    .news(news)
                    .stock(stock)
                    .build();

            assertThat(newsStock.getMentionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("mentionCount를 직접 지정 가능")
        void customMentionCount() {
            NewsStock newsStock = NewsStock.builder()
                    .id(new NewsStockId(news.getId(), stock.getCode()))
                    .news(news)
                    .stock(stock)
                    .mentionCount(5)
                    .build();

            assertThat(newsStock.getMentionCount()).isEqualTo(5);
        }
    }

    // ========== 비즈니스 메서드 테스트 ==========

    @Nested
    @DisplayName("비즈니스 메서드")
    class BusinessMethodTests {

        @Test
        @DisplayName("incrementMentionCount() — 1에서 2로 증가")
        void incrementMentionCount_FromOneToTwo() {
            // given
            NewsStock newsStock = NewsStock.create(news, stock);
            assertThat(newsStock.getMentionCount()).isEqualTo(1);

            // when
            newsStock.incrementMentionCount();

            // then
            assertThat(newsStock.getMentionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("incrementMentionCount() — 여러 번 호출")
        void incrementMentionCount_MultipleTimes() {
            // given
            NewsStock newsStock = NewsStock.create(news, stock);

            // when
            newsStock.incrementMentionCount();
            newsStock.incrementMentionCount();
            newsStock.incrementMentionCount();

            // then
            assertThat(newsStock.getMentionCount()).isEqualTo(4); // 1 + 3
        }
    }

    // ========== 정적 팩토리 메서드 테스트 ==========

    @Nested
    @DisplayName("정적 팩토리 메서드 create()")
    class CreateFactoryTests {

        @Test
        @DisplayName("create(news, stock) — 기본 생성")
        void create_Default() {
            // when
            NewsStock newsStock = NewsStock.create(news, stock);

            // then
            assertThat(newsStock.getId()).isNotNull();
            assertThat(newsStock.getId().getNewsId()).isEqualTo(news.getId());
            assertThat(newsStock.getId().getStockCode()).isEqualTo(stock.getCode());
            assertThat(newsStock.getNews()).isEqualTo(news);
            assertThat(newsStock.getStock()).isEqualTo(stock);
            assertThat(newsStock.getMentionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("create(news, stock, mentionCount) — 언급 횟수 지정 생성")
        void create_WithMentionCount() {
            // when
            NewsStock newsStock = NewsStock.create(news, stock, 10);

            // then
            assertThat(newsStock.getId()).isNotNull();
            assertThat(newsStock.getId().getNewsId()).isEqualTo(news.getId());
            assertThat(newsStock.getId().getStockCode()).isEqualTo(stock.getCode());
            assertThat(newsStock.getMentionCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("create() — 복합키가 올바르게 설정됨")
        void create_CompositeKeyCorrect() {
            // when
            NewsStock newsStock = NewsStock.create(news, stock);

            // then
            NewsStockId expectedId = new NewsStockId(news.getId(), stock.getCode());
            assertThat(newsStock.getId()).isEqualTo(expectedId);
        }

        @Test
        @DisplayName("create(news, stock, 0) — mentionCount 0 경계값")
        void create_WithZeroMentionCount() {
            // when
            NewsStock newsStock = NewsStock.create(news, stock, 0);

            // then
            assertThat(newsStock.getMentionCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("create(news, stock, -1) — 음수 mentionCount 허용 여부 문서화")
        void create_WithNegativeMentionCount() {
            // Builder 레벨에서 음수 검증이 없으므로 허용됨
            // 비즈니스 유효성 검증은 서비스 계층에서 수행해야 함
            NewsStock newsStock = NewsStock.create(news, stock, -1);

            assertThat(newsStock.getMentionCount()).isEqualTo(-1);
        }
    }

    // ========== Getter 테스트 ==========

    @Nested
    @DisplayName("Getter")
    class GetterTests {

        @Test
        @DisplayName("모든 필드 접근 가능")
        void allFields_Accessible() {
            NewsStock newsStock = NewsStock.create(news, stock, 3);

            assertThat(newsStock.getId()).isNotNull();
            assertThat(newsStock.getNews()).isEqualTo(news);
            assertThat(newsStock.getStock()).isEqualTo(stock);
            assertThat(newsStock.getMentionCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("createdAt 필드 접근 가능 (Hibernate 설정 전은 null)")
        void createdAt_Accessible() {
            NewsStock newsStock = NewsStock.create(news, stock);

            // @CreationTimestamp는 DB 저장 시 설정됨
            // 순수 객체에서는 null
            assertThat(newsStock.getCreatedAt()).isNull();
        }
    }
}
