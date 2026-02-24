package com.lucr.service;

import com.lucr.config.CacheConstants;
import com.lucr.entity.News;
import com.lucr.repository.NewsRepository;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * ViewCountServiceImpl 단위 테스트
 *
 * <p>Redis INCR 기반 조회수 증가 및 DB 동기화 로직을 검증합니다.</p>
 *
 * <p>주요 테스트 시나리오:</p>
 * <ul>
 *   <li>조회수 증가 (INCR)</li>
 *   <li>조회수 조회 (DB + Redis 합산)</li>
 *   <li>DB 동기화 (SCAN 사용, 데이터 유실 방지)</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ViewCountService 테스트")
class ViewCountServiceTest {

    @InjectMocks
    private ViewCountServiceImpl viewCountService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Cursor<String> cursor;

    private UUID testNewsId;
    private String testKey;

    @BeforeEach
    void setUp() {
        testNewsId = UUID.randomUUID();
        testKey = CacheConstants.VIEW_COUNT_PREFIX + testNewsId;

        // RedisTemplate.opsForValue() Mock 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("조회수 증가")
    class IncrementViewCountTests {

        @Test
        @DisplayName("첫 번째 증가 시 1 반환 (Redis 초기화)")
        void incrementViewCount_FirstIncrement_Returns1() {
            // given
            given(valueOperations.increment(testKey)).willReturn(1L);

            // when
            long result = viewCountService.incrementViewCount(testNewsId);

            // then
            assertThat(result).isEqualTo(1L);  // 실제 증가 로직 검증
            then(valueOperations).should(times(1)).increment(testKey);
        }

        @Test
        @DisplayName("두 번째 증가 시 2 반환 (연속 증가)")
        void incrementViewCount_SecondIncrement_Returns2() {
            // given
            given(valueOperations.increment(testKey))
                .willReturn(1L)   // 첫 번째 호출
                .willReturn(2L);  // 두 번째 호출

            // when
            long firstResult = viewCountService.incrementViewCount(testNewsId);
            long secondResult = viewCountService.incrementViewCount(testNewsId);

            // then
            assertThat(firstResult).isEqualTo(1L);
            assertThat(secondResult).isEqualTo(2L);  // 증가 확인
            then(valueOperations).should(times(2)).increment(testKey);
        }

        @Test
        @DisplayName("여러 번 증가 시 순차적으로 카운트 증가")
        void incrementViewCount_MultipleIncrements_ReturnsSequentialCounts() {
            // given
            given(valueOperations.increment(testKey))
                .willReturn(1L, 2L, 3L, 4L, 5L);

            // when & then
            assertThat(viewCountService.incrementViewCount(testNewsId)).isEqualTo(1L);
            assertThat(viewCountService.incrementViewCount(testNewsId)).isEqualTo(2L);
            assertThat(viewCountService.incrementViewCount(testNewsId)).isEqualTo(3L);
            assertThat(viewCountService.incrementViewCount(testNewsId)).isEqualTo(4L);
            assertThat(viewCountService.incrementViewCount(testNewsId)).isEqualTo(5L);

            then(valueOperations).should(times(5)).increment(testKey);
        }

        @Test
        @DisplayName("Redis 연결 실패 시 0 반환")
        void incrementViewCount_RedisFailure_ReturnsZero() {
            // given
            given(valueOperations.increment(testKey)).willReturn(null);

            // when
            long result = viewCountService.incrementViewCount(testNewsId);

            // then
            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("조회수 조회")
    class GetViewCountTests {

        @Test
        @DisplayName("DB 조회수 + Redis 증가분 합산")
        void getViewCount_Success() {
            // given
            int dbViewCount = 100;
            given(valueOperations.get(testKey)).willReturn(50);

            // when
            long result = viewCountService.getViewCount(testNewsId, dbViewCount);

            // then
            assertThat(result).isEqualTo(150L);  // 100 + 50
        }

        @Test
        @DisplayName("Redis 키가 없으면 DB 조회수만 반환")
        void getViewCount_NoRedisKey_ReturnsDbCount() {
            // given
            int dbViewCount = 100;
            given(valueOperations.get(testKey)).willReturn(null);

            // when
            long result = viewCountService.getViewCount(testNewsId, dbViewCount);

            // then
            assertThat(result).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("DB 동기화 (SCAN 사용)")
    class SyncViewCountsToDbTests {

        private News testNews;

        @BeforeEach
        void setUp() {
            testNews = News.builder()
                .id(testNewsId)
                .viewCount(100)
                .build();
        }

        @Test
        @DisplayName("SCAN으로 키를 찾아 DB에 동기화 후 Redis 키 삭제")
        void syncViewCountsToDb_Success() {
            // given
            // SCAN 결과 Mock
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, false);  // 1개 키 처리 후 종료
            given(cursor.next()).willReturn(testKey);

            // Redis GET 결과 Mock
            given(valueOperations.get(testKey)).willReturn(50);

            // DB 조회 Mock
            given(newsRepository.findById(testNewsId)).willReturn(Optional.of(testNews));

            // when
            viewCountService.syncViewCountsToDb();

            // then
            // 1. SCAN 실행 확인
            then(redisTemplate).should(times(1)).scan(any(ScanOptions.class));

            // 2. Redis에서 값 조회 확인
            then(valueOperations).should(times(1)).get(testKey);

            // 3. DB 조회 확인
            then(newsRepository).should(times(1)).findById(testNewsId);

            // 4. DB 업데이트 확인 (Dirty Checking)
            assertThat(testNews.getViewCount()).isEqualTo(150);  // 100 + 50

            // 5. Redis 키 삭제 확인
            then(redisTemplate).should(times(1)).delete(testKey);
        }

        @Test
        @DisplayName("DB 업데이트 실패 시 Redis 키를 삭제하지 않음 (데이터 유실 방지)")
        void syncViewCountsToDb_DbUpdateFailed_RedisKeyPreserved() {
            // given
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, false);
            given(cursor.next()).willReturn(testKey);
            given(valueOperations.get(testKey)).willReturn(50);

            // DB 조회 실패 (뉴스가 삭제된 경우)
            given(newsRepository.findById(testNewsId)).willReturn(Optional.empty());

            // when
            viewCountService.syncViewCountsToDb();

            // then
            // Redis 키 삭제 호출되지 않음 (데이터 유실 방지)
            then(redisTemplate).should(never()).delete(testKey);
        }

        @Test
        @DisplayName("개별 키 처리 실패 시 다음 키 계속 처리")
        void syncViewCountsToDb_IndividualKeyFails_ContinuesProcessing() {
            // given
            UUID newsId2 = UUID.randomUUID();
            String invalidKey = CacheConstants.VIEW_COUNT_PREFIX + "invalid-uuid";
            String key2 = CacheConstants.VIEW_COUNT_PREFIX + newsId2;

            // 첫 번째 키는 예외 발생 (UUID 파싱 실패), 두 번째 키는 정상 처리
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next())
                .willReturn(invalidKey)  // 첫 번째: 잘못된 키
                .willReturn(key2);       // 두 번째: 정상 키

            // 모든 키에 대한 Redis GET
            given(valueOperations.get(invalidKey)).willReturn(50);
            given(valueOperations.get(key2)).willReturn(50);

            // 두 번째 키는 정상 처리
            News news2 = News.builder().id(newsId2).viewCount(200).build();
            given(newsRepository.findById(newsId2)).willReturn(Optional.of(news2));

            // when
            viewCountService.syncViewCountsToDb();

            // then
            // 두 번째 키는 정상 처리되어 DB 업데이트 및 Redis 삭제
            then(newsRepository).should(times(1)).findById(newsId2);
            then(redisTemplate).should(times(1)).delete(key2);
            
            // 첫 번째 키는 Redis 삭제 호출되지 않음 (처리 실패)
            then(redisTemplate).should(never()).delete(invalidKey);
        }

        @Test
        @DisplayName("Redis 값이 0 이하면 스킵")
        void syncViewCountsToDb_ZeroOrNegativeValue_Skipped() {
            // given
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, false);
            given(cursor.next()).willReturn(testKey);
            given(valueOperations.get(testKey)).willReturn(0);  // 0 값

            // when
            viewCountService.syncViewCountsToDb();

            // then
            // DB 조회 호출되지 않음 (스킵)
            then(newsRepository).should(never()).findById(any());
            // Redis 키 삭제 호출되지 않음
            then(redisTemplate).should(never()).delete(testKey);
        }

        @Test
        @DisplayName("Redis 값이 null이면 스킵")
        void syncViewCountsToDb_NullValue_Skipped() {
            // given
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(true, false);
            given(cursor.next()).willReturn(testKey);
            given(valueOperations.get(testKey)).willReturn(null);

            // when
            viewCountService.syncViewCountsToDb();

            // then
            then(newsRepository).should(never()).findById(any());
            then(redisTemplate).should(never()).delete(testKey);
        }
    }
}
