package com.lucr.service;

import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.*;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.NewsMapper;
import com.lucr.mapper.StockMapper;
import com.lucr.repository.NewsStockRepository;
import com.lucr.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * StockService 비즈니스 로직 테스트
 *
 * Mock 기반 단위 테스트:
 * - Repository, Mapper를 Mock으로 대체
 * - 비즈니스 로직과 예외 처리 집중 테스트
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 테스트")
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private NewsStockRepository newsStockRepository;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private NewsMapper newsMapper;

    @InjectMocks
    private StockServiceImpl stockService;

    private StockCreateRequest createRequest;
    private Stock savedStock;
    private StockResponse stockResponse;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        createRequest = StockCreateRequest.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();

        savedStock = Stock.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        stockResponse = StockResponse.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .newsCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        defaultPageable = PageRequest.of(0, 50);
    }

    // ========== createStock ==========

    @Nested
    @DisplayName("createStock()")
    class CreateStockTests {

        @Test
        @DisplayName("성공 — 새 종목 생성")
        void createStock_Success() {
            // given
            given(stockRepository.existsByCode("005930")).willReturn(false);
            given(stockMapper.toEntity(createRequest)).willReturn(savedStock);
            given(stockRepository.save(savedStock)).willReturn(savedStock);
            given(stockMapper.toResponse(savedStock, 0)).willReturn(stockResponse);

            // when
            StockResponse result = stockService.createStock(createRequest);

            // then
            assertThat(result.getCode()).isEqualTo("005930");
            assertThat(result.getName()).isEqualTo("삼성전자");
            assertThat(result.getNewsCount()).isEqualTo(0);

            then(stockRepository).should(times(1)).existsByCode("005930");
            then(stockRepository).should(times(1)).save(savedStock);
        }

        @Test
        @DisplayName("실패 — 종목코드 중복")
        void createStock_DuplicateCode_ThrowsException() {
            // given
            given(stockRepository.existsByCode("005930")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> stockService.createStock(createRequest))
                    .isInstanceOf(DuplicateResourceException.class);

            then(stockRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("실패 — 종목코드 중복 시 ErrorCode는 DUPLICATE_STOCK_CODE")
        void createStock_DuplicateCode_ErrorCodeVerification() {
            // given
            given(stockRepository.existsByCode("005930")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> stockService.createStock(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_STOCK_CODE);
        }

        @Test
        @DisplayName("성공 — market 필드가 응답에 정확히 포함")
        void createStock_Success_MarketFieldVerified() {
            // given
            StockCreateRequest nasdaqRequest = StockCreateRequest.builder()
                    .code("AAPL")
                    .name("Apple Inc.")
                    .market(Market.NASDAQ)
                    .build();

            Stock nasdaqStock = Stock.builder()
                    .code("AAPL").name("Apple Inc.").market(Market.NASDAQ).build();

            StockResponse nasdaqResponse = StockResponse.builder()
                    .code("AAPL").name("Apple Inc.").market(Market.NASDAQ).newsCount(0).build();

            given(stockRepository.existsByCode("AAPL")).willReturn(false);
            given(stockMapper.toEntity(nasdaqRequest)).willReturn(nasdaqStock);
            given(stockRepository.save(nasdaqStock)).willReturn(nasdaqStock);
            given(stockMapper.toResponse(nasdaqStock, 0)).willReturn(nasdaqResponse);

            // when
            StockResponse result = stockService.createStock(nasdaqRequest);

            // then
            assertThat(result.getMarket()).isEqualTo(Market.NASDAQ);
        }
    }

    // ========== getStockByCode ==========

    @Nested
    @DisplayName("getStockByCode()")
    class GetStockByCodeTests {

        @Test
        @DisplayName("성공 — 종목 조회")
        void getStockByCode_Success() {
            // given
            given(stockRepository.findById("005930")).willReturn(Optional.of(savedStock));
            given(stockMapper.toResponse(savedStock)).willReturn(stockResponse);

            // when
            StockResponse result = stockService.getStockByCode("005930");

            // then
            assertThat(result.getCode()).isEqualTo("005930");
            assertThat(result.getName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("실패 — 종목 없음")
        void getStockByCode_NotFound_ThrowsException() {
            // given
            given(stockRepository.findById("999999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockService.getStockByCode("999999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("실패 — 종목 없음 시 ErrorCode는 STOCK_NOT_FOUND")
        void getStockByCode_NotFound_ErrorCodeVerification() {
            // given
            given(stockRepository.findById("999999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockService.getStockByCode("999999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOCK_NOT_FOUND);
        }
    }

    // ========== getAllStocks ==========

    @Nested
    @DisplayName("getAllStocks()")
    class GetAllStocksTests {

        @Test
        @DisplayName("성공 — 전체 종목 목록 반환")
        void getAllStocks_Success() {
            // given
            Page<Stock> stockPage = new PageImpl<>(List.of(savedStock), defaultPageable, 1);
            given(stockRepository.findAll(defaultPageable)).willReturn(stockPage);
            given(stockMapper.toResponse(savedStock, 0)).willReturn(stockResponse);

            // when
            PageResponse<StockResponse> result = stockService.getAllStocks(defaultPageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCode()).isEqualTo("005930");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("빈 목록 반환")
        void getAllStocks_Empty() {
            // given
            Page<Stock> emptyPage = new PageImpl<>(List.of(), defaultPageable, 0);
            given(stockRepository.findAll(defaultPageable)).willReturn(emptyPage);

            // when
            PageResponse<StockResponse> result = stockService.getAllStocks(defaultPageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("PageResponse 메타데이터 검증 (currentPage, pageSize 등)")
        void getAllStocks_PageResponseMetadata_Verified() {
            // given
            Stock stock2 = Stock.builder().code("035720").name("카카오").market(Market.KOSDAQ).build();
            StockResponse response2 = StockResponse.builder()
                    .code("035720").name("카카오").market(Market.KOSDAQ).newsCount(0).build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Stock> stockPage = new PageImpl<>(List.of(savedStock, stock2), pageable, 2);
            given(stockRepository.findAll(pageable)).willReturn(stockPage);
            given(stockMapper.toResponse(savedStock, 0)).willReturn(stockResponse);
            given(stockMapper.toResponse(stock2, 0)).willReturn(response2);

            // when
            PageResponse<StockResponse> result = stockService.getAllStocks(pageable);

            // then — 메타데이터 검증
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getCurrentPage()).isEqualTo(0);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getIsFirst()).isTrue();
            assertThat(result.getIsLast()).isTrue();
        }
    }

    // ========== deleteStock ==========

    @Nested
    @DisplayName("deleteStock()")
    class DeleteStockTests {

        @Test
        @DisplayName("성공 — 종목 삭제")
        void deleteStock_Success() {
            // given
            given(stockRepository.findById("005930")).willReturn(Optional.of(savedStock));
            willDoNothing().given(stockRepository).delete(savedStock);

            // when
            stockService.deleteStock("005930");

            // then
            then(stockRepository).should(times(1)).delete(savedStock);
        }

        @Test
        @DisplayName("실패 — 종목 없음")
        void deleteStock_NotFound_ThrowsException() {
            // given
            given(stockRepository.findById("999999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockService.deleteStock("999999"))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(stockRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("실패 — 종목 없음 시 ErrorCode는 STOCK_NOT_FOUND")
        void deleteStock_NotFound_ErrorCodeVerification() {
            // given
            given(stockRepository.findById("999999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockService.deleteStock("999999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOCK_NOT_FOUND);
        }
    }

    // ========== getStocksByMarket ==========

    @Nested
    @DisplayName("getStocksByMarket()")
    class GetStocksByMarketTests {

        @Test
        @DisplayName("성공 — KOSPI 종목 목록")
        void getStocksByMarket_Success() {
            // given
            Page<Stock> stockPage = new PageImpl<>(List.of(savedStock), defaultPageable, 1);
            given(stockRepository.findByMarket(Market.KOSPI, defaultPageable)).willReturn(stockPage);
            given(stockMapper.toResponse(savedStock, 0)).willReturn(stockResponse);

            // when
            PageResponse<StockResponse> result = stockService.getStocksByMarket(Market.KOSPI, defaultPageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getMarket()).isEqualTo(Market.KOSPI);
        }

        @Test
        @DisplayName("빈 결과 반환")
        void getStocksByMarket_EmptyResult() {
            // given
            Page<Stock> emptyPage = new PageImpl<>(List.of(), defaultPageable, 0);
            given(stockRepository.findByMarket(Market.AMEX, defaultPageable)).willReturn(emptyPage);

            // when
            PageResponse<StockResponse> result = stockService.getStocksByMarket(Market.AMEX, defaultPageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ========== searchStocks ==========

    @Nested
    @DisplayName("searchStocks()")
    class SearchStocksTests {

        @Test
        @DisplayName("성공 — 키워드로 검색")
        void searchStocks_Success() {
            // given
            Page<Stock> stockPage = new PageImpl<>(List.of(savedStock), defaultPageable, 1);
            given(stockRepository.searchByKeyword("삼성", defaultPageable)).willReturn(stockPage);
            given(stockMapper.toResponse(savedStock, 0)).willReturn(stockResponse);

            // when
            PageResponse<StockResponse> result = stockService.searchStocks("삼성", defaultPageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchStocks_NoMatch() {
            // given
            Page<Stock> emptyPage = new PageImpl<>(List.of(), defaultPageable, 0);
            given(stockRepository.searchByKeyword("존재하지않는", defaultPageable)).willReturn(emptyPage);

            // when
            PageResponse<StockResponse> result = stockService.searchStocks("존재하지않는", defaultPageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ========== getNewsByStockCode ==========

    @Nested
    @DisplayName("getNewsByStockCode()")
    class GetNewsByStockCodeTests {

        @Test
        @DisplayName("성공 — 종목 관련 뉴스 조회")
        void getNewsByStockCode_Success() {
            // given
            News mockNews = News.builder()
                    .id(UUID.randomUUID())
                    .title("삼성전자 주가 상승")
                    .content("삼성전자가 5% 상승했습니다.")
                    .source("NAVER_FINANCE")
                    .url("https://news.example.com/1")
                    .build();

            NewsStock mockNewsStock = NewsStock.builder()
                    .id(new NewsStockId(mockNews.getId(), "005930"))
                    .news(mockNews)
                    .stock(savedStock)
                    .mentionCount(3)
                    .build();

            NewsResponse newsResponse = NewsResponse.builder()
                    .id(mockNews.getId())
                    .title("삼성전자 주가 상승")
                    .build();

            Page<NewsStock> newsStockPage = new PageImpl<>(List.of(mockNewsStock), defaultPageable, 1);

            given(stockRepository.findById("005930")).willReturn(Optional.of(savedStock));
            given(newsStockRepository.findByStockCodeWithNews("005930", defaultPageable)).willReturn(newsStockPage);
            given(newsMapper.toResponse(mockNews)).willReturn(newsResponse);

            // when
            PageResponse<NewsResponse> result = stockService.getNewsByStockCode("005930", defaultPageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("삼성전자 주가 상승");
        }

        @Test
        @DisplayName("실패 — 종목 없음")
        void getNewsByStockCode_StockNotFound() {
            // given
            given(stockRepository.findById("999999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockService.getNewsByStockCode("999999", defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(newsStockRepository).should(never()).findByStockCodeWithNews(anyString(), any());
        }

        @Test
        @DisplayName("종목 존재하지만 관련 뉴스 없음 — 빈 목록 반환")
        void getNewsByStockCode_StockExistsButNoNews() {
            // given
            given(stockRepository.findById("005930")).willReturn(Optional.of(savedStock));
            Page<NewsStock> emptyPage = new PageImpl<>(List.of(), defaultPageable, 0);
            given(newsStockRepository.findByStockCodeWithNews("005930", defaultPageable)).willReturn(emptyPage);

            // when
            PageResponse<NewsResponse> result = stockService.getNewsByStockCode("005930", defaultPageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ========== existsByCode ==========

    @Nested
    @DisplayName("existsByCode()")
    class ExistsByCodeTests {

        @Test
        @DisplayName("존재하는 코드 — true")
        void existsByCode_Exists_ReturnsTrue() {
            // given
            given(stockRepository.existsByCode("005930")).willReturn(true);

            // when
            boolean result = stockService.existsByCode("005930");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 코드 — false")
        void existsByCode_NotExists_ReturnsFalse() {
            // given
            given(stockRepository.existsByCode("999999")).willReturn(false);

            // when
            boolean result = stockService.existsByCode("999999");

            // then
            assertThat(result).isFalse();
        }
    }
}
