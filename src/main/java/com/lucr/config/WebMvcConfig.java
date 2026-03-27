package com.lucr.config;

import com.lucr.entity.CrawlJob.CrawlJobStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정 - 커스텀 타입 변환기 등록
 *
 * @author Ekko0701
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToCrawlJobStatusConverter());
    }

    /**
     * String → CrawlJobStatus 변환기
     *
     * 기본 Spring enum 변환은 대소문자를 구분하므로 "completed" → 400 에러 발생.
     * 이 변환기는 trim + toUpperCase 후 변환하여 대소문자 무관하게 처리.
     *
     * 잘못된 값("done", "COMPLETEDD" 등)은 IllegalArgumentException →
     * MethodArgumentTypeMismatchException → GlobalExceptionHandler에서 400 반환
     */
    static class StringToCrawlJobStatusConverter implements Converter<String, CrawlJobStatus> {

        @Override
        public CrawlJobStatus convert(String source) {
            return CrawlJobStatus.valueOf(source.trim().toUpperCase());
        }
    }
}
