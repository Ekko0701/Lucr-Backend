package com.lucr.mapper;

import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Stock;
import org.springframework.stereotype.Component;

/**
 * Stock Mapper — Entity ↔ DTO 변환
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Component
public class StockMapper {

    /**
     * 생성 요청 DTO → Entity 변환
     *
     * @param request 종목 생성 요청
     * @return Stock 엔티티
     */
    public Stock toEntity(StockCreateRequest request) {
        return Stock.builder()
                .code(request.getCode())
                .name(request.getName())
                .market(request.getMarket())
                .build();
    }

    /**
     * Entity → 응답 DTO 변환
     *
     * <p>newsCount는 newsStocks 리스트의 크기로 계산한다.
     * Lazy 로딩이므로 트랜잭션 내에서 호출해야 한다.</p>
     *
     * @param stock Stock 엔티티
     * @return StockResponse DTO
     */
    public StockResponse toResponse(Stock stock) {
        return StockResponse.builder()
                .code(stock.getCode())
                .name(stock.getName())
                .market(stock.getMarket())
                .newsCount(stock.getNewsStocks() != null ? stock.getNewsStocks().size() : 0)
                .createdAt(stock.getCreatedAt())
                .build();
    }

    /**
     * Entity → 응답 DTO 변환 (newsCount 직접 지정)
     *
     * <p>목록 조회 시 N+1 문제를 방지하기 위해
     * newsCount를 별도로 전달받는 오버로드 메서드</p>
     *
     * @param stock     Stock 엔티티
     * @param newsCount 관련 뉴스 수
     * @return StockResponse DTO
     */
    public StockResponse toResponse(Stock stock, int newsCount) {
        return StockResponse.builder()
                .code(stock.getCode())
                .name(stock.getName())
                .market(stock.getMarket())
                .newsCount(newsCount)
                .createdAt(stock.getCreatedAt())
                .build();
    }
}
