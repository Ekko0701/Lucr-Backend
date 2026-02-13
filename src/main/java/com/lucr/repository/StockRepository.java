package com.lucr.repository;

import com.lucr.entity.Market;
import com.lucr.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Stock Repository — 종목 데이터 접근 계층
 *
 * <p>제네릭: {@code JpaRepository<Stock, String>}
 * — PK 타입이 UUID가 아니라 {@code String}(종목코드)임에 주의</p>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, String> {

    // ========== 메서드 이름 기반 쿼리 ==========

    /**
     * 시장별 종목 목록 조회
     *
     * 생성되는 SQL:
     * SELECT * FROM stocks WHERE market = ?
     */
    List<Stock> findByMarket(Market market);

    /**
     * 시장별 종목 목록 조회 (페이징)
     *
     * 생성되는 SQL:
     * SELECT * FROM stocks WHERE market = ? LIMIT ? OFFSET ?
     */
    Page<Stock> findByMarket(Market market, Pageable pageable);

    /**
     * 종목명 검색 (부분 일치)
     *
     * 생성되는 SQL:
     * SELECT * FROM stocks WHERE name LIKE %keyword%
     *
     * 예시: findByNameContaining("삼성") → 삼성전자, 삼성SDI, ...
     */
    List<Stock> findByNameContaining(String keyword);

    /**
     * 종목코드 존재 여부 확인
     *
     * 생성되는 SQL:
     * SELECT COUNT(*) > 0 FROM stocks WHERE code = ?
     */
    boolean existsByCode(String code);

    /**
     * 종목명 존재 여부 확인
     *
     * 생성되는 SQL:
     * SELECT COUNT(*) > 0 FROM stocks WHERE name = ?
     */
    boolean existsByName(String name);

    // ========== @Query (JPQL) ==========

    /**
     * 종목명 또는 종목코드로 검색 (페이징)
     *
     * 생성되는 JPQL:
     * SELECT s FROM Stock s
     * WHERE s.name LIKE %keyword% OR s.code LIKE %keyword%
     *
     * 예시: searchByKeyword("삼성") → 종목명에 "삼성" 포함
     *       searchByKeyword("005")  → 종목코드에 "005" 포함
     */
    @Query("SELECT s FROM Stock s WHERE s.name LIKE %:keyword% OR s.code LIKE %:keyword%")
    Page<Stock> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
