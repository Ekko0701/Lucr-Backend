package com.lucr.security;

import com.lucr.exception.ErrorCode;
import com.lucr.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 권한 부족 요청에 대한 403 Forbidden 응답 처리
 *
 * <p>Spring Security의 {@link AccessDeniedHandler}를 구현합니다.
 * 인증은 되었으나(JWT 유효) 해당 리소스에 대한 권한이 없을 때 호출됩니다.</p>
 *
 * <h3>호출 시나리오</h3>
 * <ul>
 *   <li>USER 역할로 {@code /api/v1/admin/**} (ADMIN 전용) 경로 접근</li>
 *   <li>USER 역할로 뉴스 POST/PUT/DELETE 요청 (ADMIN만 가능)</li>
 *   <li>{@code @PreAuthorize("hasRole('ADMIN')")} 메서드 접근 시 권한 부족</li>
 * </ul>
 *
 * <h3>401 vs 403 차이</h3>
 * <ul>
 *   <li><b>401 Unauthorized</b> — 인증 자체가 안 됨 (토큰 없음/만료/무효)
 *       → {@link JwtAuthenticationEntryPoint} 처리</li>
 *   <li><b>403 Forbidden</b> — 인증은 됐지만 권한이 부족
 *       → 이 핸들러 처리</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see JwtAuthenticationEntryPoint
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 접근 거부 시 403 JSON 응답 반환
     *
     * @param request               HTTP 요청 (실패한 요청 URI 로깅용)
     * @param response              HTTP 응답 (403 상태 코드 + JSON 본문)
     * @param accessDeniedException Spring Security가 전달하는 접근 거부 예외
     * @throws IOException 응답 작성 중 I/O 오류
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("접근 거부 — URI: {}, 메시지: {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.ACCESS_DENIED);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
