package com.lucr.service;

import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Market;
import com.lucr.entity.NewsStock;
import com.lucr.entity.Stock;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.NewsMapper;
import com.lucr.mapper.StockMapper;
import com.lucr.repository.NewsStockRepository;
import com.lucr.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stock Service 구현체 — 종목 비즈니스 로직
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final NewsStockRepository newsStockRepository;
    private final StockMapper stockMapper;
    private final NewsMapper newsMapper;

    // ==================== CRUD ====================

    @Override
    @Transactional
    public StockResponse createStock(StockCreateRequest request) {
        log.info("종목 생성 요청: code={}, name={}, market={}", request.getCode(), request.getName(), request.getMarket());

        // 종목코드 중복 체크
        if (stockRepository.existsByCode(request.getCode())) {
            log.warn("중복된 종목코드로 생성 시도: code={}", request.getCode());
            throw new DuplicateResourceException(ErrorCode.DUPLICATE_STOCK_CODE);
        }

        // DTO → Entity 변환 및 저장
        Stock stock = stockMapper.toEntity(request);
        Stock savedStock = stockRepository.save(stock);

        log.info("종목 생성 완료: code={}, name={}", savedStock.getCode(), savedStock.getName());
        return stockMapper.toResponse(savedStock, 0);
    }

    @Override
    public StockResponse getStockByCode(String code) {
        log.debug("종목 조회 요청: code={}", code);

        Stock stock = findStockByCode(code);
        return stockMapper.toResponse(stock);
    }

    @Override
    public PageResponse<StockResponse> getAllStocks(Pageable pageable) {
        log.debug("전체 종목 목록 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Stock> stockPage = stockRepository.findAll(pageable);
        List<StockResponse> responses = stockPage.getContent().stream()
                .map(stock -> stockMapper.toResponse(stock, 0))
                .toList();

        return PageResponse.of(stockPage, responses);
    }

    @Override
    @Transactional
    public void deleteStock(String code) {
        log.info("종목 삭제 요청: code={}", code);

        Stock stock = findStockByCode(code);
        stockRepository.delete(stock);

        log.info("종목 삭제 완료: code={}", code);
    }

    // ==================== 검색 ====================

    @Override
    public PageResponse<StockResponse> getStocksByMarket(Market market, Pageable pageable) {
        log.debug("시장별 종목 조회 요청: market={}", market);

        Page<Stock> stockPage = stockRepository.findByMarket(market, pageable);
        List<StockResponse> responses = stockPage.getContent().stream()
                .map(stock -> stockMapper.toResponse(stock, 0))
                .toList();

        return PageResponse.of(stockPage, responses);
    }

    @Override
    public PageResponse<StockResponse> searchStocks(String keyword, Pageable pageable) {
        log.debug("종목 검색 요청: keyword={}", keyword);

        Page<Stock> stockPage = stockRepository.searchByKeyword(keyword, pageable);
        List<StockResponse> responses = stockPage.getContent().stream()
                .map(stock -> stockMapper.toResponse(stock, 0))
                .toList();

        return PageResponse.of(stockPage, responses);
    }

    // ==================== 뉴스-종목 관계 ====================

    @Override
    public PageResponse<NewsResponse> getNewsByStockCode(String stockCode, Pageable pageable) {
        log.debug("종목 관련 뉴스 조회 요청: stockCode={}", stockCode);

        // 종목 존재 여부 확인
        findStockByCode(stockCode);

        Page<NewsStock> newsStockPage = newsStockRepository.findByStockCodeWithNews(stockCode, pageable);
        List<NewsResponse> responses = newsStockPage.getContent().stream()
                .map(newsStock -> newsMapper.toResponse(newsStock.getNews()))
                .toList();

        return PageResponse.of(newsStockPage, responses);
    }

    // ==================== 유틸 ====================

    @Override
    public boolean existsByCode(String code) {
        return stockRepository.existsByCode(code);
    }

    // ==================== private 헬퍼 ====================

    /**
     * 종목코드로 Stock 엔티티 조회 (없으면 ResourceNotFoundException)
     */
    private Stock findStockByCode(String code) {
        return stockRepository.findById(code)
                .orElseThrow(() -> {
                    log.error("종목을 찾을 수 없음: code={}", code);
                    return new ResourceNotFoundException(ErrorCode.STOCK_NOT_FOUND);
                });
    }
}
