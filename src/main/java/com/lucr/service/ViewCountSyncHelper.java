package com.lucr.service;

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
            news.setViewCount(news.getViewCount() + (int) increment);
            newsRepository.saveAndFlush(news);
        });

        // 트랜잭션 커밋 완료 후 Redis 삭제 → 유실 없음
        // (saveAndFlush 예외 시 여기까지 오지 않음 → Redis 보존)
        redisTemplate.delete(key);
    }
}
