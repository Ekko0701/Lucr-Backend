package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * NewsKeyword Entity — 뉴스와 키워드의 다대다 관계 중간 엔티티
 *
 * <p>
 * 단순 @ManyToMany로는 tfidf_score 같은 추가 컬럼을 저장할 수 없어
 * NewsStock과 동일하게 중간 엔티티를 직접 정의합니다.
 * </p>
 *
 * @author Ekko0701
 * @since 2026-03-08
 */
@Entity
@Table(name = "news_keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsKeyword {

    /**
     * 복합키 (news_id + keyword_id)
     */
    @EmbeddedId
    private NewsKeywordId id;

    /**
     * 관련 뉴스 (N:1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("newsId")
    @JoinColumn(name = "news_id")
    private News news;

    /**
     * 관련 키워드 (N:1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("keywordId")
    @JoinColumn(name = "keyword_id")
    private Keyword keyword;

    /**
     * TF-IDF 점수 (0.10 ~ 1.00 권장)
     */
    @Column(name = "tfidf_score", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal tfidfScore = BigDecimal.ONE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * NewsKeyword 관계 생성 편의 메서드
     */
    public static NewsKeyword create(News news, Keyword keyword, BigDecimal score) {
        return NewsKeyword.builder()
                .id(new NewsKeywordId(news.getId(), keyword.getId()))
                .news(news)
                .keyword(keyword)
                .tfidfScore(score)
                .build();
    }
}
