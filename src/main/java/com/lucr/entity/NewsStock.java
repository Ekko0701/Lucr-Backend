package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * NewsStock Entity — 뉴스와 종목의 다대다 관계를 나타내는 중간 엔티티
 *
 * <h3>왜 @ManyToMany 대신 중간 엔티티를 사용하는가?</h3>
 * <p>
 * {@code @ManyToMany}는 조인 테이블에 추가 컬럼을 넣을 수 없다.
 * 뉴스 내 종목 언급 횟수({@code mentionCount})를 저장해야 하므로
 * 중간 엔티티를 직접 정의한다.
 * </p>
 *
 * <h3>복합키 구조</h3>
 * <pre>
 * PK = (news_id, stock_code)
 *       ↑ News.id      ↑ Stock.code
 *
 * @MapsId로 복합키 필드와 FK를 하나로 통합
 * </pre>
 *
 * <h3>테이블 설계</h3>
 * <pre>
 * news_stocks
 * ├── news_id (PK, FK → news.id)
 * ├── stock_code (PK, FK → stocks.code)
 * ├── mention_count (DEFAULT 1)
 * └── created_at
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-12
 * @see NewsStockId
 */
@Entity
@Table(name = "news_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsStock {

    /**
     * 복합키 (news_id + stock_code)
     *
     * <p>{@code @EmbeddedId}: 이 필드가 복합 PK임을 JPA에 알린다.</p>
     */
    @EmbeddedId
    private NewsStockId id;

    /**
     * 관련 뉴스 (N:1)
     *
     * <ul>
     *   <li>{@code @MapsId("newsId")}: 복합키의 newsId 필드와 이 FK를 하나로 매핑</li>
     *   <li>{@code FetchType.LAZY}: 실제 접근 시에만 News를 DB에서 조회</li>
     * </ul>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("newsId")
    @JoinColumn(name = "news_id")
    private News news;

    /**
     * 관련 종목 (N:1)
     *
     * <ul>
     *   <li>{@code @MapsId("stockCode")}: 복합키의 stockCode 필드와 이 FK를 하나로 매핑</li>
     *   <li>{@code FetchType.LAZY}: 실제 접근 시에만 Stock을 DB에서 조회</li>
     * </ul>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stockCode")
    @JoinColumn(name = "stock_code")
    private Stock stock;

    /**
     * 뉴스 본문 내 종목 언급 횟수
     *
     * <p>예: "삼성전자"가 기사에서 3번 언급되었으면 mentionCount = 3</p>
     */
    @Column(name = "mention_count")
    @Builder.Default
    private Integer mentionCount = 1;

    /**
     * 생성일시 (자동 설정, 수정 불가)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==================== 비즈니스 메서드 ====================

    /**
     * 언급 횟수 증가
     */
    public void incrementMentionCount() {
        this.mentionCount++;
    }

    // ==================== 정적 팩토리 메서드 ====================

    /**
     * NewsStock 관계 생성 편의 메서드
     *
     * @param news  뉴스 엔티티
     * @param stock 종목 엔티티
     * @return 새 NewsStock 인스턴스
     */
    public static NewsStock create(News news, Stock stock) {
        return NewsStock.builder()
                .id(new NewsStockId(news.getId(), stock.getCode()))
                .news(news)
                .stock(stock)
                .build();
    }

    /**
     * NewsStock 관계 생성 편의 메서드 (언급 횟수 지정)
     *
     * @param news         뉴스 엔티티
     * @param stock        종목 엔티티
     * @param mentionCount 언급 횟수
     * @return 새 NewsStock 인스턴스
     */
    public static NewsStock create(News news, Stock stock, int mentionCount) {
        return NewsStock.builder()
                .id(new NewsStockId(news.getId(), stock.getCode()))
                .news(news)
                .stock(stock)
                .mentionCount(mentionCount)
                .build();
    }
}
