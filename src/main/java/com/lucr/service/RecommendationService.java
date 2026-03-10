package com.lucr.service;

import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.RecommendationResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * 추천 비즈니스 로직 서비스 인터페이스.
 */
public interface RecommendationService {

    /**
     * 추천 목록 조회 (점수 높은 순, 유효한 것만).
     */
    PageResponse<RecommendationResponse> getRecommendations(Pageable pageable);

    /**
     * 최소 신뢰도 필터링 추천 조회.
     */
    PageResponse<RecommendationResponse> getRecommendationsByConfidence(
            BigDecimal minConfidence, Pageable pageable);

    /**
     * 특정 종목 추천 상세 조회.
     */
    RecommendationResponse getRecommendationByStockCode(String stockCode);

    /**
     * 추천 점수 전체 갱신 (크롤링 완료 후 트리거).
     */
    int refreshAllRecommendations();

    /**
     * 만료된 추천 정리.
     */
    int cleanupExpiredRecommendations();
}
