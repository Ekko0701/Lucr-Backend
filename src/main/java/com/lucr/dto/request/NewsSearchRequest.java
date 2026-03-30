package com.lucr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 뉴스 검색 요청 DTO
 * 
 * 복잡한 검색 조건을 처리하기 위한 DTO
 * - 모든 필드가 선택적 (원하는 조건만 조합 가능)
 * - 페이징 파라미터 포함
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Schema(description = "뉴스 고급 검색 요청 (모든 필드 선택적)")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSearchRequest {

    @Schema(description = "검색 키워드 (제목/본문)", example = "삼성전자")
    private String keyword;

    @Schema(description = "뉴스 출처", example = "NAVER_FINANCE")
    private String source;

    @Schema(description = "최소 조회수", example = "1000")
    @Min(value = 0, message = "최소 조회수는 0 이상이어야 합니다.")
    private Integer minViewCount;

    @Schema(description = "최소 감정 점수 (-1.0 ~ 1.0)", example = "0.5")
    private BigDecimal minSentimentScore;

    @Schema(description = "최대 감정 점수 (-1.0 ~ 1.0)", example = "-0.5")
    private BigDecimal maxSentimentScore;

    @Schema(description = "시작 날짜", example = "2026-03-23T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료 날짜", example = "2026-03-30T23:59:59")
    private LocalDateTime endDate;

    @Schema(
            description = "인기 뉴스 여부 (true: 조회수 1000+, false: 1000 미만, null: 전체)",
            example = "true"
    )
    private Boolean isHighView;

    @Schema(description = "종목 코드 (해당 종목이 언급된 뉴스만)", example = "005930")
    private String stockCode;

    // ========== 페이징 파라미터 ==========

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "페이지 크기", example = "20")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Builder.Default
    private Integer size = 20;

    @Schema(description = "정렬 기준 (예: viewCount,desc / publishedAt,desc)", example = "createdAt,desc")
    @Builder.Default
    private String sort = "createdAt,desc";
    
    /**
     * 사용 예시
     * 
     * // 1. 키워드 검색
     * NewsSearchRequest.builder()
     *     .keyword("삼성전자")
     *     .build();
     * 
     * // 2. 인기 뉴스 (조회수 1000 이상)
     * NewsSearchRequest.builder()
     *     .minViewCount(1000)
     *     .sort("viewCount,desc")
     *     .build();
     * 
     * // 3. 긍정적인 뉴스 (최근 7일)
     * NewsSearchRequest.builder()
     *     .minSentimentScore(BigDecimal.valueOf(0.5))
     *     .startDate(LocalDateTime.now().minusDays(7))
     *     .build();
     * 
     * // 4. 네이버 금융 뉴스 (최신 10개)
     * NewsSearchRequest.builder()
     *     .source("NAVER_FINANCE")
     *     .size(10)
     *     .sort("publishedAt,desc")
     *     .build();
     */
}
