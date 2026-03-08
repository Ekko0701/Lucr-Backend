-- ========================================
-- Analysis 테이블 생성
-- ========================================
-- 설명: 키워드 및 뉴스-키워드 다대다 관계 저장
-- 의존성: news 테이블 (V3)
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-08
-- ========================================

-- 키워드 사전 테이블
CREATE TABLE keywords (
    id UUID PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    frequency INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 같은 키워드 중복 방지
ALTER TABLE keywords
    ADD CONSTRAINT uk_keyword_word UNIQUE (word);

-- 키워드 검색/정렬 성능
CREATE INDEX idx_keyword_word ON keywords(word);
CREATE INDEX idx_keyword_frequency ON keywords(frequency DESC);


-- 뉴스-키워드 중간 테이블
CREATE TABLE news_keywords (
    news_id UUID NOT NULL,
    keyword_id UUID NOT NULL,
    tfidf_score NUMERIC(4, 2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (news_id, keyword_id)
);

-- 외래키 제약조건
ALTER TABLE news_keywords
    ADD CONSTRAINT fk_news_keywords_news
    FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE;

ALTER TABLE news_keywords
    ADD CONSTRAINT fk_news_keywords_keyword
    FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE;

-- 조인/필터링 성능
CREATE INDEX idx_news_keywords_news_id ON news_keywords(news_id);
CREATE INDEX idx_news_keywords_keyword_id ON news_keywords(keyword_id);
