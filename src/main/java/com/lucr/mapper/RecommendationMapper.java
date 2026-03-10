package com.lucr.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucr.dto.response.RecommendationResponse;
import com.lucr.entity.Recommendation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Recommendation Entity -> Response DTO 변환기.
 *
 * DB에는 reason이 JSON 문자열로 저장되므로, API 응답 시 List<String>으로 파싱한다.
 */
@Component
public class RecommendationMapper {

    /**
     * reason JSON 파싱 전용 ObjectMapper.
     * Spring Bean 주입 대신 이 Mapper 내부 용도로 직접 생성해 사용한다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Recommendation 엔티티를 RecommendationResponse로 변환한다.
     *
     * stock은 LAZY 연관관계이므로, 호출 시점에 영속성 컨텍스트 내에서 접근 가능해야 한다.
     */
    public RecommendationResponse toResponse(Recommendation entity) {
        return RecommendationResponse.builder()
                .id(entity.getId())
                .stockCode(entity.getStock().getCode())
                .stockName(entity.getStock().getName())
                .market(entity.getStock().getMarket().name())
                .score(entity.getScore())
                .confidence(entity.getConfidence())
                .reasons(parseReasons(entity.getReason()))
                .relatedNewsCount(entity.getRelatedNewsCount())
                .avgSentiment(entity.getAvgSentiment())
                .totalMentions(entity.getTotalMentions())
                .updatedAt(entity.getUpdatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }

    /**
     * reason JSON 문자열을 문자열 리스트로 변환한다.
     *
     * - null/blank: 빈 리스트 반환
     * - JSON 파싱 실패: 원문 문자열 1개 리스트로 fallback
     */
    private List<String> parseReasons(String reasonJson) {
        if (reasonJson == null || reasonJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(reasonJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(reasonJson);
        }
    }
}
