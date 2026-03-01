-- ========================================
-- Crawl Jobs 테이블 생성
-- ========================================
-- 설명: 크롤링 작업 관리
-- 의존성: 없음
-- 작성자: Kim Dongjoo
-- 작성일: 2026-03-01
-- ========================================

CREATE TABLE crawl_jobs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 상태 인덱스 (상태별 필터링)
CREATE INDEX idx_crawl_jobs_status ON crawl_jobs(status);

-- 시작일 인덱스 (최근 작업 조회)
CREATE INDEX idx_crawl_jobs_started_at ON crawl_jobs(started_at DESC);

-- 생성일 인덱스 (전체 작업 이력)
CREATE INDEX idx_crawl_jobs_created_at ON crawl_jobs(created_at DESC);
