package com.lucr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 설정 — 캐시 매니저 + RedisTemplate 구성
 *
 * <h3>직렬화 방식</h3>
 * <ul>
 *   <li>Key: {@link StringRedisSerializer} — 문자열 (사람이 읽을 수 있음)</li>
 *   <li>Value: {@link GenericJackson2JsonRedisSerializer} — JSON (디버깅 용이)</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Spring Boot 4.0에서 {@code GenericJackson2JsonRedisSerializer}는 deprecated되었으나,
 * 프로젝트가 Jackson 2와 Jackson 3를 혼용하고 있어 호환성을 위해 Jackson 2 버전을 계속 사용한다.
 * 경고는 {@code @SuppressWarnings("removal")}로 억제한다.</p>
 *
 * <h3>캐시별 TTL</h3>
 * <pre>
 * news          → 5분   (뉴스 상세)
 * news-list     → 2분   (뉴스 목록)
 * news-popular  → 1분   (인기 뉴스 — 조회수 기반이라 자주 변동)
 * news-recent   → 1분   (최신 뉴스 — 새 뉴스 유입 빈도 고려)
 * stock         → 1시간 (종목 상세 — 거의 변하지 않음)
 * stock-list    → 1시간 (종목 목록)
 * stock-market  → 1시간 (시장별 종목)
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-13
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
@SuppressWarnings("removal")  // GenericJackson2JsonRedisSerializer deprecated 경고 억제
public class RedisConfig {

    /**
     * RedisTemplate — 조회수 INCR 등 직접 Redis 명령을 실행할 때 사용
     *
     * <p>{@code @Cacheable}은 CacheManager를 사용하지만,
     * 조회수 증가(INCR) 같은 저수준 명령은 RedisTemplate으로 직접 실행한다.</p>
     *
     * @param connectionFactory Spring Boot가 자동 구성한 Redis 연결 팩토리
     * @return 설정된 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: 문자열 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON 직렬화 (Jackson 2)
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisCacheManager — 캐시별 TTL 설정
     *
     * <p>Spring Boot 4.x에서는 RedisCacheManager를 직접 Bean으로 정의하여
     * 각 캐시별로 다른 TTL을 설정한다.</p>
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 설정된 RedisCacheManager
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 캐시별 설정을 담을 Map
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 뉴스 단건: 5분
        cacheConfigurations.put(CacheConstants.NEWS,
                cacheConfig(Duration.ofSeconds(CacheConstants.NEWS_TTL_SECONDS)));

        // 뉴스 목록: 2분
        cacheConfigurations.put(CacheConstants.NEWS_LIST,
                cacheConfig(Duration.ofSeconds(CacheConstants.NEWS_LIST_TTL_SECONDS)));

        // 인기 뉴스: 1분
        cacheConfigurations.put(CacheConstants.NEWS_POPULAR,
                cacheConfig(Duration.ofSeconds(CacheConstants.NEWS_RANKING_TTL_SECONDS)));

        // 최신 뉴스: 1분
        cacheConfigurations.put(CacheConstants.NEWS_RECENT,
                cacheConfig(Duration.ofSeconds(CacheConstants.NEWS_RANKING_TTL_SECONDS)));

        // 종목 단건: 1시간
        cacheConfigurations.put(CacheConstants.STOCK,
                cacheConfig(Duration.ofSeconds(CacheConstants.STOCK_TTL_SECONDS)));

        // 종목 목록: 1시간
        cacheConfigurations.put(CacheConstants.STOCK_LIST,
                cacheConfig(Duration.ofSeconds(CacheConstants.STOCK_TTL_SECONDS)));

        // 시장별 종목: 1시간
        cacheConfigurations.put(CacheConstants.STOCK_MARKET,
                cacheConfig(Duration.ofSeconds(CacheConstants.STOCK_TTL_SECONDS)));

        // RedisCacheManager 빌드
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig(Duration.ofMinutes(10)))  // 기본 TTL: 10분
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    // ==================== private 헬퍼 ====================

    /**
     * 캐시 설정 생성 헬퍼
     *
     * @param ttl 캐시 유효 시간
     * @return RedisCacheConfiguration
     */
    private RedisCacheConfiguration cacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper())))
                .disableCachingNullValues();
    }

    /**
     * Redis 전용 ObjectMapper (Jackson 2)
     *
     * <p>Jackson이 LocalDateTime 등 Java 8 타입을 올바르게 직렬화/역직렬화하려면
     * JavaTimeModule 등록이 필요하다.</p>
     *
     * <p>{@code activateDefaultTyping}은 역직렬화 시 원래 클래스 타입을 복원하기 위해 필요하다.
     * JSON에 {@code "@class": "com.lucr.dto.response.NewsResponse"} 같은 타입 정보가 포함된다.</p>
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 날짜/시간 모듈 등록 (LocalDateTime 등)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 다형성 타입 정보 활성화 — 역직렬화 시 올바른 클래스로 복원
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        return mapper;
    }
}
