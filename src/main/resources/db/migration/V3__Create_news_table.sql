-- ========================================
-- News 테이블 생성
-- ========================================
-- 설명: 뉴스 기사 정보
-- 의존성: 없음
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

CREATE TABLE news (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    source VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    published_at TIMESTAMP,
    view_count INTEGER NOT NULL DEFAULT 0,
    crawled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sentiment_score NUMERIC(3, 2),
    is_high_view BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- URL 유니크 제약조건 (중복 뉴스 방지)
ALTER TABLE news ADD CONSTRAINT uk_news_url UNIQUE (url);

-- 발행일 인덱스 (최신 뉴스 정렬)
CREATE INDEX idx_news_published_at ON news(published_at DESC);

-- 언론사 인덱스 (언론사별 필터링)
CREATE INDEX idx_news_source ON news(source);

-- 제목 검색 인덱스 (LIKE 검색 성능 향상)
CREATE INDEX idx_news_title ON news(title);

-- 조회수 인덱스 (인기 뉴스 정렬)
CREATE INDEX idx_news_view_count ON news(view_count DESC);

-- 감정 점수 인덱스 (감정별 필터링)
CREATE INDEX idx_news_sentiment ON news(sentiment_score);

-- Full-text search 인덱스 (PostgreSQL - 언어 무관 'simple' 사용)
-- 'korean' dictionary는 PostgreSQL에 기본 내장되지 않음
CREATE INDEX idx_news_title_content_fts ON news 
    USING gin(to_tsvector('simple', title || ' ' || COALESCE(content, '')));
