package com.lucr.service;

import com.lucr.config.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 조회수 관리 서비스 구현체 — Redis INCR 기반
 *
 * <p>중복 방지(SET NX) + 조회수 증가(INCR) + DB 동기화(스케줄러) 통합</p>
 *
 * @author Ekko0701
 * @since 2026-02-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountServiceImpl implements ViewCountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ViewCountSyncHelper viewCountSyncHelper;

    /**
     * 중복 방지 후 조회수 증가
     *
     * <p>SET NX(원자적)로 중복 여부를 판별하고, 새 조회면 INCR로 증가시킨다.</p>
     */
    @Override
    public void recordView(UUID newsId, String viewerKey) {
        String dedupeKey = CacheConstants.VIEW_COUNT_VIEWED_PREFIX + newsId + ":" + viewerKey;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(dedupeKey, 1, CacheConstants.VIEW_DEDUP_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(isNew)) {
            redisTemplate.opsForValue().increment(CacheConstants.VIEW_COUNT_PREFIX + newsId);
            log.debug("조회수 증가: newsId={}, viewerKey={}", newsId, viewerKey);
        } else {
            log.debug("중복 조회 무시: newsId={}, viewerKey={}", newsId, viewerKey);
        }
    }

    /**
     * 현재 조회수 조회 (dbcount + Redis 증가분 합산)
     */
    @Override
    public long getViewCount(UUID newsId) {
        Object dbCountObj = redisTemplate.opsForValue().get(CacheConstants.VIEW_COUNT_DB_PREFIX + newsId);
        long dbCount = dbCountObj != null ? ((Number) dbCountObj).longValue() : 0;

        Object incrObj = redisTemplate.opsForValue().get(CacheConstants.VIEW_COUNT_PREFIX + newsId);
        long increment = incrObj != null ? ((Number) incrObj).longValue() : 0;

        return dbCount + increment;
    }

    /**
     * Redis 조회수를 DB에 동기화 (5분마다 실행)
     *
     * <p>동작 흐름:</p>
     * <ol>
     *   <li>SCAN으로 {@code news:viewcount:*} 패턴의 키를 논블로킹 방식으로 조회</li>
     *   <li>각 키를 {@link ViewCountSyncHelper}에 위임 (독립 트랜잭션)</li>
     * </ol>
     */
    @Override
    @Scheduled(fixedRate = 300_000)
    public void syncViewCountsToDb() {
        log.info("조회수 DB 동기화 시작");

        int syncCount = 0;
        int failCount = 0;

        ScanOptions options = ScanOptions.scanOptions()
                .match(CacheConstants.VIEW_COUNT_PREFIX + "*")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    viewCountSyncHelper.syncSingleKey(key);
                    syncCount++;
                } catch (Exception e) {
                    log.error("조회수 동기화 실패 (Redis 키 보존): key={}", key, e);
                    failCount++;
                }
            }
        } catch (Exception e) {
            log.error("SCAN 실행 실패", e);
        }

        log.info("조회수 DB 동기화 완료: 성공 {}건, 실패 {}건", syncCount, failCount);
    }
}
