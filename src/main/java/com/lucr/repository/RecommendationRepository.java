package com.lucr.repository;

import com.lucr.entity.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Recommendation Repository - 추천 데이터 접근 계층
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /**
     * 종목코드로 추천 조회 (종목당 1건이므로 Optional 반환).
     */
    Optional<Recommendation> findByStock_Code(String stockCode);

    /**
     * 점수 높은 순으로 전체 추천 조회.
     */
    Page<Recommendation> findAllByOrderByScoreDesc(Pageable pageable);

    /**
     * 유효한 추천만 조회 (만료되지 않았거나 만료시각이 없는 추천).
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.expiresAt IS NULL OR r.expiresAt > :now " +
           "ORDER BY r.score DESC")
    Page<Recommendation> findValidRecommendations(
            @Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 최소 신뢰도 이상이면서 유효한 추천만 조회.
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.confidence >= :minConfidence " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) " +
           "ORDER BY r.score DESC")
    Page<Recommendation> findByMinConfidence(
            @Param("minConfidence") BigDecimal minConfidence,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * 특정 종목의 추천 존재 여부.
     */
    boolean existsByStock_Code(String stockCode);

    /**
     * 만료된 추천 삭제.
     *
     * @return 실제 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM Recommendation r " +
           "WHERE r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /**
     * 현재 시각 기준 유효한 추천 개수.
     */
    @Query("SELECT COUNT(r) FROM Recommendation r " +
           "WHERE r.expiresAt IS NULL OR r.expiresAt > :now")
    long countValidRecommendations(@Param("now") LocalDateTime now);
}
