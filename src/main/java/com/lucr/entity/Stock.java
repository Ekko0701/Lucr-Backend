package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock Entity — 주식 종목 정보
 *
 * <h3>PK 설계: 자연키(Natural Key) 사용</h3>
 * <p>
 * News, User와 달리 UUID 대리키가 아닌 종목코드({@code code})를 PK로 사용한다.
 * 종목코드는 거래소에서 고유성이 보장되며, 변경되지 않고,
 * 외부 시스템(KRX, 증권사 API)과 직접 연동 가능하다는 이점이 있다.
 * </p>
 *
 * <h3>테이블 설계</h3>
 * <pre>
 * stocks
 * ├── code (PK, VARCHAR 20)  — 종목코드 ("005930", "AAPL")
 * ├── name (NOT NULL)        — 종목명 ("삼성전자", "Apple Inc.")
 * ├── market (NOT NULL)      — 시장 구분 (KOSPI, KOSDAQ, NYSE, NASDAQ, AMEX)
 * ├── created_at             — 생성일시
 * └── updated_at             — 수정일시
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Entity
@Table(name = "stocks", indexes = {
        @Index(name = "idx_stock_name", columnList = "name"),
        @Index(name = "idx_stock_market", columnList = "market")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    /**
     * 종목코드 (PK, 직접 할당)
     *
     * <p>한국: "005930" (삼성전자), "035720" (카카오)</p>
     * <p>미국: "AAPL" (Apple), "GOOGL" (Alphabet)</p>
     *
     * <p>{@code @GeneratedValue} 없음 — 종목코드는 외부에서 부여되므로 직접 할당한다.</p>
     */
    @Id
    @Column(name = "code", length = 20)
    private String code;

    /**
     * 종목명
     *
     * <p>예: "삼성전자", "Apple Inc."</p>
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 시장 구분
     *
     * <p>{@code @Enumerated(EnumType.STRING)}: DB에 문자열로 저장 ("KOSPI", "NASDAQ" 등)</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "market", nullable = false, length = 20)
    private Market market;

    /**
     * 생성일시 (자동 설정, 수정 불가)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시 (자동 갱신)
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 이 종목과 관련된 뉴스 목록 (NewsStock 중간 테이블)
     *
     * <ul>
     *   <li>{@code mappedBy = "stock"}: NewsStock.stock 필드가 관계의 주인</li>
     *   <li>{@code cascade = ALL}: Stock 삭제 시 관련 NewsStock도 함께 삭제</li>
     *   <li>{@code orphanRemoval = true}: 컬렉션에서 제거 시 DB에서도 삭제</li>
     * </ul>
     */
    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NewsStock> newsStocks = new ArrayList<>();
}
