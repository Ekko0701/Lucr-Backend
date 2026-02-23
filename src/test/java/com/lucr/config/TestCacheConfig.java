package com.lucr.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트용 캐시 설정
 *
 * <p>테스트 환경에서는 Redis 대신 메모리 기반 캐시(ConcurrentMapCacheManager)를 사용한다.
 * 이를 통해 Redis 서버 없이도 캐시 기능을 테스트할 수 있다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-13
 */
@TestConfiguration
@EnableCaching
public class TestCacheConfig {

    /**
     * 테스트용 CacheManager
     *
     * <p>{@code @Primary} 어노테이션으로 테스트 시 이 Bean이 우선적으로 사용되도록 한다.</p>
     *
     * @return 메모리 기반 CacheManager
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheConstants.NEWS,
                CacheConstants.NEWS_LIST,
                CacheConstants.NEWS_POPULAR,
                CacheConstants.NEWS_RECENT,
                CacheConstants.STOCK,
                CacheConstants.STOCK_LIST,
                CacheConstants.STOCK_MARKET
        );
    }
}
