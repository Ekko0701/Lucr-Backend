-- ========================================
-- News-Stocks 중간 테이블 생성
-- ========================================
-- 설명: 뉴스-종목 다대다 관계
-- 의존성: news, stocks 테이블
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

CREATE TABLE news_stocks (
    news_id UUID NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    mention_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (news_id, stock_code)
);

-- 외래키 제약조건
ALTER TABLE news_stocks 
    ADD CONSTRAINT fk_news_stocks_news 
    FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE;

ALTER TABLE news_stocks 
    ADD CONSTRAINT fk_news_stocks_stock 
    FOREIGN KEY (stock_code) REFERENCES stocks(code) ON DELETE CASCADE;

-- 뉴스 ID 인덱스 (뉴스의 관련 종목 조회)
CREATE INDEX idx_news_stocks_news_id ON news_stocks(news_id);

-- 종목 코드 인덱스 (종목의 관련 뉴스 조회)
CREATE INDEX idx_news_stocks_stock_code ON news_stocks(stock_code);
