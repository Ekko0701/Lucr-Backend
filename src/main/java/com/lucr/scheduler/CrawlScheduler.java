package com.lucr.scheduler;

import com.lucr.entity.CrawlJob;
import com.lucr.messaging.CrawlJobPublisher;
import com.lucr.service.CrawlJobService;
import com.lucr.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlScheduler {

    private final CrawlJobService crawlJobService;
    private final CrawlJobPublisher crawlJobPublisher;
    private final RecommendationService recommendationService;

    @Value("${crawl.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${crawl.scheduler.max-articles:30}")
    private int maxArticles;

    /**
     * 정기 크롤링 실행 (기본: 매 6시간)
     *
     * cron 표현식: 초 분 시 일 월 요일
     * "0 0 0/6 * * *" = 매일 0시, 6시, 12시, 18시에 실행
     *
     * application.yml에서 cron 식을 오버라이드할 수 있습니다:
     *   crawl.scheduler.cron: "0 0 0/6 * * *"
     */
    @Scheduled(cron = "${crawl.scheduler.cron:0 0 0/6 * * *}")
    public void scheduledCrawl() {
        if (!schedulerEnabled) {
            log.debug("정기 크롤링 스케줄러 비활성화 상태");
            return;
        }

        // 이미 실행 중인 Job이 있으면 스킵
        if (crawlJobService.hasRunningJob()) {
            log.info("이미 실행 중인 크롤링 작업이 있어 스케줄 실행을 건너뜁니다.");
            return;
        }

        log.info("=== 정기 크롤링 시작 (maxArticles={}) ===", maxArticles);

        try {
            CrawlJob job = crawlJobService.createJob();
            crawlJobPublisher.publish(job.getId(), maxArticles);
            log.info("정기 크롤링 작업 발행: jobId={}", job.getId());
        } catch (Exception e) {
            log.error("정기 크롤링 작업 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * 만료된 추천 정리 (매일 새벽 3시)
     */
    @Scheduled(cron = "${crawl.scheduler.cleanup-cron:0 0 3 * * *}")
    public void cleanupExpiredRecommendations() {
        if (!schedulerEnabled) {
            return;
        }

        try {
            int deleted = recommendationService.cleanupExpiredRecommendations();
            if (deleted > 0) {
                log.info("만료된 추천 {}건 정리 완료", deleted);
            }
        } catch (Exception e) {
            log.warn("만료된 추천 정리 실패: {}", e.getMessage());
        }
    }
}
