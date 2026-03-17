package com.lucr.messaging;

import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import com.lucr.messaging.CrawlResultListener.CrawlResultMessage;
import com.lucr.repository.KeywordRepository;
import com.lucr.repository.NewsStockRepository;
import com.lucr.service.CrawlJobService;
import com.lucr.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CrawlResultListener 테스트 - 메시지 수신 후 분기 로직 검증
 *
 * ======== 이 테스트의 목적 ========
 *
 * CrawlResultListener는 Python Worker가 보낸 메시지를 수신하여
 * 메시지의 status 필드에 따라 다른 처리를 합니다:
 *
 *   "COMPLETED" → crawlJobService.markCompleted() 호출
 *   "FAILED"    → crawlJobService.markFailed() 호출
 *   그 외        → 로그만 남김
 *   잘못된 UUID  → IllegalArgumentException catch → 로그만 남김
 *   기타 예외    → Exception catch → 로그만 남김
 *
 * 이 5개의 분기가 올바르게 동작하는지 검증합니다.
 *
 * ======== RabbitMQ는 필요 없음 ========
 *
 * @RabbitListener는 Spring이 메시지 → 메서드 호출을 자동으로 해주는 기능입니다.
 * 단위 테스트에서는 RabbitMQ 없이 handleCrawlResult() 메서드를 직접 호출하여
 * "내부 로직"만 검증합니다.
 *
 * 실제 RabbitMQ 연동은 통합 테스트에서 검증합니다.
 *
 * ======== Mock 구조 ========
 *
 *   CrawlResultListener (테스트 대상)
 *       ├── CrawlJobService (Mock) — markCompleted(), markFailed() 호출 검증
 *       ├── KeywordRepository (Mock) — 분석 통계 조회(count) 검증
 *       ├── NewsStockRepository (Mock) — 분석 통계 조회(count) 검증
 *       └── ObjectMapper (Mock) — Map → JSON 변환 검증
 *
 * @author Ekko0701
 * @since 2026-01-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlResultListener 테스트")
class CrawlResultListenerTest {

    /**
     * CrawlJobService Mock
     * Listener가 수신한 메시지에 따라 올바른 Service 메서드를 호출하는지 검증
     */
    @Mock
    private CrawlJobService crawlJobService;

    /**
     * RecommendationService Mock
     * COMPLETED 처리 후 추천 점수 자동 갱신(refreshAllRecommendations) 호출 검증
     */
    @Mock
    private RecommendationService recommendationService;

    /**
     * ObjectMapper Mock
     * Map<String, Integer> → JSON 문자열 변환을 시뮬레이션
     * 실제 ObjectMapper를 쓰면 Jackson 의존성이 필요하므로 Mock으로 대체
     */
    @Mock
    private ObjectMapper objectMapper;

    /**
     * KeywordRepository Mock
     * COMPLETED 처리 후 분석 통계 조회(count) 호출 검증
     */
    @Mock
    private KeywordRepository keywordRepository;

    /**
     * NewsStockRepository Mock
     * COMPLETED 처리 후 분석 통계 조회(count) 호출 검증
     */
    @Mock
    private NewsStockRepository newsStockRepository;

    /**
     * 테스트 대상: CrawlResultListener
     * 위의 Mock 2개가 자동으로 생성자에 주입됨
     */
    @InjectMocks
    private CrawlResultListener crawlResultListener;

    // 테스트 공통 데이터
    private UUID testJobId;
    private String testJobIdStr;
    private Map<String, Integer> testMediaResults;
    private CrawlJob completedJob;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testJobIdStr = testJobId.toString();

        // Python이 보내는 언론사별 수집 결과
        testMediaResults = Map.of(
                "hankyung", 45,
                "mk", 38,
                "edaily", 42
        );

