package com.lucr.repository;

import com.lucr.entity.NewsStock;
import com.lucr.entity.NewsStockId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * NewsStock Repository — 뉴스-종목 관계 데이터 접근 계층
 *
 * <p>제네릭: {@code JpaRepository<NewsStock, NewsStockId>}
 * — PK 타입이 복합키({@link NewsStockId})임에 주의</p>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Repository
public interface NewsStockRepository extends JpaRepository<NewsStock, NewsStockId> {

    // ========== 종목 기준 조회 ==========

    /**
     * 특정 종목의 관련 NewsStock 목록 조회
     *
     * 생성되는 SQL:
     * SELECT * FROM news_stocks WHERE stock_code = ?
     */
    List<NewsStock> findByStock_Code(String stockCode);

    /**
     * 특정 종목의 관련 뉴스 조회 (페이징, 최신순)
     *
     * JOIN FETCH로 News를 함께 가져와 N+1 문제 방지
     */
    @Query("SELECT ns FROM NewsStock ns JOIN FETCH ns.news WHERE ns.stock.code = :stockCode ORDER BY ns.news.publishedAt DESC")
    Page<NewsStock> findByStockCodeWithNews(@Param("stockCode") String stockCode, Pageable pageable);

    // ========== 뉴스 기준 조회 ==========

    /**
     * 특정 뉴스의 관련 종목 목록 조회
     *
     * 생성되는 SQL:
     * SELECT * FROM news_stocks WHERE news_id = ?
     */
    List<NewsStock> findByNews_Id(UUID newsId);

    /**
     * 특정 뉴스의 관련 종목 조회 (언급 횟수 순, Stock JOIN FETCH)
     */
    @Query("SELECT ns FROM NewsStock ns JOIN FETCH ns.stock WHERE ns.news.id = :newsId ORDER BY ns.mentionCount DESC")
    List<NewsStock> findByNewsIdWithStock(@Param("newsId") UUID newsId);

    // ========== 삭제 ==========

    /**
     * 특정 뉴스의 모든 종목 관계 삭제
     *
     * 생성되는 SQL:
     * DELETE FROM news_stocks WHERE news_id = ?
     */
    void deleteByNews_Id(UUID newsId);

    /**
     * 특정 종목의 모든 뉴스 관계 삭제
     *
     * 생성되는 SQL:
     * DELETE FROM news_stocks WHERE stock_code = ?
     */
    void deleteByStock_Code(String stockCode);
}
