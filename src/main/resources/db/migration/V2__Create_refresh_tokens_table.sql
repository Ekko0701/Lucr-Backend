-- ========================================
-- Refresh Tokens 테이블 생성
-- ========================================
-- 설명: JWT Refresh Token 관리
-- 의존성: users 테이블
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 외래키 제약조건
ALTER TABLE refresh_tokens 
    ADD CONSTRAINT fk_refresh_tokens_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 토큰 유니크 제약조건
ALTER TABLE refresh_tokens ADD CONSTRAINT uk_refresh_tokens_token UNIQUE (token);

-- 사용자 ID 인덱스 (사용자별 토큰 조회)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 만료일 인덱스 (만료된 토큰 정리)
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
