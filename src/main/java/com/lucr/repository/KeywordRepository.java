package com.lucr.repository;

import com.lucr.entity.Keyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Keyword Repository — 키워드 데이터 접근 계층
 *
 * @author Ekko0701
 * @since 2026-03-08
 */
@Repository
public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

    /**
     * 단어로 키워드 조회 (word 컬럼 UNIQUE)
     */
    Optional<Keyword> findByWord(String word);

    /**
     * 단어 존재 여부 확인
     */
    boolean existsByWord(String word);

    /**
     * 등장 빈도 기준 상위 키워드 조회 (페이지네이션)
     */
    @Query("SELECT k FROM Keyword k ORDER BY k.frequency DESC")
    Page<Keyword> findTopKeywords(Pageable pageable);

    /**
     * 부분 문자열 키워드 검색 (자동완성)
     */
    List<Keyword> findByWordContaining(String keyword);

    /**
     * 빈도수 기준 상위 N개 키워드 조회
     */
    @Query(
            value = "SELECT * FROM keywords ORDER BY frequency DESC LIMIT :limit",
            nativeQuery = true
    )
    List<Keyword> findTopNKeywords(@Param("limit") int limit);
}
