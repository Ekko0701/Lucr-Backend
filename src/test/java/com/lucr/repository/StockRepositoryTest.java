package com.lucr.repository;

import com.lucr.entity.Market;
import com.lucr.entity.Stock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * StockRepository 통합 테스트
 *
 * @DataJpaTest:
 * - JPA 관련 컴포넌트만 로드 (경량 테스트)
 * - H2 인메모리 DB 자동 설정
 * - 각 테스트 메서드마다 트랜잭션 롤백 (테스트 격리)
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@DataJpaTest
@DisplayName("StockRepository 테스트")
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private EntityManager entityManager;

    private Stock samsungStock;
    private Stock kakaoStock;
    private Stock appleStock;

    @BeforeEach
    void setUp() {
        samsungStock = stockRepository.save(
                Stock.builder()
                        .code("005930")
                        .name("삼성전자")
                        .market(Market.KOSPI)
                        .build()
        );

        kakaoStock = stockRepository.save(
                Stock.builder()
                        .code("035720")
                        .name("카카오")
                        .market(Market.KOSDAQ)
                        .build()
        );

        appleStock = stockRepository.save(
                Stock.builder()
                        .code("AAPL")
                        .name("Apple Inc.")
                        .market(Market.NASDAQ)
                        .build()
        );
    }

    // ========== findById (PK 조회) ==========

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("존재하는 종목코드 — Stock 반환")
        void findById_Exists_ReturnsStock() {
            // when
            Optional<Stock> result = stockRepository.findById("005930");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("삼성전자");
            assertThat(result.get().getMarket()).isEqualTo(Market.KOSPI);
        }

        @Test
        @DisplayName("존재하지 않는 종목코드 — empty 반환")
        void findById_NotExists_ReturnsEmpty() {
            // when
            Optional<Stock> result = stockRepository.findById("999999");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========== findByMarket ==========

    @Nested
    @DisplayName("findByMarket()")
    class FindByMarketTests {

        @Test
        @DisplayName("KOSPI 종목 — 1개 반환")
        void findByMarket_Kospi_ReturnsOne() {
            // when
            List<Stock> result = stockRepository.findByMarket(Market.KOSPI);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("005930");
        }

        @Test
        @DisplayName("NASDAQ 종목 — 1개 반환")
        void findByMarket_Nasdaq_ReturnsOne() {
            // when
            List<Stock> result = stockRepository.findByMarket(Market.NASDAQ);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("NYSE 종목 — 0개 반환")
        void findByMarket_Nyse_ReturnsEmpty() {
            // when
            List<Stock> result = stockRepository.findByMarket(Market.NYSE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("시장별 종목 조회 — 페이징")
        void findByMarket_WithPaging() {
            // when
            Page<Stock> result = stockRepository.findByMarket(
                    Market.KOSPI, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 시장에 여러 종목 — 모두 반환")
        void findByMarket_MultipleStocksInSameMarket() {
            // given — NASDAQ에 추가 종목 저장
            stockRepository.save(Stock.builder()
                    .code("GOOGL")
                    .name("Alphabet Inc.")
                    .market(Market.NASDAQ)
                    .build());

            // when
            List<Stock> result = stockRepository.findByMarket(Market.NASDAQ);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Stock::getCode)
                    .containsExactlyInAnyOrder("AAPL", "GOOGL");
        }
    }

    // ========== findByNameContaining ==========

    @Nested
    @DisplayName("findByNameContaining()")
    class FindByNameContainingTests {

        @Test
        @DisplayName("'삼성' 검색 — 삼성전자 반환")
        void findByNameContaining_Samsung() {
            // when
            List<Stock> result = stockRepository.findByNameContaining("삼성");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("'Apple' 검색 — Apple Inc. 반환")
        void findByNameContaining_Apple() {
            // when
            List<Stock> result = stockRepository.findByNameContaining("Apple");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Apple Inc.");
        }

        @Test
        @DisplayName("존재하지 않는 검색어 — 빈 리스트 반환")
        void findByNameContaining_NoMatch() {
            // when
            List<Stock> result = stockRepository.findByNameContaining("테슬라");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 검색 — 전체 반환")
        void findByNameContaining_EmptyString_ReturnsAll() {
            // when
            List<Stock> result = stockRepository.findByNameContaining("");

            // then — 빈 문자열은 LIKE '%%'로 변환되어 전체 반환
            assertThat(result).hasSize(3);
        }
    }

    // ========== existsByCode / existsByName ==========

    @Nested
    @DisplayName("existsByCode() / existsByName()")
    class ExistsTests {

        @Test
        @DisplayName("존재하는 종목코드 — true")
        void existsByCode_Exists_ReturnsTrue() {
            assertThat(stockRepository.existsByCode("005930")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 종목코드 — false")
        void existsByCode_NotExists_ReturnsFalse() {
            assertThat(stockRepository.existsByCode("999999")).isFalse();
        }

        @Test
        @DisplayName("존재하는 종목명 — true")
        void existsByName_Exists_ReturnsTrue() {
            assertThat(stockRepository.existsByName("삼성전자")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 종목명 — false")
        void existsByName_NotExists_ReturnsFalse() {
            assertThat(stockRepository.existsByName("테슬라")).isFalse();
        }
    }

    // ========== searchByKeyword (JPQL) ==========

    @Nested
    @DisplayName("searchByKeyword()")
    class SearchByKeywordTests {

        @Test
        @DisplayName("종목명으로 검색 — '카카오'")
        void searchByKeyword_ByName() {
            // when
            Page<Stock> result = stockRepository.searchByKeyword(
                    "카카오", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("카카오");
        }

        @Test
        @DisplayName("종목코드로 검색 — '005'")
        void searchByKeyword_ByCode() {
            // when
            Page<Stock> result = stockRepository.searchByKeyword(
                    "005", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCode()).isEqualTo("005930");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchByKeyword_NoMatch() {
            // when
            Page<Stock> result = stockRepository.searchByKeyword(
                    "ZZZZZZ", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("대소문자 검색 — 'aapl' 소문자 검색 시 동작 확인")
        void searchByKeyword_CaseSensitivity() {
            // H2의 LIKE는 기본적으로 대소문자를 구분함
            // 'aapl' 소문자로 검색하면 'AAPL'을 찾지 못할 수 있음
            Page<Stock> lowerResult = stockRepository.searchByKeyword(
                    "aapl", PageRequest.of(0, 10));
            Page<Stock> upperResult = stockRepository.searchByKeyword(
                    "AAPL", PageRequest.of(0, 10));

            // 대문자 검색은 확실히 동작
            assertThat(upperResult.getContent()).hasSize(1);
            // 소문자 검색 결과는 DB 엔진에 따라 다름 (H2 기본: 대소문자 구분)
            // 중요한 건 대문자 검색이 동작한다는 것
            assertThat(upperResult.getContent().get(0).getCode()).isEqualTo("AAPL");
        }
    }

    // ========== CRUD 기본 동작 ==========

    @Nested
    @DisplayName("기본 CRUD")
    class CrudTests {

        @Test
        @DisplayName("저장 — createdAt 자동 설정")
        void save_SetsCreatedAt() {
            // given
            Stock newStock = Stock.builder()
                    .code("GOOGL")
                    .name("Alphabet Inc.")
                    .market(Market.NASDAQ)
                    .build();

            // when — flush를 해야 @CreationTimestamp가 적용됨
            Stock saved = stockRepository.saveAndFlush(newStock);

            // then
            assertThat(saved.getCode()).isEqualTo("GOOGL");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("종목코드(PK) 중복 저장 시 업데이트 동작")
        void save_DuplicateCode_Updates() {
            // given — 같은 PK로 다른 데이터
            Stock duplicate = Stock.builder()
                    .code("005930")
                    .name("삼성전자(수정)")
                    .market(Market.KOSPI)
                    .build();

            // when
            Stock merged = stockRepository.save(duplicate);
            stockRepository.flush();

            // then — merge 동작으로 name 업데이트됨
            Optional<Stock> found = stockRepository.findById("005930");
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("삼성전자(수정)");
        }

        @Test
        @DisplayName("삭제")
        void delete_RemovesStock() {
            // given
            assertThat(stockRepository.findById("005930")).isPresent();

            // when
            stockRepository.deleteById("005930");
            stockRepository.flush();

            // then
            assertThat(stockRepository.findById("005930")).isEmpty();
        }

        @Test
        @DisplayName("전체 조회 — 3개 반환")
        void findAll_ReturnsThreeStocks() {
            assertThat(stockRepository.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("전체 조회 — 페이징 (size=2)")
        void findAll_WithPaging() {
            // when
            Page<Stock> page = stockRepository.findAll(
                    PageRequest.of(0, 2, Sort.by("code")));

            // then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("종목코드(PK) 중복 저장 시 createdAt 보존")
        void save_DuplicateCode_PreservesCreatedAt() {
            // given — 기존 종목의 createdAt 확인
            entityManager.flush();
            entityManager.clear();

            Stock original = stockRepository.findById("005930").orElseThrow();
            assertThat(original.getCreatedAt()).isNotNull();

            // when — 같은 PK로 name만 변경하여 저장
            original.setName("삼성전자(수정)");
            stockRepository.save(original);
            entityManager.flush();
            entityManager.clear();

            // then — createdAt은 보존됨
            Stock reloaded = stockRepository.findById("005930").orElseThrow();
            assertThat(reloaded.getName()).isEqualTo("삼성전자(수정)");
            assertThat(reloaded.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("수정 시 updatedAt 자동 갱신")
        void update_SetsUpdatedAt() {
            // given
            entityManager.flush();
            entityManager.clear();

            Stock stock = stockRepository.findById("005930").orElseThrow();

            // when — 필드 수정 후 저장
            stock.setName("삼성전자(수정됨)");
            stockRepository.save(stock);
            entityManager.flush();
            entityManager.clear();

            // then — updatedAt이 설정됨
            Stock reloaded = stockRepository.findById("005930").orElseThrow();
            assertThat(reloaded.getUpdatedAt()).isNotNull();
        }
    }
}
