package com.lucr.service;

import java.util.UUID;

/**
 * 조회수 관리 서비스 — Redis 기반
 *
 * <p>조회수 증가는 Redis INCR로 처리하고,
 * 주기적으로 DB에 동기화한다.</p>
 */
public interface ViewCountService {
    /**
     * 조회수 증가 (Redis INCR)
     *
     * @param newsId 뉴스 ID
     * @return 증가 후 조회수
     */
    long incrementViewCount(UUID newsId);

    /**
     * 현재 조회수 조회 (Redis + DB 합산)
     *
     * @param newsId 뉴스 ID
     * @param dbViewCount DB에 저장된 조회수
     * @return 현재 총 조회수
     */
    long getViewCount(UUID newsId, int dbViewCount);

    /**
     * Redis에 쌓인 조회수를 DB에 동기화
     *
     * <p>스케줄러에서 주기적으로 호출한다. (예: 5분마다)</p>
     */
    void syncViewCountsToDb();
}
