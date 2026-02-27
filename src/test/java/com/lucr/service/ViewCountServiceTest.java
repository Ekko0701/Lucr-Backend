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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
 *   <li>recordView: SET NX 중복 방지 + INCR</li>
 *   <li>getViewCount: dbcount + Redis 증가분 합산</li>
 *   <li>syncViewCountsToDb: SCAN → ViewCountSyncHelper 위임 및 예외 처리</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ViewCountServiceImpl 테스트")
class ViewCountServiceTest {

    @InjectMocks
    private ViewCountServiceImpl viewCountService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ViewCountSyncHelper viewCountSyncHelper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Cursor<String> cursor;

    private UUID testNewsId;
    private String incrKey;    // "news:viewcount:{uuid}"
    private String dbcountKey; // "news:dbcount:{uuid}"
    private String dedupeKey;  // "news:viewed:{uuid}:user:testuser"

    @BeforeEach
    void setUp() {
        testNewsId = UUID.randomUUID();
        incrKey    = CacheConstants.VIEW_COUNT_PREFIX + testNewsId;
        dbcountKey = CacheConstants.VIEW_COUNT_DB_PREFIX + testNewsId;
        dedupeKey  = CacheConstants.VIEW_COUNT_VIEWED_PREFIX + testNewsId + ":user:testuser";
    }

    // ==================== recordView ====================

    @Nested
    @DisplayName("recordView — SET NX 중복 방지 + INCR")
    class RecordViewTests {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("새 조회 — SET NX 성공 시 INCR 호출")
        void recordView_NewViewer_IncrementsCounter() {
            // given: 해당 viewerKey 로 처음 조회 (SET NX 성공)
            given(valueOperations.setIfAbsent(eq(dedupeKey), eq(1),
                    eq(CacheConstants.VIEW_DEDUP_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                    .willReturn(Boolean.TRUE);

            // when
            viewCountService.recordView(testNewsId, "user:testuser");

            // then: INCR 호출됨
            then(valueOperations).should(times(1)).increment(incrKey);
        }

        @Test
        @DisplayName("중복 조회 — SET NX 실패 시 INCR 호출 안 됨")
        void recordView_DuplicateViewer_SkipsIncrement() {
            // given: 24시간 내 이미 조회한 viewerKey (SET NX 실패)
            given(valueOperations.setIfAbsent(eq(dedupeKey), eq(1),
                    eq(CacheConstants.VIEW_DEDUP_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                    .willReturn(Boolean.FALSE);

            // when
            viewCountService.recordView(testNewsId, "user:testuser");

            // then: INCR 호출 안 됨
            then(valueOperations).should(never()).increment(anyString());
        }

        @Test
        @DisplayName("SET NX null 반환(Redis 연결 실패) — INCR 호출 안 됨")
        void recordView_NullFromSetIfAbsent_SkipsIncrement() {
            // given: Redis 연결 이상으로 null 반환
            given(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                    .willReturn(null);

            // when
            viewCountService.recordView(testNewsId, "user:testuser");

            // then
            then(valueOperations).should(never()).increment(anyString());
        }
    }

    // ==================== getViewCount ====================

    @Nested
    @DisplayName("getViewCount — dbcount + Redis 증가분 합산")
    class GetViewCountTests {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("dbcount(100) + 증가분(50) = 150 반환")
        void getViewCount_ReturnsSumOfDbcountAndIncrement() {
            // given
            given(valueOperations.get(dbcountKey)).willReturn(100);
            given(valueOperations.get(incrKey)).willReturn(50);

            // when
            long result = viewCountService.getViewCount(testNewsId);

            // then
            assertThat(result).isEqualTo(150L);
        }

        @Test
        @DisplayName("dbcount 키 없으면 0으로 취급")
        void getViewCount_NoDbcountKey_TreatsAsZero() {
            // given: 동기화 직후 등 dbcount 키가 없는 상황
            given(valueOperations.get(dbcountKey)).willReturn(null);
            given(valueOperations.get(incrKey)).willReturn(30);

            // when
            long result = viewCountService.getViewCount(testNewsId);

            // then: 0 + 30 = 30
            assertThat(result).isEqualTo(30L);
        }

        @Test
        @DisplayName("증가분 키 없으면 dbcount만 반환")
        void getViewCount_NoIncrKey_ReturnsDbcountOnly() {
            // given: 동기화 직후 Redis 증가분이 없는 상황
            given(valueOperations.get(dbcountKey)).willReturn(100);
            given(valueOperations.get(incrKey)).willReturn(null);

            // when
            long result = viewCountService.getViewCount(testNewsId);

            // then: 100 + 0 = 100
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
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next()).willReturn(incrKey, key2);

            // when
            viewCountService.syncViewCountsToDb();

            // then: 각 키마다 syncSingleKey가 정확히 1회씩 호출됨
            then(viewCountSyncHelper).should(times(1)).syncSingleKey(incrKey);
            then(viewCountSyncHelper).should(times(1)).syncSingleKey(key2);
        }

        @Test
        @DisplayName("syncSingleKey에서 예외 발생해도 나머지 키 처리를 계속한다")
        void syncViewCountsToDb_ExceptionOnOneKey_ContinuesWithNextKey() {
            // given
            String key2 = CacheConstants.VIEW_COUNT_PREFIX + UUID.randomUUID();
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next()).willReturn(incrKey, key2);
            willThrow(new RuntimeException("DB 오류")).given(viewCountSyncHelper).syncSingleKey(incrKey);

            // when
            viewCountService.syncViewCountsToDb();

            // then: 첫 번째 키 실패와 무관하게 두 번째 키도 정상 처리됨
            then(viewCountSyncHelper).should(times(1)).syncSingleKey(key2);
        }

        @Test
        @DisplayName("Redis에 동기화할 키가 없으면 syncSingleKey를 호출하지 않는다")
        void syncViewCountsToDb_NoKeys_NeverCallsHelper() {
            // given
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(false);

            // when
            viewCountService.syncViewCountsToDb();

            // then
            then(viewCountSyncHelper).should(never()).syncSingleKey(any());
        }
    }
}
