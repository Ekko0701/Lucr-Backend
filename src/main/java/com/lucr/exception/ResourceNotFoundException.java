package com.lucr.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 뉴스를 찾을 수 없는 경우
     */
    public static ResourceNotFoundException newsNotFound(String id) {
        return new ResourceNotFoundException(
                ErrorCode.NEWS_NOT_FOUND,
                "뉴스를 찾을 수 없습니다: " + id
        );
    }

    /**
     * 크롤링 작업을 찾을 수 없는 경우
     */
    public static ResourceNotFoundException crawlJobNotFound(String id) {
        return new ResourceNotFoundException(
                ErrorCode.CRAWL_JOB_NOT_FOUND,
                "크롤링 작업을 찾을 수 없습니다: " + id
        );
    }
}
