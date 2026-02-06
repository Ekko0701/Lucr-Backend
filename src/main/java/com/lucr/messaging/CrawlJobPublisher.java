package com.lucr.messaging;

import com.lucr.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 크롤링 요청 메시지 발행자 (Spring → RabbitMQ → Python)
 *
 * 역할:
 *   AdminController에서 크롤링 요청이 들어오면,
 *   RabbitMQ Exchange에 메시지를 발행하여 Python Crawler가 소비하도록 전달합니다.
 *
 * 흐름:
 *   publish() 호출
 *     → CrawlRequestMessage 객체 생성
 *     → JacksonJsonMessageConverter가 JSON으로 변환
 *     → Exchange(lucr.crawl.exchange)에 발행
 *     → Binding 규칙에 따라 Request Queue(lucr.crawl.request)로 라우팅
 *     → Python Worker가 큐에서 소비
 *
 * @author Ekko0701
 * @since 2026-02-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlJobPublisher {

    /** Spring AMQP가 자동 등록하는 메시지 발행 도구 */
    private final RabbitTemplate rabbitTemplate;

    /**
     * Python Crawler에 전달되는 크롤링 요청 메시지 DTO
     *
     * JSON 변환 결과:
     * {
     *   "jobId": "550e8400-e29b-41d4-a716-446655440000",
     *   "maxArticles": 50
     * }
     *
     * @param jobId       CrawlJob의 UUID (작업 추적용)
     * @param maxArticles 언론사당 최대 수집 기사 수
     */
    public record CrawlRequestMessage(
            String jobId,
            int maxArticles
    ) {}

    /**
     * 크롤링 요청 메시지를 RabbitMQ에 발행
     *
     * @param jobId       CrawlJob UUID
     * @param maxArticles 언론사당 최대 수집 기사 수
     */
    public void publish(UUID jobId, int maxArticles) {
        // 1. 메시지 객체 생성 (UUID → String 변환하여 JSON 호환성 확보)
        CrawlRequestMessage message = new CrawlRequestMessage(
                jobId.toString(),
                maxArticles
        );

        // 2. Exchange + Routing Key로 메시지 발행
        //    - MessageConverter(JacksonJsonMessageConverter)가 message → JSON 자동 변환
        //    - Exchange가 Routing Key를 보고 Request Queue로 라우팅
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWL_EXCHANGE,      // 목적지 Exchange
                RabbitMQConfig.CRAWL_REQUEST_KEY,    // 라우팅 키
                message                              // 메시지 (자동 JSON 변환)
        );

        log.info("크롤링 요청 메시지 발행: jobId={}, maxArticles={}", jobId, maxArticles);
    }
}
