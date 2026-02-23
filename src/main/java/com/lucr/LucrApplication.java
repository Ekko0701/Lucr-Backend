package com.lucr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class LucrApplication {

	public static void main(String[] args) {
		SpringApplication.run(LucrApplication.class, args);
	}

	/**
	 * 캐시 활성화 (Redis 사용 시에만)
	 *
	 * <p>테스트 환경에서는 spring.cache.type=simple로 설정되므로 이 설정이 무시된다.</p>
	 */
	@Configuration
	@EnableCaching
	@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
	static class CachingConfig {
	}
}
