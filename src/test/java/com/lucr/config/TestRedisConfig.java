package com.lucr.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Redis 설정
 *
 * <p>통합 테스트에서 실제 Redis 서버 없이도 Bean 생성을 지원합니다.</p>
 *
 * <p>Spring Data Redis Repository 기능을 비활성화하고
 * RedisTemplate만 Mock으로 제공합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-25
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * 테스트용 Mock RedisTemplate
     *
     * <p>ViewCountService가 사용하는 RedisTemplate을 Mock으로 제공합니다.</p>
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // Mock ConnectionFactory 사용
        template.setConnectionFactory(redisConnectionFactory());
        
        // Serializer 설정 (실제로는 사용 안 되지만 Bean 생성을 위해 필요)
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 테스트용 Mock RedisConnectionFactory
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // LettuceConnectionFactory Mock (실제 연결은 하지 않음)
        return mock(LettuceConnectionFactory.class);
    }
}
