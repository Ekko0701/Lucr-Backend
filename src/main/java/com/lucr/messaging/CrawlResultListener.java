package com.lucr.messaging;

import com.lucr.config.RabbitMQConfig;
import com.lucr.repository.KeywordRepository;
import com.lucr.repository.NewsStockRepository;
import com.lucr.service.CrawlJobService;
import com.lucr.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * 크롤링 완료 이벤트 Listener (Python → RabbitMQ → Spring)
 *
 * 역할:
 *   Python Worker가 크롤링을 완료(또는 실패)하면 RabbitMQ에 결과 이벤트를 발행합니다.
 *   이 Listener가 해당 이벤트를 수신하여 CrawlJob의 상태를 DB에 반영하고,
 *   크롤링 완료 시 추천 점수를 자동 갱신합니다.
 *
 * 메시지 흐름:
 *   Python CrawlResultPublisher
 *     → Exchange(lucr.crawl.exchange) + Routing Key(crawl.result)
 *     → Queue(lucr.crawl.result)
 *     → 이 Listener의 handleCrawlResult() 자동 호출
 *
 * 수신 JSON 예시:
 *   {
 *     "jobId": "550e8400-e29b-41d4-a716-446655440000",
 *     "status": "COMPLETED",
 *     "totalArticles": 245,
 *     "mediaResults": {"hankyung": 45, "mk": 38, ...}
 *   }
 *
 * ObjectMapper 관련:
 *   Spring Boot 4.x는 Jackson 3.x (tools.jackson 패키지)를 사용합니다.
 *   spring-boot-starter-json이 ObjectMapper Bean을 자동 등록하므로
 *   DI로 주입받아 사용합니다.
 *
 * @author Ekko0701
 * @since 2026-01-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlResultListener {

    private final CrawlJobService crawlJobService;
    private final RecommendationService recommendationService;
    private final KeywordRepository keywordRepository;
    private final NewsStockRepository newsStockRepository;

    /**
     * Jackson 3.x ObjectMapper - JSON 직렬화/역직렬화 도구
     *
     * Spring Boot 4.x (Jackson 3.x) 패키지: tools.jackson.databind.ObjectMapper
     * Spring Boot 3.x (Jackson 2.x) 패키지: com.fasterxml.jackson.databind.ObjectMapper
     *
     * spring-boot-starter-json이 자동 등록한 Bean을 DI로 주입받습니다.
     */
    private final ObjectMapper objectMapper;

    /**
     * Python Worker가 발행한 크롤링 결과 메시지 DTO
     *
     * JSON 키 이름이 camelCase로 일치하므로
     * JacksonJsonMessageConverter가 자동으로 매핑합니다.
     *
     * @param jobId         CrawlJob UUID (문자열)
     * @param status        "COMPLETED" 또는 "FAILED"
     * @param totalArticles 수집된 총 기사 수
     * @param mediaResults  언론사별 수집 결과 {"hankyung": 45, "mk": 38, ...}
     */
    public record CrawlResultMessage(
            String jobId,
            String status,
            int totalArticles,
            Map<String, Integer> mediaResults
    ) {}

    /**
     * RabbitMQ lucr.crawl.result 큐에서 메시지를 수신하는 콜백
     *
     * @RabbitListener 동작 원리:
     *   1. Spring Boot 시작 시 이 메서드를 RabbitMQ Consumer로 자동 등록
     *   2. lucr.crawl.result Queue에 메시지가 도착하면 이 메서드가 자동 호출
     *   3. JacksonJsonMessageConverter가 JSON → CrawlResultMessage 자동 변환
     *   4. 메서드가 정상 완료되면 자동 ACK (예외 발생 시 자동 NACK)
     *
     * Python의 pika Consumer와 비교:
     *   - Python: channel.basic_consume() + 콜백 등록 + 수동 ACK
     *   - Spring: @RabbitListener 하나로 전부 해결 (자동 ACK)
     *
     * @param message JacksonJsonMessageConverter가 변환한 메시지 객체
     */
    @RabbitListener(queues = RabbitMQConfig.CRAWL_RESULT_QUEUE)
    public void handleCrawlResult(CrawlResultMessage message) {
        log.info("크롤링+분석 결과 수신: jobId={}, status={}, totalArticles={}",
                message.jobId(), message.status(), message.totalArticles());

        try {
            UUID jobId = UUID.fromString(message.jobId());

            switch (message.status()) {
                case "COMPLETED" -> {
                    // Jackson 3.x ObjectMapper로 Map → JSON 문자열 변환
                    // 예: {"hankyung": 45, "mk": 38} → "{\"hankyung\":45,\"mk\":38}"
                    String mediaResultsJson = objectMapper.writeValueAsString(message.mediaResults());
                    crawlJobService.markCompleted(jobId, message.totalArticles(), mediaResultsJson);
                    log.info("크롤링+분석 완료 처리: jobId={}, total={}건", jobId, message.totalArticles());

                    // 분석 결과 누적 통계를 운영 로그로 남깁니다.
                    logAnalysisStats();

                    // 추천 점수 자동 갱신 (실패해도 크롤링 결과 저장에 영향 없음)
                    try {
                        int updated = recommendationService.refreshAllRecommendations();
                        log.info("크롤링 완료 후 추천 갱신: {}개 종목", updated);
                    } catch (Exception e) {
                        log.warn("추천 갱신 실패 (크롤링 결과는 정상 저장됨): {}", e.getMessage());
                    }
                }
                case "FAILED" -> {
                    crawlJobService.markFailed(jobId, "Python Worker에서 크롤링 실패");
                    log.warn("크롤링+분석 실패 처리: jobId={}", jobId);
                }
                default -> log.warn("알 수 없는 상태: jobId={}, status={}", jobId, message.status());
            }

        } catch (IllegalArgumentException e) {
            log.error("잘못된 jobId 형식: {}", message.jobId());
        } catch (Exception e) {
            log.error("크롤링 결과 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 분석 누적 통계를 로그로 출력합니다.
     *
     * 통계 로깅 실패는 메인 흐름(CrawlJob 상태 업데이트)에 영향을 주지 않도록
     * 예외를 내부에서 처리합니다.
     */
    private void logAnalysisStats() {
        try {
            long totalKeywords = keywordRepository.count();
            long totalNewsStocks = newsStockRepository.count();

            log.info(
                    "분석 통계 — 누적 키워드: {}개, 누적 뉴스-종목 연결: {}개",
                    totalKeywords, totalNewsStocks
            );
        } catch (Exception e) {
            log.warn("분석 통계 조회 실패 (무시됨): {}", e.getMessage());
        }
    }
}
