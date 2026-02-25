package com.lucr.service;

import com.lucr.config.CacheConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * ViewCountServiceImpl 단위 테스트
 *
 * <p>테스트 범위:</p>
 * <ul>
 *   <li>incrementViewCount: Redis INCR 호출 및 반환값 처리</li>
 *   <li>getViewCount: DB + Redis 합산 조회</li>
 *   <li>syncViewCountsToDb: SCAN → ViewCountSyncHelper 위임 및 예외 처리</li>
 * </ul>
 *
 * <p>syncViewCountsToDb의 키 단위 상세 로직(DB 업데이트, Redis 삭제)은
 * ViewCountSyncHelper의 책임이므로 ViewCountSyncHelperTest에서 검증한다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ViewCountServiceImpl 테스트")
class ViewCountServiceTest {

    // 테스트 대상 (실제 구현체)
    @InjectMocks
    private ViewCountServiceImpl viewCountService;

    // Redis 연산 Mock — 실제 Redis 연결 없이 동작 검증
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    // 위임 대상 Mock — syncSingleKey() 호출 여부만 검증, 내부 로직은 ViewCountSyncHelperTest에서 검증
    @Mock
    private ViewCountSyncHelper viewCountSyncHelper;

    // opsForValue()가 반환하는 Redis 값 연산 객체 Mock
    @Mock
    private ValueOperations<String, Object> valueOperations;

    // SCAN 커서 Mock — hasNext()/next() 시뮬레이션용
    @Mock
    private Cursor<String> cursor;

    private UUID testNewsId;
    private String testKey;  // "news:viewcount:{uuid}" 형태의 Redis 키

    @BeforeEach
    void setUp() {
        testNewsId = UUID.randomUUID();
        testKey = CacheConstants.VIEW_COUNT_PREFIX + testNewsId;
    }

    // ==================== incrementViewCount ====================

    @Nested
    @DisplayName("incrementViewCount — Redis INCR")
    class IncrementViewCountTests {

        @BeforeEach
        void setUp() {
            // opsForValue()는 이 Nested 클래스의 테스트에서만 사용
            // 최상위 setUp()에 두면 syncViewCountsToDb 테스트에서 UnnecessaryStubbingException 발생
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("Redis INCR 후 반환된 누적값을 그대로 반환한다")
        void incrementViewCount_ReturnsRedisValue() {
            // given: Redis가 현재 누적값 5를 반환하는 상황
            given(valueOperations.increment(testKey)).willReturn(5L);

            // when
            long result = viewCountService.incrementViewCount(testNewsId);

            // then: Redis 반환값을 그대로 반환해야 함
            assertThat(result).isEqualTo(5L);
            // Redis INCR이 정확히 1회 호출됐는지 확인
            then(valueOperations).should(times(1)).increment(testKey);
        }

        @Test
        @DisplayName("Redis가 null을 반환하면 (연결 실패 등) 0을 반환한다")
        void incrementViewCount_NullFromRedis_ReturnsZero() {
            // given: Redis 연결 실패 등으로 null 반환되는 상황
            given(valueOperations.increment(testKey)).willReturn(null);

            // when
            long result = viewCountService.incrementViewCount(testNewsId);

            // then: null 대신 0을 반환해 NPE 방지
            assertThat(result).isZero();
        }
    }

    // ==================== getViewCount ====================

    @Nested
    @DisplayName("getViewCount — DB + Redis 합산")
    class GetViewCountTests {

        @BeforeEach
        void setUp() {
            // opsForValue()는 이 Nested 클래스의 테스트에서만 사용
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("DB 조회수와 Redis 증가분을 합산하여 반환한다")
        void getViewCount_ReturnsSumOfDbAndRedis() {
            // given: Redis에 아직 DB에 반영되지 않은 증가분 50이 있는 상황
            given(valueOperations.get(testKey)).willReturn(50);

            // when: DB 조회수는 100으로 전달
            long result = viewCountService.getViewCount(testNewsId, 100);

            // then: DB 100 + Redis 50 = 150 반환
            assertThat(result).isEqualTo(150L);
        }

        @Test
        @DisplayName("Redis 키가 없으면 DB 조회수만 반환한다")
        void getViewCount_NoRedisKey_ReturnsDbCountOnly() {
            // given: 동기화 직후 등 Redis에 해당 키가 없는 상황
            given(valueOperations.get(testKey)).willReturn(null);

            // when
            long result = viewCountService.getViewCount(testNewsId, 100);

            // then: Redis 증가분 없으므로 DB 조회수 100만 반환
            assertThat(result).isEqualTo(100L);
        }
    }

    // ==================== syncViewCountsToDb ====================

    @Nested
    @DisplayName("syncViewCountsToDb — SCAN 순회 및 위임")
    class SyncViewCountsToDbTests {

        @Test
        @DisplayName("SCAN으로 찾은 각 키를 ViewCountSyncHelper에 위임한다")
        void syncViewCountsToDb_DelegatesToHelper() {
            // given: Redis에 2개의 viewcount 키가 있는 상황
            String key2 = CacheConstants.VIEW_COUNT_PREFIX + UUID.randomUUID();
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            // cursor.hasNext(): true → true → false (키 2개 순회 후 종료)
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next()).willReturn(testKey, key2);

            // when
            viewCountService.syncViewCountsToDb();

            // then: 각 키마다 syncSingleKey가 정확히 1회씩 호출됐는지 확인
            // 실제 DB/Redis 처리는 ViewCountSyncHelper의 책임이므로 여기서는 위임 여부만 검증
            then(viewCountSyncHelper).should(times(1)).syncSingleKey(testKey);
            then(viewCountSyncHelper).should(times(1)).syncSingleKey(key2);
        }

        @Test
        @DisplayName("syncSingleKey에서 예외 발생해도 나머지 키 처리를 계속한다")
        void syncViewCountsToDb_ExceptionOnOneKey_ContinuesWithNextKey() {
            // given: Redis에 2개의 키가 있고, 첫 번째 키 처리 중 예외 발생하는 상황
            String key2 = CacheConstants.VIEW_COUNT_PREFIX + UUID.randomUUID();
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next()).willReturn(testKey, key2);

            // 첫 번째 키 처리 시 DB 장애 등으로 예외 발생
            // → 해당 키의 Redis 데이터는 ViewCountSyncHelper 내부에서 보존됨
            willThrow(new RuntimeException("DB 오류")).given(viewCountSyncHelper).syncSingleKey(testKey);

            // when
            viewCountService.syncViewCountsToDb();

            // then: 첫 번째 키 실패와 무관하게 두 번째 키도 정상 처리됨
            // 한 키의 실패가 전체 동기화를 멈추지 않아야 함
            then(viewCountSyncHelper).should(times(1)).syncSingleKey(key2);
        }

        @Test
        @DisplayName("Redis에 동기화할 키가 없으면 syncSingleKey를 호출하지 않는다")
        void syncViewCountsToDb_NoKeys_NeverCallsHelper() {
            // given: 직전 동기화 후 아직 조회수가 쌓이지 않아 Redis에 키가 없는 상황
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(false);  // 즉시 순회 종료

            // when
            viewCountService.syncViewCountsToDb();

            // then: 처리할 키가 없으므로 syncSingleKey 미호출
            then(viewCountSyncHelper).should(never()).syncSingleKey(any());
        }
    }
}
