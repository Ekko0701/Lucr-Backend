package com.lucr.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 메시지 브로커 설정
 *
 * 메시지 흐름:
 *   [Spring Boot] ---(crawl.request)---> [Exchange] ---> [Request Queue] ---> [Python Crawler]
 *   [Python Crawler] ---(crawl.result)---> [Exchange] ---> [Result Queue] ---> [Spring Boot]
 *
 * @author kimdongjoo
 * @since 2026-02-06
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange (메시지 라우터) ──
    // 모든 크롤링 메시지를 Routing Key 기반으로 적절한 Queue에 전달하는 교환기
    public static final String CRAWL_EXCHANGE = "lucr.crawl.exchange";

    // ── Queue (메시지 저장소) ──
    // Spring → Python: 크롤링 요청 메시지가 대기하는 큐
    public static final String CRAWL_REQUEST_QUEUE = "lucr.crawl.request";
    // Python → Spring: 크롤링 완료 결과가 대기하는 큐
    public static final String CRAWL_RESULT_QUEUE = "lucr.crawl.result";

    // ── Routing Key (메시지 라우팅 규칙) ──
    // Exchange가 메시지를 어떤 Queue로 보낼지 결정하는 키
    public static final String CRAWL_REQUEST_KEY = "crawl.request";
    public static final String CRAWL_RESULT_KEY = "crawl.result";

    /**
     * 메시지 변환기 - Java 객체 ↔ JSON 자동 직렬화/역직렬화
     * Spring Boot가 메시지를 보내거나 받을 때 JSON 형식으로 변환
     */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Topic Exchange 등록
     * Routing Key 패턴 매칭으로 메시지를 적절한 Queue에 분배
     */
    @Bean
    public TopicExchange crawlExchange() {
        return new TopicExchange(CRAWL_EXCHANGE);
    }

    /**
     * 크롤링 요청 큐 (durable: RabbitMQ 재시작 시에도 큐 유지)
     * Spring이 발행한 크롤링 요청을 Python Worker가 소비
     */
    @Bean
    public Queue crawlRequestQueue() {
        return QueueBuilder.durable(CRAWL_REQUEST_QUEUE).build();
    }

    /**
     * 크롤링 결과 큐 (durable: RabbitMQ 재시작 시에도 큐 유지)
     * Python Worker가 발행한 완료 이벤트를 Spring이 소비
     */
    @Bean
    public Queue crawlResultQueue() {
        return QueueBuilder.durable(CRAWL_RESULT_QUEUE).build();
    }

    /**
     * 요청 큐 바인딩: Exchange → Request Queue
     * "crawl.request" 키로 발행된 메시지를 crawlRequestQueue로 라우팅
     */
    @Bean
    public Binding crawlRequestBinding(Queue crawlRequestQueue, TopicExchange crawlExchange) {
        return BindingBuilder.bind(crawlRequestQueue).to(crawlExchange).with(CRAWL_REQUEST_KEY);
    }

    /**
     * 결과 큐 바인딩: Exchange → Result Queue
     * "crawl.result" 키로 발행된 메시지를 crawlResultQueue로 라우팅
     */
    @Bean
    public Binding crawlResultBinding(Queue crawlResultQueue, TopicExchange crawlExchange) {
        return BindingBuilder.bind(crawlResultQueue).to(crawlExchange).with(CRAWL_RESULT_KEY);
    }
}
