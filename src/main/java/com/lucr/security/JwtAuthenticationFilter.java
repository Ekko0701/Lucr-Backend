package com.lucr.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * JWT 인증 필터 — 모든 HTTP 요청에서 Bearer 토큰을 검증하고 SecurityContext를 설정
 *
 * <p>{@link OncePerRequestFilter}를 상속하여 요청당 한 번만 실행됩니다.
 * Spring Security 필터 체인의 {@code UsernamePasswordAuthenticationFilter} 앞에 위치하여,
 * 토큰 기반 인증을 폼 로그인보다 먼저 처리합니다.</p>
 *
 * <h3>전체 처리 흐름</h3>
 * <pre>
 * Client 요청 (Authorization: Bearer eyJhbGci...)
 *     ↓
 * ┌─── JwtAuthenticationFilter ────────────────────────────────────┐
 * │                                                                 │
 * │  1. shouldNotFilter() — 공개 경로이면 필터 스킵                    │
 * │          ↓ (인증 필요 경로)                                       │
 * │  2. resolveToken() — Authorization 헤더에서 "Bearer " 제거        │
 * │          ↓ (토큰이 있으면)                                        │
 * │  3. jwtTokenProvider.validateToken() — 서명 + 만료 + iss 검증     │
 * │          ↓ (유효하면)                                             │
 * │  4. JWT Claims에서 userId, email, role 추출                       │
 * │          ↓                                                      │
 * │  5. UsernamePasswordAuthenticationToken 생성                     │
 * │          ↓                                                      │
 * │  6. SecurityContextHolder에 Authentication 설정                  │
 * │          ↓                                                      │
 * │  7. filterChain.doFilter() — 다음 필터 / 컨트롤러로 전달           │
 * │                                                                 │
 * └─────────────────────────────────────────────────────────────────┘
 *     ↓
 * SecurityFilterChain.authorizeHttpRequests() — 경로별 인가 규칙 적용
 *     ↓
 * Controller 도달
 * </pre>
 *
 * <h3>토큰이 없거나 유효하지 않은 경우</h3>
 * <p>SecurityContext를 설정하지 않고 다음 필터로 그대로 전달합니다.
 * 이후 {@code authorizeHttpRequests()} 규칙에 따라:</p>
 * <ul>
 *   <li>{@code permitAll()} 경로 → 정상 처리</li>
 *   <li>{@code authenticated()} 경로 → {@code JwtAuthenticationEntryPoint}가 401 반환</li>
 *   <li>{@code hasRole("ADMIN")} 경로 → {@code JwtAccessDeniedHandler}가 403 반환</li>
 * </ul>
 *
 * <h3>왜 DB 조회를 하지 않는가? (Stateless 인증)</h3>
 * <p>JWT AccessToken의 Claims에 userId, email, role이 이미 포함되어 있으므로,
 * 매 요청마다 DB를 조회하지 않고 토큰만으로 인증/인가를 수행합니다.
 * 이것이 JWT Stateless 인증의 핵심 장점입니다.</p>
 *
 * <h3>왜 {@code OncePerRequestFilter}인가?</h3>
 * <p>서블릿 컨테이너에서 forward/include 시 필터가 중복 실행될 수 있습니다.
 * {@code OncePerRequestFilter}는 요청당 한 번만 실행을 보장합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see JwtTokenProvider
 * @see OncePerRequestFilter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** HTTP 요청 헤더 이름 (OAuth 2.0 표준) */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 토큰 접두사 (뒤에 공백 포함) */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 토큰 생성/검증/파싱 담당 */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 필터 핵심 로직 — 토큰 추출 → 검증 → SecurityContext 설정
     *
     * <p>이 메서드는 {@code shouldNotFilter()}가 {@code false}를 반환하는 요청에 대해서만 실행됩니다.</p>
     *
     * <h4>인증 성공 시</h4>
     * <p>{@link SecurityContextHolder}에 {@link UsernamePasswordAuthenticationToken}을 설정합니다.
     * 이후 컨트롤러에서 {@code SecurityContextHolder.getContext().getAuthentication().getPrincipal()}로
     * 현재 인증된 사용자의 UUID를 가져올 수 있습니다.</p>
     *
     * <h4>인증 실패 또는 토큰 없음</h4>
     * <p>SecurityContext를 설정하지 않고 다음 필터로 전달합니다.
     * 예외를 직접 던지지 않고, Spring Security의 인가 처리에 위임합니다.</p>
     *
     * @param request     HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @param response    HTTP 응답 (이 필터에서는 직접 사용하지 않음)
     * @param filterChain 다음 필터로 요청을 전달하는 체인
     * @throws ServletException 서블릿 처리 중 오류
     * @throws IOException      I/O 오류
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Authorization 헤더에서 Bearer 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰이 존재하고 유효한 경우에만 인증 처리
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            try {
                // 3. JWT Claims에서 사용자 정보 추출 (DB 조회 없음)
                UUID userId = jwtTokenProvider.getUserId(token);
                String email = jwtTokenProvider.getEmail(token);
                String role = jwtTokenProvider.getRole(token);

                // 4. Spring Security Authentication 객체 생성
                //    - principal: 사용자 UUID (컨트롤러에서 getPrincipal()로 접근)
                //    - credentials: null (토큰 기반 인증이므로 비밀번호 불필요)
                //    - authorities: ROLE_USER 또는 ROLE_ADMIN (RBAC에 사용)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,           // principal: 사용자 ID
                                null,             // credentials: 토큰 기반이므로 null
                                Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + role)
                                )
                        );

                // 5. SecurityContext에 인증 정보 설정
                //    이후 SecurityFilterChain의 인가 규칙(hasRole 등)이 이 정보를 참조
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 인증 성공: userId={}, email={}, role={}", userId, email, role);

            } catch (Exception e) {
                // JWT 파싱 중 예상치 못한 오류 발생 시 인증 정보 제거
                // (예: Claims 추출 실패, UUID 파싱 오류 등)
                log.error("JWT 인증 처리 중 오류: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 6. 다음 필터로 요청 전달 (인증 성공/실패 여부와 무관하게 항상 실행)
        //    인증되지 않은 요청은 SecurityFilterChain의 인가 규칙에서 거부됨
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 문자열 추출
     *
     * <p>헤더 형식: {@code Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...}</p>
     *
     * <h4>처리 규칙</h4>
     * <ul>
     *   <li>헤더가 없으면 → {@code null} 반환</li>
     *   <li>헤더가 "Bearer "로 시작하지 않으면 → {@code null} 반환</li>
     *   <li>"Bearer " 접두사를 제거하고 순수 토큰 문자열만 반환</li>
     * </ul>
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 (Bearer 접두사 제거된), 또는 {@code null}
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 인증이 필요 없는 공개 경로에서는 이 필터를 스킵
     *
     * <p>JWT 토큰 파싱은 CPU 연산이 필요하므로,
     * 인증 없이 접근 가능한 공개 경로에서는 불필요한 파싱을 건너뜁니다.</p>
     *
     * <h4>스킵 대상 경로</h4>
     * <ul>
     *   <li>{@code /api/v1/auth/login} — 로그인 (토큰 없이 접근)</li>
     *   <li>{@code /api/v1/auth/register} — 회원가입 (토큰 없이 접근)</li>
     *   <li>{@code /api/v1/auth/check-email} — 이메일 중복 확인</li>
     *   <li>{@code /api/v1/auth/refresh} — 토큰 갱신 (Body에 RefreshToken 전달)</li>
     *   <li>{@code /swagger-ui/**} — Swagger UI 문서</li>
     *   <li>{@code /v3/api-docs/**} — OpenAPI 스펙</li>
     *   <li>{@code /actuator/**} — 헬스 체크 등 운영 엔드포인트</li>
     * </ul>
     *
     * <p>※ 이 필터 스킵과 {@code SecurityConfig}의 {@code permitAll()}은 별개입니다.
     * {@code shouldNotFilter}는 성능 최적화 목적이고,
     * {@code permitAll()}은 인가(Authorization) 규칙입니다.</p>
     *
     * @param request HTTP 요청
     * @return {@code true} — 필터 스킵 (공개 경로), {@code false} — 필터 실행
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/check-email")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }
}
