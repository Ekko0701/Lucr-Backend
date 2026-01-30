package com.lucr.exception;

/**
 * 중복된 리소스가 존재할 때 발생하는 예외
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 중복된 뉴스 URL인 경우
     */
    public static DuplicateResourceException duplicateNewsUrl(String url) {
        return new DuplicateResourceException(
                ErrorCode.DUPLICATE_NEWS_URL,
                "이미 존재하는 뉴스 URL입니다: " + url
        );
    }
}