        // markCompleted()가 반환할 CrawlJob
        completedJob = CrawlJob.builder()
                .id(testJobId)
                .status(CrawlJobStatus.COMPLETED)
                .totalArticles(125)
                .build();
    }

    // ========== 1. COMPLETED 상태 테스트 ==========

    @Nested
    @DisplayName("COMPLETED 상태 처리")
    class CompletedTests {

        /**
         * 핵심 시나리오: Python이 "COMPLETED" 메시지를 보냈을 때
         *
         * 기대 동작:
         *   1. objectMapper.writeValueAsString(mediaResults) 호출 → Map을 JSON으로 변환
         *   2. crawlJobService.markCompleted(jobId, totalArticles, jsonString) 호출
         *
         * 이 테스트에서 검증하는 것:
         *   - "COMPLETED" 분기로 진입하는가?
         *   - markCompleted()에 올바른 인자가 전달되는가?
         *   - markFailed()는 호출되지 않는가?
         */
        @Test
        @DisplayName("성공 - markCompleted() 호출됨")
        void handleCompleted_Success() throws Exception {
            // given: COMPLETED 메시지 생성
            CrawlResultMessage message = new CrawlResultMessage(
                    testJobIdStr,    // jobId (UUID 문자열)
                    "COMPLETED",     // status
                    125,             // totalArticles
                    testMediaResults // mediaResults
            );

            // objectMapper.writeValueAsString()이 호출되면 이 문자열을 반환
            String expectedJson = "{\"hankyung\":45,\"mk\":38,\"edaily\":42}";
            given(objectMapper.writeValueAsString(testMediaResults))
                    .willReturn(expectedJson);

            // markCompleted()가 호출되면 completedJob을 반환
            given(crawlJobService.markCompleted(testJobId, 125, expectedJson))
                    .willReturn(completedJob);

            // when: 메시지 수신 (직접 호출)
            crawlResultListener.handleCrawlResult(message);

            // then: markCompleted()가 정확히 1번, 올바른 인자로 호출됨
            then(crawlJobService).should(times(1))
                    .markCompleted(testJobId, 125, expectedJson);

            // 분석 통계 조회가 함께 실행되어야 함
            then(keywordRepository).should(times(1)).count();
            then(newsStockRepository).should(times(1)).count();

            // 추천 점수 자동 갱신이 호출되어야 함
            then(recommendationService).should(times(1)).refreshAllRecommendations();

            // markFailed()는 호출되지 않아야 함
            then(crawlJobService).should(never())
                    .markFailed(any(), anyString());
        }

        /**
         * STEP9 보강 케이스:
         * 통계 조회 중 예외가 나더라도(예: 일시적 DB 문제)
         * 메인 완료 처리(markCompleted)는 정상적으로 끝나야 합니다.
         *
         * CrawlResultListener.logAnalysisStats()는 내부 try-catch를 사용하므로
         * 예외를 외부로 전파하지 않고 무시합니다.
         */
        @Test
        @DisplayName("통계 조회 예외 - markCompleted()는 정상 처리")
        void handleCompleted_StatsQueryThrows_StillCompletes() throws Exception {
            // given
            CrawlResultMessage message = new CrawlResultMessage(
                    testJobIdStr,
                    "COMPLETED",
                    100,
                    testMediaResults
            );

            String expectedJson = "{\"hankyung\":45,\"mk\":38,\"edaily\":42}";
            given(objectMapper.writeValueAsString(testMediaResults))
                    .willReturn(expectedJson);
            given(crawlJobService.markCompleted(testJobId, 100, expectedJson))
                    .willReturn(completedJob);

            // 첫 번째 count()에서 예외 발생 → logAnalysisStats() 내부 catch로 처리됨
            given(keywordRepository.count()).willThrow(new RuntimeException("통계 조회 실패"));

            // when
            crawlResultListener.handleCrawlResult(message);

            // then: 핵심 완료 처리는 반드시 호출됨
            then(crawlJobService).should(times(1))
                    .markCompleted(testJobId, 100, expectedJson);

            // 첫 count는 시도됨
            then(keywordRepository).should(times(1)).count();

            // 첫 count 예외로 try 블록이 중단되어 두 번째 count는 호출되지 않음
            then(newsStockRepository).should(never()).count();
        }
    }

    // ========== 2. FAILED 상태 테스트 ==========

    @Nested
    @DisplayName("FAILED 상태 처리")
    class FailedTests {

        /**
         * Python이 "FAILED" 메시지를 보냈을 때
         *
         * 기대 동작:
         *   markFailed(jobId, "Python Worker에서 크롤링 실패") 호출
         */
        @Test
        @DisplayName("성공 - markFailed() 호출됨")
        void handleFailed_Success() {
            // given: FAILED 메시지 (totalArticles, mediaResults는 의미 없음)
            CrawlResultMessage message = new CrawlResultMessage(
                    testJobIdStr,
                    "FAILED",
                    0,
                    Map.of()
            );

            CrawlJob failedJob = CrawlJob.builder()
                    .id(testJobId)
                    .status(CrawlJobStatus.FAILED)
                    .build();

            given(crawlJobService.markFailed(testJobId, "Python Worker에서 크롤링 실패"))
                    .willReturn(failedJob);

            // when: 메시지 수신
            crawlResultListener.handleCrawlResult(message);

            // then: markFailed()가 호출됨
            then(crawlJobService).should(times(1))
                    .markFailed(testJobId, "Python Worker에서 크롤링 실패");

            // markCompleted()는 호출되지 않아야 함
            then(crawlJobService).should(never())
                    .markCompleted(any(), anyInt(), anyString());
        }
    }

    // ========== 3. 알 수 없는 상태 테스트 ==========

    @Nested
    @DisplayName("알 수 없는 상태 처리")
    class UnknownStatusTests {

        /**
         * Python이 "UNKNOWN" 같은 예상치 못한 상태를 보냈을 때
         *
         * 기대 동작:
         *   로그만 남기고, Service 메서드는 호출하지 않음
         *   예외도 발생하지 않음 (메시지는 ACK됨)
         */
        @Test
        @DisplayName("알 수 없는 상태 - Service 호출 없음 (로그만)")
        void handleUnknownStatus_NoServiceCall() {
            // given: 알 수 없는 상태
            CrawlResultMessage message = new CrawlResultMessage(
                    testJobIdStr,
                    "UNKNOWN_STATUS",  // COMPLETED도 FAILED도 아닌 값
                    0,
                    Map.of()
            );

            // when: 메시지 수신
            crawlResultListener.handleCrawlResult(message);

            // then: 어떤 Service 메서드도 호출되지 않음
            then(crawlJobService).should(never())
                    .markCompleted(any(), anyInt(), anyString());
            then(crawlJobService).should(never())
                    .markFailed(any(), anyString());
        }
    }

    // ========== 4. 잘못된 UUID 테스트 ==========

    @Nested
    @DisplayName("잘못된 UUID 처리")
    class InvalidUuidTests {

        /**
         * Python이 잘못된 형식의 jobId를 보냈을 때
         *
         * UUID.fromString("not-a-uuid") → IllegalArgumentException 발생
         * catch 블록에서 로그만 남기고 정상 종료 (메시지 ACK)
         *
         * 이것이 중요한 이유:
         *   잘못된 메시지 때문에 예외가 전파되면 Spring이 NACK를 보내고
         *   메시지가 큐에 다시 들어가서 무한 루프가 됨
         *   → catch로 잡아서 로그만 남기는 것이 올바른 처리
         */
        @Test
        @DisplayName("잘못된 UUID 형식 - 예외 없이 정상 종료")
        void handleInvalidUuid_CaughtGracefully() {
            // given: 잘못된 UUID 형식
            CrawlResultMessage message = new CrawlResultMessage(
                    "not-a-valid-uuid",  // UUID.fromString()에서 예외 발생
                    "COMPLETED",
                    100,
                    testMediaResults
            );

            // when & then: 예외가 외부로 전파되지 않음
            // assertDoesNotThrow 대신 호출만 해도 예외가 안 나면 통과
            crawlResultListener.handleCrawlResult(message);

            // Service 메서드는 호출되지 않음 (UUID 파싱 전에 예외)
            then(crawlJobService).should(never())
                    .markCompleted(any(), anyInt(), anyString());
            then(crawlJobService).should(never())
                    .markFailed(any(), anyString());
        }
    }

    // ========== 5. Service 예외 발생 테스트 ==========

    @Nested
    @DisplayName("Service 호출 중 예외 처리")
    class ServiceExceptionTests {

        /**
         * markCompleted()에서 예외가 발생했을 때
         *
         * 예: jobId가 유효하지만 DB에 해당 작업이 없는 경우
         * → ResourceNotFoundException 발생
         * → catch (Exception e) 블록에서 로그 남기고 정상 종료
         *
         * 이것도 무한 루프 방지를 위해 예외를 밖으로 전파하지 않음
         */
        @Test
        @DisplayName("markCompleted() 예외 - 예외 없이 정상 종료")
        void handleCompleted_ServiceThrows_CaughtGracefully() throws Exception {
            // given: COMPLETED 메시지
            CrawlResultMessage message = new CrawlResultMessage(
                    testJobIdStr,
                    "COMPLETED",
                    100,
                    testMediaResults
            );

            String expectedJson = "{\"hankyung\":45}";
            given(objectMapper.writeValueAsString(testMediaResults))
                    .willReturn(expectedJson);

            // markCompleted() 호출 시 RuntimeException 발생
            given(crawlJobService.markCompleted(testJobId, 100, expectedJson))
                    .willThrow(new RuntimeException("DB 연결 실패"));

            // when & then: 예외가 외부로 전파되지 않음
            crawlResultListener.handleCrawlResult(message);

            // markCompleted()는 호출되었지만 예외가 발생
            then(crawlJobService).should(times(1))
                    .markCompleted(testJobId, 100, expectedJson);
        }

        @Test
        @DisplayName("markFailed() 예외 - 예외 없이 정상 종료")
        void handleFailed_ServiceThrows_CaughtGracefully() {
            // given: FAILED 메시지
            CrawlResultMessage message = new CrawlResultMessage(
                    testJobIdStr,
                    "FAILED",
                    0,
                    Map.of()
            );

            // markFailed() 호출 시 예외 발생
            given(crawlJobService.markFailed(testJobId, "Python Worker에서 크롤링 실패"))
                    .willThrow(new RuntimeException("DB 연결 실패"));

            // when & then: 예외가 외부로 전파되지 않음
            crawlResultListener.handleCrawlResult(message);

            then(crawlJobService).should(times(1))
                    .markFailed(testJobId, "Python Worker에서 크롤링 실패");
        }
    }
}
