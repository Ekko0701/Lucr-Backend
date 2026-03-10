-- ======================================================
-- V9: 추천 시스템 테이블 생성
-- 선행: V8__add_analysis_tables.sql (keywords, news_keywords)
-- ======================================================

-- recommendations: 종목별 투자 추천 점수
CREATE TABLE recommendations (
    id UUID PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    score NUMERIC(4, 3) NOT NULL,           -- 0.000 ~ 1.000
    confidence NUMERIC(3, 2) NOT NULL,      -- 0.00 ~ 1.00 (신뢰도)
    reason TEXT,                            -- 추천 이유 (JSON 배열)
    related_news_count INTEGER NOT NULL DEFAULT 0,
    avg_sentiment NUMERIC(4, 3),            -- 평균 감정 점수 (정규화 후)
    total_mentions INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,                   -- 추천 유효 기간

    CONSTRAINT fk_recommendation_stock
        FOREIGN KEY (stock_code) REFERENCES stocks(code) ON DELETE CASCADE,

    CONSTRAINT uq_recommendation_stock
        UNIQUE (stock_code)
);

-- 인덱스
CREATE INDEX idx_recommendations_score ON recommendations(score DESC);
CREATE INDEX idx_recommendations_stock_code ON recommendations(stock_code);
CREATE INDEX idx_recommendations_expires_at ON recommendations(expires_at);