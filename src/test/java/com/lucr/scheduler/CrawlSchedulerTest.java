package com.lucr.scheduler;

import com.lucr.entity.CrawlJob;
import com.lucr.messaging.CrawlJobPublisher;
import com.lucr.service.CrawlJobService;
import com.lucr.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlScheduler 테스트")
class CrawlSchedulerTest {

    @Mock
    private CrawlJobService crawlJobService;

    @Mock
    private CrawlJobPublisher crawlJobPublisher;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private CrawlScheduler crawlScheduler;

    @Nested
    @DisplayName("scheduledCrawl()")
    class ScheduledCrawlTests {

        @Test
        @DisplayName("비활성화 상태 - 아무것도 실행하지 않음")
        void scheduledCrawl_Disabled_DoesNothing() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", false);

            crawlScheduler.scheduledCrawl();

            then(crawlJobService).should(never()).hasRunningJob();
            then(crawlJobService).should(never()).createJob();
        }

        @Test
        @DisplayName("이미 실행 중인 Job이 있으면 스킵")
        void scheduledCrawl_AlreadyRunning_Skips() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", true);
            given(crawlJobService.hasRunningJob()).willReturn(true);

            crawlScheduler.scheduledCrawl();

            then(crawlJobService).should(never()).createJob();
            then(crawlJobPublisher).should(never()).publish(any(), anyInt());
        }

        @Test
        @DisplayName("정상 실행 - Job 생성 + 메시지 발행")
        void scheduledCrawl_Success() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", true);
            ReflectionTestUtils.setField(crawlScheduler, "maxArticles", 30);

            UUID jobId = UUID.randomUUID();
            CrawlJob job = CrawlJob.builder().id(jobId).build();
            given(crawlJobService.hasRunningJob()).willReturn(false);
            given(crawlJobService.createJob()).willReturn(job);

            crawlScheduler.scheduledCrawl();

            then(crawlJobService).should(times(1)).createJob();
            then(crawlJobPublisher).should(times(1)).publish(jobId, 30);
        }

        @Test
        @DisplayName("createJob 예외 - 로그만 남기고 정상 종료")
        void scheduledCrawl_CreateJobThrows_CaughtGracefully() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", true);
            given(crawlJobService.hasRunningJob()).willReturn(false);
            given(crawlJobService.createJob()).willThrow(new RuntimeException("DB 오류"));

            crawlScheduler.scheduledCrawl();

            then(crawlJobPublisher).should(never()).publish(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("cleanupExpiredRecommendations()")
    class CleanupTests {

        @Test
        @DisplayName("비활성화 상태 - 아무것도 실행하지 않음")
        void cleanup_Disabled_DoesNothing() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", false);

            crawlScheduler.cleanupExpiredRecommendations();

            then(recommendationService).should(never()).cleanupExpiredRecommendations();
        }

        @Test
        @DisplayName("정상 실행 - 만료된 추천 정리")
        void cleanup_Success() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", true);
            given(recommendationService.cleanupExpiredRecommendations()).willReturn(5);

            crawlScheduler.cleanupExpiredRecommendations();

            then(recommendationService).should(times(1)).cleanupExpiredRecommendations();
        }

        @Test
        @DisplayName("예외 발생 - 로그만 남기고 정상 종료")
        void cleanup_Throws_CaughtGracefully() {
            ReflectionTestUtils.setField(crawlScheduler, "schedulerEnabled", true);
            given(recommendationService.cleanupExpiredRecommendations())
                    .willThrow(new RuntimeException("DB 오류"));

            crawlScheduler.cleanupExpiredRecommendations();

            // 예외가 외부로 전파되지 않으면 테스트 통과
        }
    }
}
