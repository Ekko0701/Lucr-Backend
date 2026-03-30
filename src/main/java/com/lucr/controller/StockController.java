package com.lucr.controller;

import static com.lucr.config.openapi.OpenApiConstants.FORBIDDEN_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.INVALID_TYPE_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.MISSING_PARAMETER_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.UNAUTHORIZED_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.VALIDATION_ERROR_RESPONSE_REF;

import com.lucr.common.ApiResponse;
import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Market;
import com.lucr.exception.ErrorResponse;
import com.lucr.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "종목 등록", description = "새 종목을 등록합니다. ADMIN 권한이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "종목 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = VALIDATION_ERROR_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 종목코드 (E409005)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
    @Operation(summary = "전체 종목 조회", description = "등록된 전체 종목을 페이징하여 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF)
    })
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
    @Operation(summary = "종목 상세 조회", description = "종목코드로 종목 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "종목을 찾을 수 없음 (E404006)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
    @Operation(summary = "종목 삭제", description = "종목코드로 종목을 삭제합니다. ADMIN 권한이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "종목을 찾을 수 없음 (E404006)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
    @Operation(summary = "종목 검색", description = "종목명 또는 종목코드로 종목을 검색합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = MISSING_PARAMETER_RESPONSE_REF)
    })
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
    @Operation(summary = "시장별 종목 조회", description = "시장 구분(KOSPI, KOSDAQ, NYSE, NASDAQ, AMEX)별로 종목을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF)
    })
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
    @Operation(summary = "종목 관련 뉴스 조회", description = "종목코드에 연결된 뉴스 목록을 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "종목을 찾을 수 없음 (E404006)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
    @Operation(summary = "종목코드 존재 확인", description = "종목코드가 이미 등록되어 있는지 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = MISSING_PARAMETER_RESPONSE_REF)
    })
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkExists(
            @RequestParam String code
    ) {
        boolean exists = stockService.existsByCode(code);
        String message = exists ? "존재하는 종목코드입니다." : "존재하지 않는 종목코드입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }
}
