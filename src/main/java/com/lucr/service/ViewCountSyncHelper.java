package com.lucr.service;

import com.lucr.config.CacheConstants;
import com.lucr.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.lucr.config.CacheConstants.VIEW_COUNT_PREFIX;

@Service
@RequiredArgsConstructor
public class ViewCountSyncHelper {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NewsRepository newsRepository;

    @Transactional
    public void syncSingleKey(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return;

        long increment = ((Number) value).longValue();
        if (increment <= 0) return;

        UUID newsId = UUID.fromString(key.replace(VIEW_COUNT_PREFIX, ""));

        newsRepository.findById(newsId).ifPresent(news -> {
            int newViewCount = news.getViewCount() + (int) increment;
            news.setViewCount(newViewCount);
            newsRepository.saveAndFlush(news);

            // ① DB 기준값 갱신 → 다음 getViewCount() 호출 시 정확한 합산
            redisTemplate.opsForValue().set(CacheConstants.VIEW_COUNT_DB_PREFIX + newsId, (long) newViewCount);

            // ② 콘텐츠 캐시 무효화 → stale viewCount 방지
            redisTemplate.delete(CacheConstants.NEWS + "::" + newsId);
        });

        // 증가분 버퍼 초기화 (DB 커밋 완료 후)
        redisTemplate.delete(key);
    }
}
