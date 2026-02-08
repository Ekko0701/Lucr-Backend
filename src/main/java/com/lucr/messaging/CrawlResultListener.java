package com.lucr.messaging;

import com.lucr.config.RabbitMQConfig;
import com.lucr.service.CrawlJobService;
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
 *   이 Listener가 해당 이벤트를 수신하여 CrawlJob의 상태를 DB에 반영합니다.
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
        log.info("크롤링 결과 수신: jobId={}, status={}, totalArticles={}",
                message.jobId(), message.status(), message.totalArticles());

        try {
            UUID jobId = UUID.fromString(message.jobId());

            switch (message.status()) {
                case "COMPLETED" -> {
                    // Jackson 3.x ObjectMapper로 Map → JSON 문자열 변환
                    // 예: {"hankyung": 45, "mk": 38} → "{\"hankyung\":45,\"mk\":38}"
                    String mediaResultsJson = objectMapper.writeValueAsString(message.mediaResults());
                    crawlJobService.markCompleted(jobId, message.totalArticles(), mediaResultsJson);
                    log.info("크롤링 작업 완료 처리: jobId={}, total={}건", jobId, message.totalArticles());
                }
                case "FAILED" -> {
                    crawlJobService.markFailed(jobId, "Python Worker에서 크롤링 실패");
                    log.warn("크롤링 작업 실패 처리: jobId={}", jobId);
                }
                default -> log.warn("알 수 없는 상태: jobId={}, status={}", jobId, message.status());
            }

        } catch (IllegalArgumentException e) {
            log.error("잘못된 jobId 형식: {}", message.jobId());
        } catch (Exception e) {
            log.error("크롤링 결과 처리 실패: {}", e.getMessage(), e);
        }
    }
}
