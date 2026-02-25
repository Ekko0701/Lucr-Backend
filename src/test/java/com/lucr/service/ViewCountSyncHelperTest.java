package com.lucr.service;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static com.lucr.config.CacheConstants.VIEW_COUNT_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * ViewCountSyncHelper 단위 테스트
 *
 * <p>syncViewCountsToDb()에서 위임받은 키 단위 처리 로직을 검증한다.</p>
 *
 * <p>테스트 범위 — syncSingleKey()의 모든 분기:</p>
 * <pre>
 *   Redis 값 null      → early return (DB/Redis 미호출)
 *   Redis 값 0 이하    → early return (DB/Redis 미호출)
 *   UUID 파싱 실패     → 예외 전파 (Redis 키 보존)
 *   뉴스 없음(삭제됨)  → saveAndFlush 미호출 + Redis 키 삭제(정리)
 *   saveAndFlush 예외  → 예외 전파 (Redis 키 보존)
 *   정상 처리          → viewCount 업데이트 + saveAndFlush + Redis 키 삭제
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ViewCountSyncHelper 테스트")
class ViewCountSyncHelperTest {

    // 테스트 대상 (실제 구현체)
    @InjectMocks
    private ViewCountSyncHelper viewCountSyncHelper;

    // Redis 연산 Mock
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    // DB 접근 Mock
    @Mock
    private NewsRepository newsRepository;

    // opsForValue()가 반환하는 Redis 값 연산 객체 Mock
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private UUID testNewsId;
    private String testKey;  // "news:viewcount:{uuid}" 형태의 Redis 키
    private News testNews;

