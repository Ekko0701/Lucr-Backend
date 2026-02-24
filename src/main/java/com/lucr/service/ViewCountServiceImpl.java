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

/**
 * 조회수 관리 서비스 구현체 — Redis INCR 기반
 *
 * <p>조회수 증가는 Redis INCR로 처리하여 DB 부하를 줄이고,
 * 주기적으로 DB에 동기화하여 데이터 영속성을 보장한다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountServiceImpl implements ViewCountService {

    // Redis 연산을 위한 템플릿 (String 키, Object 값)
    private final RedisTemplate<String, Object> redisTemplate;

    // 키 단위 트랜잭션 처리 위임 (위임 패턴)
    private final ViewCountSyncHelper viewCountSyncHelper;

    /**
     * 조회수 증가 (Redis INCR)
     *
     * <p>Redis의 INCR 명령은 원자적(atomic) 연산이므로
     * 동시성 문제 없이 조회수를 증가시킬 수 있다.</p>
     *
     * @param newsId 뉴스 ID
     * @return 증가 후 조회수 (Redis에 저장된 증가분)
     */
    @Override
    public long incrementViewCount(UUID newsId) {
        // Redis 키 생성: "news:viewcount:{uuid}"
        String key = CacheConstants.VIEW_COUNT_PREFIX + newsId;

        // Redis INCR 명령 실행
        // - 키가 없으면 0으로 초기화 후 1 증가
        // - 키가 있으면 현재 값에 1 증가
        // - 원자적 연산으로 동시성 안전 보장
        Long count = redisTemplate.opsForValue().increment(key);
        
        log.debug("조회수 증가: newsId={}, redisCount={}", newsId, count);

        // null 체크 (Redis 연결 실패 등의 경우 대비)
        return count != null ? count : 0;
    }

    /**
     * 현재 조회수 조회 (Redis + DB 합산)
     *
     * <p>실제 총 조회수 = DB에 저장된 조회수 + Redis 증가분</p>
     *
     * @param newsId 뉴스 ID
     * @param dbViewCount DB에 저장된 조회수 (기준값)
     * @return 현재 총 조회수
     */
    @Override
    public long getViewCount(UUID newsId, int dbViewCount) {
        // Redis 키 생성
        String key = CacheConstants.VIEW_COUNT_PREFIX + newsId;

        // Redis에서 증가분 조회
        // - GET 명령으로 현재 저장된 값 가져오기
        // - 키가 없으면 null 반환
        Object redisCount = redisTemplate.opsForValue().get(key);
        
        // Object를 Long으로 변환
        // - Redis는 값을 Object로 반환하므로 타입 캐스팅 필요
        // - Number 타입으로 먼저 캐스팅 후 longValue() 호출
        long redisIncrement = redisCount != null ? ((Number) redisCount).longValue() : 0;

        // DB 조회수 + Redis 증가분 = 총 조회수
        return dbViewCount + redisIncrement;
    }

    /**
     * Redis 조회수를 DB에 동기화 (5분마다 실행)
     *
     * <p>동작 흐름 (데이터 유실 방지):</p>
     * <ol>
     *   <li>SCAN으로 {@code news:viewcount:*} 패턴의 키를 논블로킹 방식으로 조회</li>
     *   <li>각 키의 값(증가분)을 DB의 viewCount에 더함</li>
     *   <li>{@code saveAndFlush()}로 즉시 DB 반영 확인</li>
     *   <li>DB 반영 성공 후에만 Redis 키 삭제 (데이터 유실 방지)</li>
     *   <li>삭제된 뉴스의 Redis 키도 정리하여 메모리 누수 방지</li>
     * </ol>
     *
     * <p>개선 사항:</p>
     * <ul>
     *   <li>KEYS → SCAN: Redis 블로킹 방지 (프로덕션 안전)</li>
     *   <li>단일 트랜잭션 → 키별 독립 트랜잭션: DB 커밋 실패 시 다른 키 유실 방지</li>
     *   <li>위임 패턴: 트랜잭션 처리는 {@link ViewCountSyncHelper}에 위임</li>
     * </ul>
     *
     * <p>왜 5분마다?</p>
     * <ul>
     *   <li>너무 자주: DB 부하 증가, Redis INCR의 장점 상쇄</li>
     *   <li>너무 늦게: Redis 메모리 누적, 장애 시 데이터 손실 위험</li>
     *   <li>5분: 균형잡힌 주기 (트래픽 패턴에 따라 조정 가능)</li>
     * </ul>
     */
    @Override
    @Scheduled(fixedRate = 300_000)  // 5분 = 300,000 밀리초
    public void syncViewCountsToDb() {
        log.info("조회수 DB 동기화 시작");

        int syncCount = 0;
        int failCount = 0;

        // 1. SCAN 옵션 설정 (논블로킹 방식)
        ScanOptions options = ScanOptions.scanOptions()
            .match(CacheConstants.VIEW_COUNT_PREFIX + "*")  // 패턴 매칭
            .count(100)  // 한 번에 100개씩 스캔 (배치 크기)
            .build();

        // 2. SCAN 실행 (try-with-resources로 리소스 자동 정리)
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            
            // 3. 커서를 순회하며 키별로 독립 트랜잭션 처리 (위임 패턴)
            while (cursor.hasNext()) {
                String key = cursor.next();

                try {
                    // 키 하나의 DB 업데이트 + Redis 삭제를 ViewCountSyncHelper에 위임
                    // - 각 키가 독립된 트랜잭션으로 처리됨
                    // - DB 커밋 완료 후 Redis 삭제 → 데이터 유실 방지
                    // - 예외 발생 시 해당 키의 Redis는 보존 → 다음 동기화에서 재시도
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
