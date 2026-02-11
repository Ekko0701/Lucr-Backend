package com.lucr.exception;

/**
 * 인증 관련 예외
 *
 * <ul>
 *   <li>토큰 만료 (EXPIRED_TOKEN)</li>
 *   <li>토큰 무효 (INVALID_TOKEN)</li>
 *   <li>비밀번호 불일치 (INVALID_PASSWORD)</li>
 *   <li>인증 필요 (UNAUTHORIZED_ACCESS)</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
public class AuthenticationException extends BusinessException {

    /**
     * ErrorCode 기반 생성자
     *
     * @param errorCode 인증 관련 에러 코드 (EXPIRED_TOKEN, INVALID_TOKEN 등)
     */
    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode + 커스텀 메시지 생성자
     *
     * @param errorCode 인증 관련 에러 코드
     * @param message   상세 에러 메시지 (기본 메시지 대신 사용)
     */
    public AuthenticationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * JWT 토큰 만료 시 사용
     *
     * <p>JwtTokenProvider.validateToken()에서 ExpiredJwtException 발생 시,
     * 또는 RefreshToken 만료 확인 시 호출됩니다.</p>
     */
    public static AuthenticationException expiredToken() {
        return new AuthenticationException(ErrorCode.EXPIRED_TOKEN);
    }

    /**
     * JWT 토큰이 유효하지 않을 때 사용
     *
     * <p>서명 불일치, 형식 오류, 지원하지 않는 토큰 등
     * 토큰 자체에 문제가 있는 경우 호출됩니다.</p>
     */
    public static AuthenticationException invalidToken() {
        return new AuthenticationException(ErrorCode.INVALID_TOKEN);
    }

    /**
     * 로그인 시 비밀번호가 일치하지 않을 때 사용
     *
     * <p>AuthServiceImpl.login()에서 PasswordEncoder.matches() 실패 시 호출됩니다.</p>
     */
    public static AuthenticationException invalidPassword() {
        return new AuthenticationException(ErrorCode.INVALID_PASSWORD);
    }

    /**
     * 인증 정보 없이 인증 필요 API에 접근할 때 사용
     *
     * <p>Authorization 헤더가 없거나 Bearer 토큰이 누락된 경우 호출됩니다.</p>
     */
    public static AuthenticationException unauthorized() {
        return new AuthenticationException(ErrorCode.UNAUTHORIZED_ACCESS);
    }
}