    @BeforeEach
    void setUp() {
        testNewsId = UUID.randomUUID();
        testKey = VIEW_COUNT_PREFIX + testNewsId;

        // 초기 조회수 100인 뉴스 엔티티
        testNews = News.builder()
                .id(testNewsId)
                .viewCount(100)
                .build();

        // 모든 테스트에서 opsForValue() 사용
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    // ==================== 정상 처리 ====================

    @Nested
    @DisplayName("정상 처리")
    class SuccessTests {

        @Test
        @DisplayName("DB 조회수에 Redis 증가분을 더하고, saveAndFlush 후 Redis 키를 삭제한다")
        void syncSingleKey_Success() {
            // given: Redis에 증가분 50이 있고, DB에 뉴스가 존재하는 상황
            given(valueOperations.get(testKey)).willReturn(50);
            given(newsRepository.findById(testNewsId)).willReturn(Optional.of(testNews));

            // when
            viewCountSyncHelper.syncSingleKey(testKey);

            // then 1: viewCount가 DB값(100) + Redis 증가분(50) = 150으로 업데이트됐는지 확인
            assertThat(testNews.getViewCount()).isEqualTo(150);

            // then 2: saveAndFlush가 호출됐는지 확인 (DB 즉시 반영)
            then(newsRepository).should(times(1)).saveAndFlush(testNews);

            // then 3: DB 반영 성공 후 Redis 키가 삭제됐는지 확인
            then(redisTemplate).should(times(1)).delete(testKey);
        }
    }

    // ==================== early return 분기 ====================

    @Nested
    @DisplayName("early return — Redis 값 무효")
    class EarlyReturnTests {

        @Test
        @DisplayName("Redis 값이 null이면 DB 조회 없이 즉시 반환한다")
        void syncSingleKey_NullValue_ReturnsEarly() {
            // given: 키가 만료됐거나 동시 처리로 이미 삭제된 상황
            given(valueOperations.get(testKey)).willReturn(null);

            // when
            viewCountSyncHelper.syncSingleKey(testKey);

            // then: DB 조회 미호출 (처리할 데이터가 없으므로)
            then(newsRepository).should(never()).findById(any());
            // then: Redis 키 삭제 미호출 (null이면 이미 없는 키)
            then(redisTemplate).should(never()).delete(testKey);
        }

        @Test
        @DisplayName("Redis 값이 0이면 DB 조회 없이 즉시 반환한다")
        void syncSingleKey_ZeroValue_ReturnsEarly() {
            // given: 증가분이 0인 상황 (정상적으로 발생하기 어렵지만 방어 처리)
            given(valueOperations.get(testKey)).willReturn(0);

            // when
            viewCountSyncHelper.syncSingleKey(testKey);

            // then: 증가분 없으므로 DB/Redis 모두 미호출
            then(newsRepository).should(never()).findById(any());
            then(redisTemplate).should(never()).delete(testKey);
        }

        @Test
        @DisplayName("Redis 값이 음수이면 DB 조회 없이 즉시 반환한다")
        void syncSingleKey_NegativeValue_ReturnsEarly() {
            // given: 데이터 오염 등으로 음수가 저장된 상황
            given(valueOperations.get(testKey)).willReturn(-1);

            // when
            viewCountSyncHelper.syncSingleKey(testKey);

            // then: 음수 증가분은 의미 없으므로 DB/Redis 모두 미호출
            then(newsRepository).should(never()).findById(any());
            then(redisTemplate).should(never()).delete(testKey);
        }
    }

    // ==================== 삭제된 뉴스 ====================

    @Nested
    @DisplayName("삭제된 뉴스 처리")
    class DeletedNewsTests {

        @Test
        @DisplayName("DB에 뉴스가 없으면(삭제됨) saveAndFlush 없이 Redis 키만 삭제한다")
        void syncSingleKey_NewsNotFound_DeletesRedisKey() {
            // given: 뉴스는 이미 삭제됐지만 Redis에 viewcount 키가 남아있는 상황
            given(valueOperations.get(testKey)).willReturn(50);
            given(newsRepository.findById(testNewsId)).willReturn(Optional.empty());

            // when
            viewCountSyncHelper.syncSingleKey(testKey);

            // then: 뉴스가 없으므로 saveAndFlush 미호출
            then(newsRepository).should(never()).saveAndFlush(any());

            // then: 재시도해도 의미 없으므로 Redis 키를 정리해야 함
            then(redisTemplate).should(times(1)).delete(testKey);
        }
    }

    // ==================== 예외 전파 (Redis 키 보존) ====================

    @Nested
    @DisplayName("예외 전파 — Redis 키 보존")
    class ExceptionPropagationTests {

        @Test
        @DisplayName("saveAndFlush 예외 발생 시 예외가 전파되고 Redis 키를 삭제하지 않는다")
        void syncSingleKey_SaveAndFlushFails_PreservesRedisKey() {
            // given: DB 장애 등으로 saveAndFlush가 실패하는 상황
            given(valueOperations.get(testKey)).willReturn(50);
            given(newsRepository.findById(testNewsId)).willReturn(Optional.of(testNews));
            // saveAndFlush 예외 → delete(key) 라인에 도달하지 못함 → Redis 키 보존
            willThrow(new RuntimeException("DB 연결 실패"))
                    .given(newsRepository).saveAndFlush(any(News.class));

            // when & then: 예외가 호출자(syncViewCountsToDb의 try-catch)로 전파됨
            assertThatThrownBy(() -> viewCountSyncHelper.syncSingleKey(testKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");

            // then: 예외로 인해 delete(key)에 도달하지 못했으므로 Redis 키 보존됨
            // → 다음 동기화 주기(5분 후)에 재시도 가능
            then(redisTemplate).should(never()).delete(testKey);
        }

        @Test
        @DisplayName("잘못된 UUID 형식의 키는 예외가 전파되고 Redis 키를 삭제하지 않는다")
        void syncSingleKey_InvalidUuidKey_PreservesRedisKey() {
            // given: 키 형식이 오염된 상황 ("news:viewcount:invalid-format")
            String invalidKey = VIEW_COUNT_PREFIX + "invalid-uuid-format";
            given(valueOperations.get(invalidKey)).willReturn(50);

            // when & then: UUID 파싱 실패로 IllegalArgumentException 전파
            assertThatThrownBy(() -> viewCountSyncHelper.syncSingleKey(invalidKey))
                    .isInstanceOf(IllegalArgumentException.class);

            // then: 파싱 실패 후 delete 라인에 도달하지 못하므로 키 보존
            then(newsRepository).should(never()).findById(any());
            then(redisTemplate).should(never()).delete(invalidKey);
        }
    }
}
