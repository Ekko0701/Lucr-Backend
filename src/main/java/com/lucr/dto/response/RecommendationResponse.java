package com.lucr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 추천 조회 응답 DTO.
 *
 * Recommendation 엔티티를 API 응답 형태로 직렬화할 때 사용한다.
 * reason(JSON 문자열)은 Mapper에서 파싱되어 reasons(List)로 내려간다.
 */
@Schema(description = "투자 추천 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    @Schema(description = "추천 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    @Schema(description = "종목코드", example = "005930")
    private String stockCode;
    @Schema(description = "종목명", example = "삼성전자")
    private String stockName;
    @Schema(description = "시장 구분", example = "KOSPI")
    private String market;
    @Schema(description = "추천 점수 (0.000 ~ 1.000)", example = "0.850")
    private BigDecimal score;
    @Schema(description = "추천 신뢰도 (0.00 ~ 1.00)", example = "0.70")
    private BigDecimal confidence;
    @Schema(description = "추천 근거 목록", example = "[\"긍정적 뉴스 다수\", \"거래량 증가\"]")
    private List<String> reasons;
    @Schema(description = "관련 뉴스 건수", example = "7")
    private int relatedNewsCount;
    @Schema(description = "정규화 평균 감정 점수 (0.000 ~ 1.000)", example = "0.720")
    private BigDecimal avgSentiment;
    @Schema(description = "종목 총 언급 수", example = "15")
    private int totalMentions;
    @Schema(description = "마지막 갱신 시각", example = "2026-03-30T06:00:00")
    private LocalDateTime updatedAt;
    @Schema(description = "추천 만료 시각", example = "2026-03-31T06:00:00")
    private LocalDateTime expiresAt;
}
