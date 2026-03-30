package com.lucr.security;

import com.lucr.exception.ErrorCode;
import com.lucr.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증되지 않은 요청에 대한 401 Unauthorized 응답 처리
 *
 * <p>Spring Security의 {@link AuthenticationEntryPoint}를 구현합니다.
 * {@code SecurityFilterChain}에서 인증이 필요한 엔드포인트에
 * 유효한 JWT 없이 접근했을 때 이 핸들러가 호출됩니다.</p>
 *
 * <h3>호출 시나리오</h3>
 * <ul>
 *   <li>Authorization 헤더가 없는 요청 → 인증 필요 경로 접근</li>
 *   <li>Bearer 토큰이 만료되거나 유효하지 않은 경우</li>
 *   <li>토큰 형식이 올바르지 않은 경우 (Bearer 접두사 누락 등)</li>
 * </ul>
 *
 * <h3>응답 형식</h3>
 * <p>프로젝트의 {@code ErrorResponse} 형식에 맞춰 JSON 응답을 직접 작성합니다.
 * Spring Security의 예외 처리는 컨트롤러 밖에서 발생하므로
 * {@code GlobalExceptionHandler}를 거치지 않고 직접 응답을 구성해야 합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see JwtAccessDeniedHandler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 인증 실패 시 401 JSON 응답 반환
     *
     * @param request       HTTP 요청 (실패한 요청 URI 로깅용)
     * @param response      HTTP 응답 (401 상태 코드 + JSON 본문)
     * @param authException Spring Security가 전달하는 인증 예외
     * @throws IOException 응답 작성 중 I/O 오류
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("인증 실패 — URI: {}, 메시지: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED_ACCESS);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
