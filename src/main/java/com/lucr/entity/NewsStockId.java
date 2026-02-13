package com.lucr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * NewsStock 복합키 클래스
 *
 * <p>news_stocks 테이블의 PK는 {@code (news_id, stock_code)} 두 컬럼의 조합이다.
 * JPA에서 복합키를 사용하려면 별도의 {@code @Embeddable} 클래스로 정의해야 한다.</p>
 *
 * <h3>필수 요구사항</h3>
 * <ul>
 *   <li>{@code Serializable} 구현 — JPA가 복합키를 Map의 키처럼 사용하므로 직렬화 필요</li>
 *   <li>{@code equals()} / {@code hashCode()} 오버라이드 — 동일한 (news_id, stock_code) 조합을 같은 키로 인식</li>
 *   <li>기본 생성자 — JPA 스펙 요구사항</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NewsStockId implements Serializable {

    @Column(name = "news_id")
    private UUID newsId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;
}
