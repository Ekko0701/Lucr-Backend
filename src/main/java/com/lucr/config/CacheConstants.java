package com.lucr.config;

/**
 * 캐시 이름 및 TTL 상수 정의
 *
 * <p>캐시 이름을 상수로 관리하여 Service 코드와 RedisConfig에서 동일한 이름을 사용한다.</p>
 *
 * <h4>캐시 전략</h4>
 * <ul>
 *   <li>변경 빈도 높음 (뉴스 목록): 짧은 TTL (1~2분)</li>
 *   <li>변경 빈도 중간 (뉴스 상세): 중간 TTL (5분)</li>
 *   <li>변경 빈도 낮음 (종목 정보): 긴 TTL (1시간)</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-13
 */
public final class CacheConstants {

    private CacheConstants() {
        // 인스턴스 생성 방지
    }

    // ==================== 캐시 이름 ====================

    /** 뉴스 단건 조회 캐시 */
    public static final String NEWS = "news";

    /** 뉴스 목록 캐시 */
    public static final String NEWS_LIST = "news-list";

    /** 인기 뉴스 목록 캐시 */
    public static final String NEWS_POPULAR = "news-popular";

    /** 최신 뉴스 목록 캐시 */
    public static final String NEWS_RECENT = "news-recent";

    /** 종목 단건 조회 캐시 */
    public static final String STOCK = "stock";

    /** 종목 목록 캐시 */
    public static final String STOCK_LIST = "stock-list";

    /** 시장별 종목 목록 캐시 */
    public static final String STOCK_MARKET = "stock-market";

    // ==================== TTL (초 단위) ====================

    /** 뉴스 단건: 5분 */
    public static final long NEWS_TTL_SECONDS = 300;

    /** 뉴스 목록: 2분 */
    public static final long NEWS_LIST_TTL_SECONDS = 120;

    /** 인기/최신 뉴스: 1분 */
    public static final long NEWS_RANKING_TTL_SECONDS = 60;

    /** 종목 관련: 1시간 */
    public static final long STOCK_TTL_SECONDS = 3600;

    // ==================== Redis 키 프리픽스 ====================

    /** 조회수 Redis 키 프리픽스 */
    public static final String VIEW_COUNT_PREFIX = "news:viewcount:";
}
