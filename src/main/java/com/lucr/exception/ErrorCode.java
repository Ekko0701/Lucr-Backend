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
    
    // 401 Unauthorized — 인증 실패
    /** 로그인 시 비밀번호 불일치 */
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "E401001", "비밀번호가 일치하지 않습니다."),
    /** JWT AccessToken 또는 RefreshToken 유효기간 만료 */
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "E401002", "토큰이 만료되었습니다."),
    /** JWT 서명 불일치, 형식 오류, 지원하지 않는 토큰 등 */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E401003", "유효하지 않은 토큰입니다."),
    /** Authorization 헤더 누락 또는 Bearer 토큰 미제공 */
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "E401004", "인증이 필요합니다. 유효한 토큰을 포함하여 요청해주세요."),

    // 403 Forbidden — 인가 실패
    /** 인증은 되었으나 해당 리소스에 대한 권한이 없을 때 (RBAC) */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "E403001", "접근 권한이 없습니다."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404001", "요청한 리소스를 찾을 수 없습니다."),
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "E404002", "뉴스를 찾을 수 없습니다."),
    CRAWL_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "E404003", "크롤링 작업을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E404004", "사용자를 찾을 수 없습니다."),
    /** 토큰 갱신 시 DB에 해당 RefreshToken이 존재하지 않을 때 */
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "E404005", "리프레시 토큰을 찾을 수 없습니다."),
    /** 종목코드로 조회했으나 해당 종목이 존재하지 않을 때 */
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "E404006", "종목을 찾을 수 없습니다."),
    /** 종목코드로 추천 조회했으나 해당 추천 정보가 존재하지 않을 때 */
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E404007", "해당 종목의 추천 정보를 찾을 수 없습니다."),
    
    // 409 Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E409001", "이미 존재하는 리소스입니다."),
    DUPLICATE_NEWS_URL(HttpStatus.CONFLICT, "E409002", "이미 존재하는 뉴스 URL입니다."),
    CRAWL_JOB_ALREADY_RUNNING(HttpStatus.CONFLICT, "E409003", "이미 실행 중인 크롤링 작업이 있습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "E409004", "이미 사용 중인 이메일입니다."),
    /** 이미 등록된 종목코드로 종목 생성을 시도할 때 */
    DUPLICATE_STOCK_CODE(HttpStatus.CONFLICT, "E409005", "이미 존재하는 종목코드입니다."),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500001", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500002", "데이터베이스 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
