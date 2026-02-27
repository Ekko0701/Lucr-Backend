package com.lucr.service;

import java.util.UUID;

/**
 * 조회수 관리 서비스 — Redis 기반
 *
 * <p>중복 방지(dedup) + 조회수 증가 통합, DB 동기화 기준값 분리 관리</p>
 */
public interface ViewCountService {

    /**
     * 중복 방지 후 조회수 증가 (SET NX + INCR)
     *
     * <p>viewerKey 기준 24시간 내 중복 조회는 무시한다.</p>
     *
     * @param newsId    뉴스 ID
     * @param viewerKey 로그인 시 "user:{userId}", 비로그인 시 "ip:{IP}"
     */
    void recordView(UUID newsId, String viewerKey);

    /**
     * 현재 조회수 조회 (dbcount + Redis 증가분 합산)
     *
     * @param newsId 뉴스 ID
     * @return 현재 총 조회수
     */
    long getViewCount(UUID newsId);

    /**
     * Redis에 쌓인 조회수를 DB에 동기화
     *
     * <p>스케줄러에서 주기적으로 호출한다. (예: 5분마다)</p>
     */
    void syncViewCountsToDb();
}
