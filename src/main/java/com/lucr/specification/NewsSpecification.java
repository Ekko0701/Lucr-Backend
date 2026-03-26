package com.lucr.specification;

import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.entity.News;
import com.lucr.entity.NewsStock;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 검색 동적 쿼리 Specification
 *
 * NewsSearchRequest의 null이 아닌 필터만 WHERE 조건에 포함합니다.
 *
 * 생성 예시:
 *   keyword="삼성", minSentimentScore=0.3, stockCode="005930"
 *   →
 *   WHERE (title LIKE '%삼성%' OR content LIKE '%삼성%')
 *     AND sentiment_score >= 0.3
 *     AND id IN (SELECT news_id FROM news_stocks WHERE stock_code = '005930')
 *
 * @author Ekko0701
 * @since 2026-03-17
 */
public class NewsSpecification {

    private NewsSpecification() {
        // 유틸리티 클래스 — 인스턴스화 방지
    }

    /**
     * NewsSearchRequest → Specification 변환
     *
     * @param request 검색 조건 DTO
     * @return 동적 Specification (null이 아닌 조건만 AND 결합)
     */
    public static Specification<News> fromSearchRequest(NewsSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 키워드 검색 (제목 OR 본문 LIKE)
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("content"), pattern)
                ));
            }

            // 2. 출처 필터
            if (request.getSource() != null && !request.getSource().isBlank()) {
                predicates.add(cb.equal(root.get("source"), request.getSource()));
            }

            // 3. 최소 조회수
            if (request.getMinViewCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("viewCount"), request.getMinViewCount()));
            }

            // 4. 감정 점수 하한
            if (request.getMinSentimentScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("sentimentScore"), request.getMinSentimentScore()));
            }

            // 5. 감정 점수 상한
            if (request.getMaxSentimentScore() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("sentimentScore"), request.getMaxSentimentScore()));
            }

            // 6. 발행일 시작
            if (request.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("publishedAt"), request.getStartDate()));
            }

            // 7. 발행일 종료
            if (request.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("publishedAt"), request.getEndDate()));
            }

            // 8. 인기 뉴스 여부
            if (request.getIsHighView() != null) {
                predicates.add(cb.equal(root.get("isHighView"), request.getIsHighView()));
            }

            // 9. 종목 코드 필터 (NewsStock JOIN)
            if (request.getStockCode() != null && !request.getStockCode().isBlank()) {
                Join<News, NewsStock> newsStockJoin = root.join("newsStocks", JoinType.INNER);
                predicates.add(cb.equal(
                        newsStockJoin.get("stock").get("code"),
                        request.getStockCode()));
            }

            // SELECT DISTINCT 설정 — query 객체의 설정을 변경하므로 return에 포함하지 않음
            // JOIN 시 같은 뉴스가 중복 반환되는 것을 방지 (데이터 조회 + COUNT 쿼리 모두 적용)
            query.distinct(true);

            // WHERE 조건만 반환 (null이 아닌 필터들을 AND로 결합)
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
