package com.lucr.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 열거형
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E400001", "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "E400002", "타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "E400003", "필수 파라미터가 누락되었습니다."),
    
    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404001", "요청한 리소스를 찾을 수 없습니다."),
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "E404002", "뉴스를 찾을 수 없습니다."),
    
    // 409 Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E409001", "이미 존재하는 리소스입니다."),
    DUPLICATE_NEWS_URL(HttpStatus.CONFLICT, "E409002", "이미 존재하는 뉴스 URL입니다."),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500001", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500002", "데이터베이스 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
