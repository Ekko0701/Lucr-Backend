package com.lucr.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Stock Entity 단위 테스트
 *
 * - Builder 기본값 검증
 * - Getter/Setter 검증
 * - newsStocks 초기화 검증
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@DisplayName("Stock Entity 테스트")
class StockTest {

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = Stock.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
    }

    // ========== Builder 기본값 테스트 ==========

    @Nested
    @DisplayName("Builder 기본값")
    class BuilderDefaultTests {

        @Test
        @DisplayName("newsStocks 기본값은 빈 리스트")
        void defaultNewsStocks_IsEmptyList() {
            assertThat(stock.getNewsStocks()).isNotNull();
            assertThat(stock.getNewsStocks()).isEmpty();
        }

        @Test
        @DisplayName("createdAt, updatedAt 기본값은 null (Hibernate가 설정)")
        void defaultTimestamps_AreNull() {
            assertThat(stock.getCreatedAt()).isNull();
            assertThat(stock.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("NoArgsConstructor로 생성 시 newsStocks는 null (@Builder.Default 미적용)")
        void noArgsConstructor_NewsStocksIsNull() {
            // NoArgsConstructor는 @Builder.Default를 적용하지 않음
            Stock noArgsStock = new Stock();

            assertThat(noArgsStock.getNewsStocks()).isEmpty();
            assertThat(noArgsStock.getCode()).isNull();
            assertThat(noArgsStock.getName()).isNull();
            assertThat(noArgsStock.getMarket()).isNull();
        }

        @Test
        @DisplayName("AllArgsConstructor로 모든 필드 지정 가능")
        void allArgsConstructor_AllFieldsSet() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ArrayList<NewsStock> newsStocks = new ArrayList<>();

            // when
            Stock allArgsStock = new Stock(
                    "005930", "삼성전자", Market.KOSPI,
                    now, now, newsStocks
            );

            // then
            assertThat(allArgsStock.getCode()).isEqualTo("005930");
            assertThat(allArgsStock.getName()).isEqualTo("삼성전자");
            assertThat(allArgsStock.getMarket()).isEqualTo(Market.KOSPI);
            assertThat(allArgsStock.getCreatedAt()).isEqualTo(now);
            assertThat(allArgsStock.getUpdatedAt()).isEqualTo(now);
            assertThat(allArgsStock.getNewsStocks()).isSameAs(newsStocks);
        }
    }

    // ========== Getter/Setter 테스트 ==========

    @Nested
    @DisplayName("Getter / Setter")
    class GetterSetterTests {

        @Test
        @DisplayName("모든 필드 정상 접근")
        void allFields_Accessible() {
            assertThat(stock.getCode()).isEqualTo("005930");
            assertThat(stock.getName()).isEqualTo("삼성전자");
            assertThat(stock.getMarket()).isEqualTo(Market.KOSPI);
        }

        @Test
        @DisplayName("name setter로 변경 가능")
        void setName_Works() {
            // when
            stock.setName("삼성전자(수정)");

            // then
            assertThat(stock.getName()).isEqualTo("삼성전자(수정)");
        }

        @Test
        @DisplayName("market setter로 변경 가능")
        void setMarket_Works() {
            // when
            stock.setMarket(Market.KOSDAQ);

            // then
            assertThat(stock.getMarket()).isEqualTo(Market.KOSDAQ);
        }
    }

    // ========== 다양한 시장 Builder 테스트 ==========

    @Nested
    @DisplayName("시장별 종목 생성")
    class MarketVariantTests {

        @Test
        @DisplayName("KOSDAQ 종목 생성")
        void createKosdaqStock() {
            Stock kosdaqStock = Stock.builder()
                    .code("035720")
                    .name("카카오")
                    .market(Market.KOSDAQ)
                    .build();

            assertThat(kosdaqStock.getCode()).isEqualTo("035720");
            assertThat(kosdaqStock.getMarket()).isEqualTo(Market.KOSDAQ);
        }

        @Test
        @DisplayName("NYSE 미국 종목 생성")
        void createNyseStock() {
            Stock nyseStock = Stock.builder()
                    .code("AAPL")
                    .name("Apple Inc.")
                    .market(Market.NYSE)
                    .build();

            assertThat(nyseStock.getCode()).isEqualTo("AAPL");
            assertThat(nyseStock.getMarket()).isEqualTo(Market.NYSE);
        }

        @Test
        @DisplayName("NASDAQ 미국 종목 생성")
        void createNasdaqStock() {
            Stock nasdaqStock = Stock.builder()
                    .code("GOOGL")
                    .name("Alphabet Inc.")
                    .market(Market.NASDAQ)
                    .build();

            assertThat(nasdaqStock.getCode()).isEqualTo("GOOGL");
            assertThat(nasdaqStock.getMarket()).isEqualTo(Market.NASDAQ);
        }

        @Test
        @DisplayName("AMEX 미국 종목 생성")
        void createAmexStock() {
            Stock amexStock = Stock.builder()
                    .code("SPY")
                    .name("SPDR S&P 500 ETF")
                    .market(Market.AMEX)
                    .build();

            assertThat(amexStock.getCode()).isEqualTo("SPY");
            assertThat(amexStock.getMarket()).isEqualTo(Market.AMEX);
        }

        @Test
        @DisplayName("시장별 종목 생성 시 name 필드도 정상 설정")
        void marketVariant_NameVerified() {
            Stock nyseStock = Stock.builder()
                    .code("JPM")
                    .name("JPMorgan Chase & Co.")
                    .market(Market.NYSE)
                    .build();

            assertThat(nyseStock.getName()).isEqualTo("JPMorgan Chase & Co.");
            assertThat(nyseStock.getMarket()).isEqualTo(Market.NYSE);
        }
    }

    // ========== Edge Case 테스트 ==========

    @Nested
    @DisplayName("Edge Case")
    class EdgeCaseTests {

        @Test
        @DisplayName("code, name이 null 또는 빈 문자열이어도 Builder는 예외 없이 생성")
        void builder_WithNullAndEmptyFields() {
            // null 필드
            Stock nullStock = Stock.builder()
                    .code(null)
                    .name(null)
                    .market(null)
                    .build();

            assertThat(nullStock.getCode()).isNull();
            assertThat(nullStock.getName()).isNull();
            assertThat(nullStock.getMarket()).isNull();

            // 빈 문자열
            Stock emptyStock = Stock.builder()
                    .code("")
                    .name("")
                    .market(Market.KOSPI)
                    .build();

            assertThat(emptyStock.getCode()).isEmpty();
            assertThat(emptyStock.getName()).isEmpty();
        }
    }
}
