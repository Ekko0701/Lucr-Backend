package com.lucr;

import com.lucr.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Lucr 애플리케이션 통합 테스트
 *
 * <p>Spring Boot 컨텍스트가 정상적으로 로드되는지 검증합니다.</p>
 *
 * <p>테스트 환경:</p>
 * <ul>
 *   <li>Redis: Mock RedisTemplate (Redis 서버 불필요)</li>
 *   <li>Redis Repository: 비활성화 (excludeAutoConfiguration)</li>
 *   <li>Cache: Simple Cache (ConcurrentMapCacheManager)</li>
 *   <li>DB: H2 인메모리 DB</li>
 *   <li>RabbitMQ: Listener 자동 시작 비활성화</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-25
 */
@SpringBootTest(properties = {
	"spring.data.redis.repositories.enabled=false"  // Redis Repository 비활성화
})
@Import(TestRedisConfig.class)
class LucrApplicationTests {

	@Test
	void contextLoads() {
		// Spring Boot 컨텍스트가 정상적으로 로드되는지 테스트
	}

}
