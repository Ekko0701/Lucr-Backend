package com.lucr.dto.response;

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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    /** 추천 ID */
    private UUID id;

    /** 종목 코드 (예: 005930, AAPL) */
    private String stockCode;

    /** 종목명 */
    private String stockName;

    /** 시장 구분 문자열 (예: KOSPI, NASDAQ) */
    private String market;

    /** 추천 점수 (0.000 ~ 1.000) */
    private BigDecimal score;

    /** 추천 신뢰도 (0.00 ~ 1.00) */
    private BigDecimal confidence;

    /** 추천 근거 목록 */
    private List<String> reasons;

    /** 관련 뉴스 건수 */
    private int relatedNewsCount;

    /** 정규화 평균 감정 점수 (0.000 ~ 1.000) */
    private BigDecimal avgSentiment;

    /** 종목 총 언급 수 */
    private int totalMentions;

    /** 마지막 갱신 시각 */
    private LocalDateTime updatedAt;

    /** 추천 만료 시각 */
    private LocalDateTime expiresAt;
}
