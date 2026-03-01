-- ========================================
-- 추가 성능 최적화 인덱스
-- ========================================
-- 설명: 복합 인덱스 및 성능 최적화
-- 의존성: 모든 테이블
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

-- 뉴스: 발행일 + 조회수 복합 인덱스 (인기 뉴스 + 최신순)
CREATE INDEX idx_news_published_at_view_count 
    ON news(published_at DESC, view_count DESC);

-- 뉴스: 언론사 + 발행일 복합 인덱스 (언론사별 최신 뉴스)
CREATE INDEX idx_news_source_published_at 
    ON news(source, published_at DESC);

-- Refresh Token: 사용자 + 만료일 복합 인덱스 (유효한 토큰 조회)
CREATE INDEX idx_refresh_tokens_user_expires 
    ON refresh_tokens(user_id, expires_at);
