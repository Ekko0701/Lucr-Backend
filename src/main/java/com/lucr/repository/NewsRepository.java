package com.lucr.repository;

import com.lucr.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * News Repository - 뉴스 데이터 접근 계층
 * 
 * Spring Data JPA가 제공하는 기능:
 * 1. 기본 CRUD 메서드 자동 제공 (save, findById, findAll, delete 등)
 * 2. 메서드 이름 기반 쿼리 자동 생성
 * 3. @Query 어노테이션으로 커스텀 쿼리 작성
 * 4. 페이징 및 정렬 지원
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {
    
    // ========== 1. 기본 CRUD (JpaRepository가 자동 제공) ==========
    // save(news)           - INSERT/UPDATE
    // findById(id)         - SELECT by ID
    // findAll()            - SELECT ALL
    // delete(news)         - DELETE
    // count()              - COUNT
    // existsById(id)       - EXISTS
    
    
    // ========== 2. 메서드 이름 기반 쿼리 (Query Methods) ==========
    
    /**
     * URL로 뉴스 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news WHERE url = ?
     * 
     * 용도: 같은 URL의 뉴스가 이미 있는지 확인 (중복 크롤링 방지)
     */
    Optional<News> findByUrl(String url);
    
    /**
     * 뉴스 출처로 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news WHERE source = ?
     * 
     * 예시: findBySource("NAVER_FINANCE")
     */
    List<News> findBySource(String source);
    
    /**
     * 제목에 키워드가 포함된 뉴스 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news WHERE title LIKE %keyword%
     * 
     * 예시: findByTitleContaining("삼성전자")
     */
    List<News> findByTitleContaining(String keyword);
    
    /**
     * 고조회수 뉴스 조회 (조회수 >= 1000)
     * 
     * 생성되는 SQL:
     * SELECT * FROM news WHERE is_high_view = true
     */
    List<News> findByIsHighView(Boolean isHighView);
    
    /**
     * 조회수가 특정 값 이상인 뉴스 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news WHERE view_count >= ?
     * 
     * 예시: findByViewCountGreaterThanEqual(1000)
     */
    List<News> findByViewCountGreaterThanEqual(Integer viewCount);
    
    /**
     * 감정 점수가 특정 범위 내인 뉴스 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * WHERE sentiment_score BETWEEN ? AND ?
     * 
     * 예시: findBySentimentScoreBetween(0.5, 1.0) → 긍정적인 뉴스
     */
    List<News> findBySentimentScoreBetween(BigDecimal min, BigDecimal max);
    
    /**
     * 특정 날짜 이후 발행된 뉴스 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news WHERE published_at >= ?
     * 
     * 예시: findByPublishedAtAfter(LocalDateTime.now().minusDays(7))
     */
    List<News> findByPublishedAtAfter(LocalDateTime publishedAt);
    
    /**
     * 출처와 발행일 기준으로 조회 (복합 조건)
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * WHERE source = ? AND published_at >= ?
     */
    List<News> findBySourceAndPublishedAtAfter(String source, LocalDateTime publishedAt);
    
    /**
     * 조회수 기준 내림차순 정렬 + 페이징
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * ORDER BY view_count DESC 
     * LIMIT ? OFFSET ?
     * 
     * 용도: "인기 뉴스" 페이지
     */
    Page<News> findAllByOrderByViewCountDesc(Pageable pageable);
    
    /**
     * 발행일 기준 내림차순 정렬 + 페이징
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * ORDER BY published_at DESC 
     * LIMIT ? OFFSET ?
     * 
     * 용도: "최신 뉴스" 페이지
     */
    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    
    // ========== 3. @Query 어노테이션 (JPQL - 커스텀 쿼리) ==========
    
    /**
     * 제목 또는 본문에 키워드가 포함된 뉴스 검색
     * 
     * JPQL: Entity 이름과 필드 이름 사용 (테이블/컬럼 이름 X)
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * WHERE title LIKE %keyword% OR content LIKE %keyword%
     */
    @Query("SELECT n FROM News n WHERE n.title LIKE %:keyword% OR n.content LIKE %:keyword%")
    List<News> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 출처별 뉴스 개수 조회
     * 
     * 생성되는 SQL:
     * SELECT source, COUNT(*) 
     * FROM news 
     * GROUP BY source
     */
    @Query("SELECT n.source, COUNT(n) FROM News n GROUP BY n.source")
    List<Object[]> countBySource();
    
    /**
     * 평균 조회수 조회
     * 
     * 생성되는 SQL:
     * SELECT AVG(view_count) FROM news
     */
    @Query("SELECT AVG(n.viewCount) FROM News n")
    Double getAverageViewCount();
    
    /**
     * 특정 기간 동안의 뉴스 조회 (고급 쿼리)
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * WHERE published_at BETWEEN ? AND ?
     * ORDER BY view_count DESC
     */
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate ORDER BY n.viewCount DESC")
    List<News> findNewsBetweenDates(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 감정 점수가 긍정적이고 고조회수인 뉴스 조회
     * 
     * 생성되는 SQL:
     * SELECT * FROM news 
     * WHERE sentiment_score > 0.5 AND is_high_view = true
     * ORDER BY published_at DESC
     * LIMIT ?
     */
    @Query("SELECT n FROM News n WHERE n.sentimentScore > 0.5 AND n.isHighView = true ORDER BY n.publishedAt DESC")
    List<News> findPositiveHighViewNews(Pageable pageable);
    
    
    // ========== 4. Native Query (실제 SQL 사용) ==========
    
    /**
     * Native SQL 쿼리 사용 (PostgreSQL 특정 기능 사용 가능)
     * 
     * 주의: DB에 종속적 (PostgreSQL → MySQL 변경 시 수정 필요)
     * 
     * 용도: 복잡한 집계 쿼리, 성능 최적화가 필요한 경우
     */
    @Query(value = "SELECT * FROM news WHERE sentiment_score > :minScore ORDER BY view_count DESC LIMIT :limit", 
           nativeQuery = true)
    List<News> findTopNewsByNativeQuery(
        @Param("minScore") Double minScore, 
        @Param("limit") Integer limit
    );
    
    
    // ========== 5. Exists 쿼리 (존재 여부 확인) ==========
    
    /**
     * URL 존재 여부 확인
     * 
     * 생성되는 SQL:
     * SELECT COUNT(*) > 0 FROM news WHERE url = ?
     * 
     * 용도: 중복 크롤링 방지
     */
    boolean existsByUrl(String url);
    
    /**
     * 특정 출처의 뉴스 존재 여부 확인
     */
    boolean existsBySource(String source);
    
    
    // ========== 6. Delete 쿼리 ==========
    
    /**
     * 출처별 뉴스 삭제
     * 
     * 생성되는 SQL:
     * DELETE FROM news WHERE source = ?
     * 
     * 주의: @Transactional 필수!
     */
    void deleteBySource(String source);
    
    /**
     * 특정 날짜 이전 뉴스 삭제
     * 
     * 생성되는 SQL:
     * DELETE FROM news WHERE published_at < ?
     * 
     * 용도: 오래된 뉴스 정리
     */
    void deleteByPublishedAtBefore(LocalDateTime publishedAt);
}
