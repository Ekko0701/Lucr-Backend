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
 * NewsKeyword 복합키 클래스
 *
 * <p>news_keywords 테이블의 PK는 {@code (news_id, keyword_id)} 조합입니다.</p>
 *
 * @author Ekko0701
 * @since 2026-03-08
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NewsKeywordId implements Serializable {

    @Column(name = "news_id")
    private UUID newsId;

    @Column(name = "keyword_id")
    private UUID keywordId;
}
