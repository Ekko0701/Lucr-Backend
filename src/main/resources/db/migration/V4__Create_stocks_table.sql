-- ========================================
-- Stocks 테이블 생성
-- ========================================
-- 설명: 종목 정보
-- 의존성: 없음
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

CREATE TABLE stocks (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    market VARCHAR(20) NOT NULL CHECK (market IN ('KOSPI', 'KOSDAQ', 'NYSE', 'NASDAQ', 'AMEX')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 종목명 인덱스 (종목명 검색)
CREATE INDEX idx_stocks_name ON stocks(name);

-- 시장 인덱스 (시장별 필터링)
CREATE INDEX idx_stocks_market ON stocks(market);
