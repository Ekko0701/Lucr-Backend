package com.lucr.controller;

import com.lucr.common.ApiResponse;
import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Market;
import com.lucr.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 종목 REST API 컨트롤러
 *
 * <h3>엔드포인트 목록</h3>
 * <pre>
 * POST   /api/v1/stocks                — 종목 등록 (ADMIN)
 * GET    /api/v1/stocks                — 전체 종목 목록 (인증 필요)
 * GET    /api/v1/stocks/{code}         — 종목 상세 (인증 필요)
 * DELETE /api/v1/stocks/{code}         — 종목 삭제 (ADMIN)
 * GET    /api/v1/stocks/search         — 종목 검색 (인증 필요)
 * GET    /api/v1/stocks/market/{market} — 시장별 조회 (인증 필요)
 * GET    /api/v1/stocks/{code}/news    — 종목 관련 뉴스 (인증 필요)
 * GET    /api/v1/stocks/exists         — 종목코드 존재 확인 (인증 필요)
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Tag(name = "종목", description = "종목 CRUD, 검색, 시장별 조회")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockService stockService;

    // ==================== CRUD ====================

    /**
     * 종목 등록 (ADMIN 전용)
     *
     * <p>POST /api/v1/stocks</p>
     *
     * @param request 종목 생성 요청 (code, name, market)
     * @return 201 Created + 생성된 종목 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StockResponse>> createStock(
            @Valid @RequestBody StockCreateRequest request
    ) {
        log.info("종목 등록 요청: code={}, name={}", request.getCode(), request.getName());

        StockResponse data = stockService.createStock(request);

        log.info("종목 등록 완료: code={}", data.getCode());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("종목이 성공적으로 등록되었습니다.", data));
    }

    /**
     * 전체 종목 목록 조회 (페이징)
     *
     * <p>GET /api/v1/stocks?page=0&size=50&sort=name</p>
     *
     * @param pageable 페이징 파라미터 (기본: 50개, 이름순)
     * @return 200 OK + 종목 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StockResponse>>> getAllStocks(
            @ParameterObject @PageableDefault(size = 50, sort = "name") Pageable pageable
    ) {
        PageResponse<StockResponse> data = stockService.getAllStocks(pageable);
        return ResponseEntity.ok(ApiResponse.success("요청이 성공적으로 처리되었습니다.", data));
    }

    /**
     * 종목 상세 조회
     *
     * <p>GET /api/v1/stocks/{code}</p>
     *
     * @param code 종목코드 (예: "005930", "AAPL")
     * @return 200 OK + 종목 상세 정보
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<StockResponse>> getStock(
            @PathVariable String code
    ) {
        StockResponse data = stockService.getStockByCode(code);
        return ResponseEntity.ok(ApiResponse.success("요청이 성공적으로 처리되었습니다.", data));
    }

    /**
     * 종목 삭제 (ADMIN 전용)
     *
     * <p>DELETE /api/v1/stocks/{code}</p>
     *
     * @param code 삭제할 종목코드
     * @return 200 OK
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteStock(
            @PathVariable String code
    ) {
        log.info("종목 삭제 요청: code={}", code);

        stockService.deleteStock(code);

        log.info("종목 삭제 완료: code={}", code);
        return ResponseEntity.ok(ApiResponse.success("종목이 성공적으로 삭제되었습니다."));
    }

    // ==================== 검색 ====================

    /**
     * 종목 검색 (종목명 또는 종목코드)
     *
     * <p>GET /api/v1/stocks/search?keyword=삼성&page=0&size=50</p>
     *
     * @param keyword  검색어 (종목명 또는 종목코드)
     * @param pageable 페이징 파라미터
     * @return 200 OK + 검색 결과
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StockResponse>>> searchStocks(
            @RequestParam String keyword,
            @ParameterObject @PageableDefault(size = 50, sort = "name") Pageable pageable
    ) {
        PageResponse<StockResponse> data = stockService.searchStocks(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("요청이 성공적으로 처리되었습니다.", data));
    }

    /**
     * 시장별 종목 조회
     *
     * <p>GET /api/v1/stocks/market/KOSPI?page=0&size=50</p>
     *
     * @param market   시장 구분 (KOSPI, KOSDAQ, NYSE, NASDAQ, AMEX)
     * @param pageable 페이징 파라미터
     * @return 200 OK + 해당 시장 종목 목록
     */
    @GetMapping("/market/{market}")
    public ResponseEntity<ApiResponse<PageResponse<StockResponse>>> getStocksByMarket(
            @PathVariable Market market,
            @ParameterObject @PageableDefault(size = 50, sort = "name") Pageable pageable
    ) {
        PageResponse<StockResponse> data = stockService.getStocksByMarket(market, pageable);
        return ResponseEntity.ok(ApiResponse.success("요청이 성공적으로 처리되었습니다.", data));
    }

    // ==================== 뉴스-종목 관계 ====================

    /**
     * 종목 관련 뉴스 조회
     *
     * <p>GET /api/v1/stocks/005930/news?page=0&size=20</p>
     *
     * @param code     종목코드
     * @param pageable 페이징 파라미터 (기본: 20개, 최신순)
     * @return 200 OK + 관련 뉴스 목록
     */
    @GetMapping("/{code}/news")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getNewsByStock(
            @PathVariable String code,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<NewsResponse> data = stockService.getNewsByStockCode(code, pageable);
        return ResponseEntity.ok(ApiResponse.success("요청이 성공적으로 처리되었습니다.", data));
    }

    // ==================== 유틸 ====================

    /**
     * 종목코드 존재 확인
     *
     * <p>GET /api/v1/stocks/exists?code=005930</p>
     *
     * @param code 확인할 종목코드
     * @return 200 OK + 존재 여부 (true/false)
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkExists(
            @RequestParam String code
    ) {
        boolean exists = stockService.existsByCode(code);
        String message = exists ? "존재하는 종목코드입니다." : "존재하지 않는 종목코드입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }
}
