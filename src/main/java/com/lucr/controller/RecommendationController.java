package com.lucr.controller;

import static com.lucr.config.openapi.OpenApiConstants.FORBIDDEN_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.INVALID_TYPE_RESPONSE_REF;
import static com.lucr.config.openapi.OpenApiConstants.UNAUTHORIZED_RESPONSE_REF;

import com.lucr.common.ApiResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.RecommendationResponse;
import com.lucr.exception.ErrorResponse;
import com.lucr.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 투자 추천 REST API 컨트롤러
 *
 * 역할:
 *   Phase 2에서 축적된 뉴스 분석 데이터(감정 점수, 키워드, 종목 언급)를 기반으로
 *   계산된 종목별 추천 점수를 조회하고 갱신하는 엔드포인트 제공
 *
 * 엔드포인트:
 *   GET  /api/v1/recommendations                    - 추천 목록 (점수순)
 *   GET  /api/v1/recommendations?minConfidence=0.5   - 신뢰도 필터링
 *   GET  /api/v1/recommendations/stocks/{stockCode}  - 종목별 추천 상세
 *   POST /api/v1/recommendations/refresh             - 추천 전체 갱신 (ADMIN)
 *
 * @author Ekko0701
 * @since 2026-03-09
 */
@Tag(name = "투자 추천", description = "추천 목록 조회, 종목별 추천, 추천 갱신")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    // ========== 추천 조회 ==========

    /**
     * 추천 종목 목록 조회 (점수 높은 순)
     *
     * 유효한(만료되지 않은) 추천만 반환합니다.
     * minConfidence가 지정되면 해당 신뢰도 이상인 추천만 필터링합니다.
     *
     * @param minConfidence 최소 신뢰도 필터 (선택)
     * @param pageable      페이징 정보 (기본 size=10)
     * @return 200 OK + 추천 목록 (페이징)
     */
    @Operation(summary = "추천 목록 조회", description = "추천 종목을 점수 높은 순으로 페이징 조회합니다. minConfidence를 지정하면 해당 신뢰도 이상만 필터링합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = INVALID_TYPE_RESPONSE_REF)
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecommendationResponse>>> getRecommendations(
            @RequestParam(required = false) BigDecimal minConfidence,
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable) {

        PageResponse<RecommendationResponse> data;

        if (minConfidence != null) {
            log.info("신뢰도 기반 추천 조회 요청: minConfidence={}, page={}, size={}",
                    minConfidence, pageable.getPageNumber(), pageable.getPageSize());
            data = recommendationService.getRecommendationsByConfidence(minConfidence, pageable);
            log.info("신뢰도 기반 추천 조회 완료: totalElements={}", data.getTotalElements());
        } else {
            log.info("추천 목록 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
            data = recommendationService.getRecommendations(pageable);
            log.info("추천 목록 조회 완료: totalElements={}", data.getTotalElements());
        }

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ========== 종목별 추천 상세 ==========

    /**
     * 특정 종목 추천 상세 조회
     *
     * @param stockCode 종목 코드 (예: 005930, AAPL)
     * @return 200 OK + 종목 추천 상세 정보
     */
    @Operation(summary = "종목별 추천 조회", description = "특정 종목코드의 추천 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 종목의 추천 정보 없음 (E404007)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/stocks/{stockCode}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendationByStock(
            @PathVariable String stockCode) {
        log.info("종목별 추천 조회 요청: stockCode={}", stockCode);

        RecommendationResponse data =
                recommendationService.getRecommendationByStockCode(stockCode);

        log.info("종목별 추천 조회 완료: stockCode={}, score={}", stockCode, data.getScore());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ========== 추천 갱신 (ADMIN) ==========

    /**
     * 추천 전체 갱신
     *
     * 모든 종목의 추천 점수를 재계산합니다.
     * SecurityConfig에서 ADMIN 권한만 접근 가능하도록 설정합니다.
     *
     * @return 200 OK + 갱신된 종목 수
     */
    @Operation(summary = "추천 전체 갱신", description = "모든 종목의 추천 점수를 재계산합니다. ADMIN 권한이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE_REF),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", ref = FORBIDDEN_RESPONSE_REF)
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshRecommendations() {
        log.info("추천 전체 갱신 요청");

        int count = recommendationService.refreshAllRecommendations();

        log.info("추천 전체 갱신 완료: {}개 종목", count);
        return ResponseEntity.ok(
                ApiResponse.success(count + "개 종목 추천 갱신 완료"));
    }
}
