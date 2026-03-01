-- ========================================
-- Users 테이블 생성
-- ========================================
-- 설명: 사용자 정보 관리
-- 의존성: 없음
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 이메일 유니크 제약조건
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- 이메일 인덱스 (로그인 성능 향상)
CREATE INDEX idx_users_email ON users(email);

-- 역할 인덱스 (관리자 조회 성능 향상)
CREATE INDEX idx_users_role ON users(role);

-- 생성일 인덱스 (최근 가입자 조회)
CREATE INDEX idx_users_created_at ON users(created_at DESC);
