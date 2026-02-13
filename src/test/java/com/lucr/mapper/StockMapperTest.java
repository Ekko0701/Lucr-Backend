package com.lucr.mapper;

import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Market;
import com.lucr.entity.NewsStock;
import com.lucr.entity.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * StockMapper 단위 테스트
 *
 * - toEntity: StockCreateRequest → Stock 변환 검증
 * - toResponse(Stock): Entity → DTO 변환 (newsCount 자동 계산)
 * - toResponse(Stock, int): Entity → DTO 변환 (newsCount 직접 지정)
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@DisplayName("StockMapper 테스트")
class StockMapperTest {

    private StockMapper stockMapper;

    @BeforeEach
    void setUp() {
        stockMapper = new StockMapper();
    }

    // ========== toEntity 테스트 ==========

    @Nested
    @DisplayName("toEntity()")
    class ToEntityTests {

        @Test
        @DisplayName("StockCreateRequest → Stock 변환 성공")
        void toEntity_Success() {
            // given
            StockCreateRequest request = StockCreateRequest.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // when
            Stock stock = stockMapper.toEntity(request);

            // then
            assertThat(stock.getCode()).isEqualTo("005930");
            assertThat(stock.getName()).isEqualTo("삼성전자");
            assertThat(stock.getMarket()).isEqualTo(Market.KOSPI);
        }

        @Test
        @DisplayName("변환 시 자동생성 필드는 null")
        void toEntity_AutoFieldsAreNull() {
            // given
            StockCreateRequest request = StockCreateRequest.builder()
                    .code("AAPL")
                    .name("Apple Inc.")
                    .market(Market.NASDAQ)
                    .build();

            // when
            Stock stock = stockMapper.toEntity(request);

            // then
            assertThat(stock.getCreatedAt()).isNull();
            assertThat(stock.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("변환 시 newsStocks는 빈 리스트로 초기화")
        void toEntity_NewsStocksIsEmpty() {
            // given
            StockCreateRequest request = StockCreateRequest.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // when
            Stock stock = stockMapper.toEntity(request);

            // then
            assertThat(stock.getNewsStocks()).isNotNull();
            assertThat(stock.getNewsStocks()).isEmpty();
        }

        @Test
        @DisplayName("null 요청 → NullPointerException")
        void toEntity_NullRequest_ThrowsNPE() {
            assertThatThrownBy(() -> stockMapper.toEntity(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ========== toResponse(Stock) 테스트 ==========

    @Nested
    @DisplayName("toResponse(Stock)")
    class ToResponseTests {

        @Test
        @DisplayName("newsStocks가 비어있으면 newsCount = 0")
        void toResponse_EmptyNewsStocks_NewsCountZero() {
            // given
            Stock stock = Stock.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // when
            StockResponse response = stockMapper.toResponse(stock);

            // then
            assertThat(response.getCode()).isEqualTo("005930");
            assertThat(response.getName()).isEqualTo("삼성전자");
            assertThat(response.getMarket()).isEqualTo(Market.KOSPI);
            assertThat(response.getNewsCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("newsStocks에 항목이 있으면 newsCount = size()")
        void toResponse_WithNewsStocks_NewsCountCalculated() {
            // given
            Stock stock = Stock.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // newsStocks 리스트에 Mock 데이터 추가
            List<NewsStock> newsStocks = new ArrayList<>();
            newsStocks.add(NewsStock.builder().build());
            newsStocks.add(NewsStock.builder().build());
            newsStocks.add(NewsStock.builder().build());
            stock.setNewsStocks(newsStocks);

            // when
            StockResponse response = stockMapper.toResponse(stock);

            // then
            assertThat(response.getNewsCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("newsStocks가 null이면 newsCount = 0")
        void toResponse_NullNewsStocks_NewsCountZero() {
            // given
            Stock stock = Stock.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();
            stock.setNewsStocks(null);

            // when
            StockResponse response = stockMapper.toResponse(stock);

            // then
            assertThat(response.getNewsCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("createdAt 필드 변환")
        void toResponse_CreatedAtMapped() {
            // given
            LocalDateTime now = LocalDateTime.of(2026, 2, 12, 10, 0, 0);
            Stock stock = Stock.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .createdAt(now)
                    .build();

            // when
            StockResponse response = stockMapper.toResponse(stock);

            // then
            assertThat(response.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("null Stock → NullPointerException")
        void toResponse_NullStock_ThrowsNPE() {
            assertThatThrownBy(() -> stockMapper.toResponse((Stock) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ========== toResponse(Stock, int) 오버로드 테스트 ==========

    @Nested
    @DisplayName("toResponse(Stock, int)")
    class ToResponseWithNewsCountTests {

        @Test
        @DisplayName("newsCount를 직접 지정하여 변환")
        void toResponse_WithExplicitNewsCount() {
            // given
            Stock stock = Stock.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // when
            StockResponse response = stockMapper.toResponse(stock, 42);

            // then
            assertThat(response.getCode()).isEqualTo("005930");
            assertThat(response.getNewsCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("newsCount 0으로 지정 (N+1 방지 목적)")
        void toResponse_WithZeroNewsCount() {
            // given
            Stock stock = Stock.builder()
                    .code("AAPL")
                    .name("Apple Inc.")
                    .market(Market.NYSE)
                    .build();

            // when
            StockResponse response = stockMapper.toResponse(stock, 0);

            // then
            assertThat(response.getNewsCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("null Stock + newsCount → NullPointerException")
        void toResponse_NullStock_WithCount_ThrowsNPE() {
            assertThatThrownBy(() -> stockMapper.toResponse(null, 5))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("음수 newsCount — 그대로 반환 (유효성 검증 없음)")
        void toResponse_NegativeNewsCount() {
            // given
            Stock stock = Stock.builder()
                    .code("005930")
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // when
            StockResponse response = stockMapper.toResponse(stock, -1);

            // then — Mapper는 유효성 검증을 하지 않음
            assertThat(response.getNewsCount()).isEqualTo(-1);
        }

        @Test
        @DisplayName("모든 필드 변환 검증 (code, name, market, newsCount, createdAt)")
        void toResponse_WithExplicitNewsCount_AllFieldsVerified() {
            // given
            LocalDateTime createdAt = LocalDateTime.of(2026, 2, 12, 15, 30);
            Stock stock = Stock.builder()
                    .code("AAPL")
                    .name("Apple Inc.")
                    .market(Market.NASDAQ)
                    .createdAt(createdAt)
                    .build();

            // when
            StockResponse response = stockMapper.toResponse(stock, 7);

            // then
            assertThat(response.getCode()).isEqualTo("AAPL");
            assertThat(response.getName()).isEqualTo("Apple Inc.");
            assertThat(response.getMarket()).isEqualTo(Market.NASDAQ);
            assertThat(response.getNewsCount()).isEqualTo(7);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    // ========== Market enum 전체 테스트 ==========

    @Nested
    @DisplayName("Market enum 전체")
    class MarketEnumTests {

        @ParameterizedTest
        @EnumSource(Market.class)
        @DisplayName("모든 Market enum 값으로 변환 성공")
        void toEntity_AllMarkets_Success(Market market) {
            // given
            StockCreateRequest request = StockCreateRequest.builder()
                    .code("TEST")
                    .name("테스트 종목")
                    .market(market)
                    .build();

            // when
            Stock stock = stockMapper.toEntity(request);

            // then
            assertThat(stock.getMarket()).isEqualTo(market);
        }
    }
}
