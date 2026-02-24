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

import com.lucr.config.CacheConstants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import com.lucr.config.CacheConstants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

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

    /**
     * 종목 생성
     * 
     * 캐시 전략:
     * - 목록 캐시 전체 삭제 (새 종목이 목록에 포함되어야 함)
     * - stock-list: 전체 종목 목록
     * - stock-market: 시장별 종목 목록
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.STOCK_LIST, allEntries = true),
            @CacheEvict(value = CacheConstants.STOCK_MARKET, allEntries = true)
    })
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

    /**
     * 종목코드로 단건 조회
     * 
     * 캐시 전략:
     * - @Cacheable: 조회 결과를 Redis에 캐시 (TTL: 1시간)
     * - key: 종목코드 (예: "stock::005930")
     * - 종목 정보는 자주 변하지 않으므로 긴 TTL 적용
     */
    @Override
    @Cacheable(value = CacheConstants.STOCK, key = "#code")
    public StockResponse getStockByCode(String code) {
        log.debug("종목 조회 요청: code={}", code);

        Stock stock = findStockByCode(code);
        return stockMapper.toResponse(stock);
    }

    /**
     * 전체 종목 목록 조회 (페이징)
     * 
     * 캐시 전략:
     * - @Cacheable: 페이지별 결과를 Redis에 캐시 (TTL: 1시간)
     * - key: "pageNumber_pageSize" (예: "0_10", "1_20")
     */
    @Override
    @Cacheable(value = CacheConstants.STOCK_LIST,
            key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<StockResponse> getAllStocks(Pageable pageable) {
        log.debug("전체 종목 목록 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Stock> stockPage = stockRepository.findAll(pageable);
        List<StockResponse> responses = stockPage.getContent().stream()
                .map(stock -> stockMapper.toResponse(stock, 0))
                .toList();

        return PageResponse.of(stockPage, responses);
    }

    /**
     * 종목 삭제
     * 
     * 캐시 전략:
     * - 단건 캐시 삭제: 삭제된 종목의 상세 정보 캐시 제거
     * - 목록 캐시 전체 삭제: 삭제된 종목이 제외된 목록으로 갱신
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.STOCK, key = "#code"),
            @CacheEvict(value = CacheConstants.STOCK_LIST, allEntries = true),
            @CacheEvict(value = CacheConstants.STOCK_MARKET, allEntries = true)
    })
    public void deleteStock(String code) {
        log.info("종목 삭제 요청: code={}", code);

        Stock stock = findStockByCode(code);
        stockRepository.delete(stock);

        log.info("종목 삭제 완료: code={}", code);
    }

    // ==================== 검색 ====================

    /**
     * 시장별 종목 목록 조회
     * 
     * 캐시 전략:
     * - @Cacheable: 시장별 결과를 Redis에 캐시 (TTL: 1시간)
     * - key: "market_pageNumber_pageSize" (예: "KOSPI_0_10")
     * - Market enum의 name()을 키로 사용
     */
    @Override
    @Cacheable(value = CacheConstants.STOCK_MARKET,
            key = "#market.name() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
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
