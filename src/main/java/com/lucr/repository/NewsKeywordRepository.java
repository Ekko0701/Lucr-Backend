package com.lucr.repository;

import com.lucr.entity.NewsKeyword;
import com.lucr.entity.NewsKeywordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * NewsKeyword Repository — 뉴스-키워드 관계 데이터 접근 계층
 *
 * <p>PK 타입이 복합키({@link NewsKeywordId})임에 주의하세요.</p>
 *
 * @author Ekko0701
 * @since 2026-03-08
 */
@Repository
public interface NewsKeywordRepository extends JpaRepository<NewsKeyword, NewsKeywordId> {

    /**
     * 특정 뉴스의 키워드 목록 조회 (TF-IDF 내림차순)
     */
    @Query("""
        SELECT nk FROM NewsKeyword nk
        JOIN FETCH nk.keyword
        WHERE nk.news.id = :newsId
        ORDER BY nk.tfidfScore DESC
        """)
    List<NewsKeyword> findByNewsIdWithKeyword(@Param("newsId") UUID newsId);

    /**
     * 특정 키워드가 등장한 뉴스 ID 목록 조회
     */
    @Query("SELECT nk.news.id FROM NewsKeyword nk WHERE nk.keyword.id = :keywordId")
    List<UUID> findNewsIdsByKeywordId(@Param("keywordId") UUID keywordId);

    /**
     * 특정 뉴스의 모든 키워드 관계 삭제
     */
    void deleteByNews_Id(UUID newsId);
}
