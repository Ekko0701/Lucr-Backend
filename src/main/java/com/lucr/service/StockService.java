package com.lucr.service;

import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Market;
import org.springframework.data.domain.Pageable;

/**
 * Stock Service 인터페이스 — 종목 비즈니스 로직 정의
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
public interface StockService {

    // === CRUD ===

    StockResponse createStock(StockCreateRequest request);

    StockResponse getStockByCode(String code);

    PageResponse<StockResponse> getAllStocks(Pageable pageable);

    void deleteStock(String code);

    // === 검색 ===

    PageResponse<StockResponse> getStocksByMarket(Market market, Pageable pageable);

    PageResponse<StockResponse> searchStocks(String keyword, Pageable pageable);

    // === 뉴스-종목 관계 ===

    PageResponse<NewsResponse> getNewsByStockCode(String stockCode, Pageable pageable);

    // === 유틸 ===

    boolean existsByCode(String code);
}
