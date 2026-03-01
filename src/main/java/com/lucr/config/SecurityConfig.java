package com.lucr.config;

import com.lucr.security.JwtAccessDeniedHandler;
import com.lucr.security.JwtAuthenticationEntryPoint;
import com.lucr.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정 — JWT 인증 + 역할 기반 접근 제어 (RBAC)
 *
 * <h3>변경 이력</h3>
 * <ul>
 *   <li>[1-1] 임시 permitAll 설정 + PasswordEncoder Bean</li>
 *   <li>[1-2] JWT 인증 필터 + 경로별 인가 규칙 + AuthenticationManager + 예외 핸들러</li>
 * </ul>
 *
 * <h3>요청 흐름</h3>
 * <pre>
 * Client
 *   → JwtAuthenticationFilter (Bearer 토큰 검증, SecurityContext 설정)
 *   → SecurityFilterChain (경로별 인가 규칙 적용)
 *       ├── 인증 실패 → JwtAuthenticationEntryPoint (401)
 *       ├── 권한 부족 → JwtAccessDeniedHandler (403)
 *       └── 통과     → Controller
 * </pre>
 *
 * <h3>인가 규칙 (RBAC)</h3>
 * <pre>
 * 경로                        │ 접근 조건
 * ────────────────────────────┼────────────────────
 * /api/v1/auth/login          │ permitAll (인증 불필요)
 * /api/v1/auth/register       │ permitAll
 * /api/v1/auth/check-email    │ permitAll
 * /api/v1/auth/refresh        │ permitAll
 * /swagger-ui/**              │ permitAll (API 문서)
 * /v3/api-docs/**             │ permitAll
 * /actuator/**                │ permitAll (헬스 체크)
 * /api/v1/admin/**            │ ADMIN 역할만
 * GET /api/v1/news/**         │ 인증된 사용자 (USER + ADMIN)
 * POST/PUT/DELETE /api/v1/news│ ADMIN 역할만
 * GET /api/v1/stocks/**       │ 인증된 사용자 (USER + ADMIN)
 * POST/DELETE /api/v1/stocks  │ ADMIN 역할만
 * 나머지 모든 경로              │ 인증 필요 (authenticated)
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Configuration
@EnableWebSecurity       // Spring Security 웹 보안 활성화
@EnableMethodSecurity    // @PreAuthorize, @PostAuthorize 어노테이션 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 인증 필터 — Bearer 토큰 검증 + SecurityContext 설정 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 인증 실패 핸들러 — 401 Unauthorized JSON 응답 */
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /** 접근 거부 핸들러 — 403 Forbidden JSON 응답 */
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /** CORS 허용 Origin 목록 — application.yml에서 주입 */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * JwtAuthenticationFilter의 Servlet 필터 자동 등록 비활성화
     *
     * <p>{@code @Component}로 선언된 {@code JwtAuthenticationFilter}는
     * Spring Boot에 의해 Servlet 필터로 자동 등록됩니다.
     * 하지만 이 필터는 {@link #securityFilterChain(HttpSecurity)}에서
     * {@code addFilterBefore()}로 SecurityFilterChain 내부에 추가되므로,
     * Servlet 필터로도 등록되면 <strong>이중 실행</strong>됩니다.</p>
     *
     * <h4>이중 등록 시 발생하는 문제</h4>
     * <pre>
     * 1. JwtAuthenticationFilter (Servlet 필터) 실행 → SecurityContext 설정
     * 2. SecurityContextHolderFilter 실행 → SecurityContext를 빈 컨텍스트로 덮어씀!
     * 3. JwtAuthenticationFilter (SecurityFilterChain 내부) → OncePerRequestFilter로 스킵
     * 4. 인증 정보 유실 → 모든 요청 401 Unauthorized
     * </pre>
     *
     * <p>Servlet 필터 등록을 비활성화하여 SecurityFilterChain 내부에서만 실행되도록 합니다.</p>
     */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * CORS 설정 Bean — 환경별 Origin 허용 + 인증 정보 포함
     *
     * <p>Cross-Origin Resource Sharing을 설정하여 다른 도메인에서의 API 요청을 허용합니다.</p>
     *
     * <h4>환경별 설정</h4>
     * <pre>
     * 개발(dev):   http://localhost:3000, http://localhost:3001 (전체 허용)
     * 운영(prod):  https://your-production-domain.com (엄격 제한)
     * </pre>
     *
     * <h4>설정 항목</h4>
     * <ul>
     *   <li><strong>allowedOrigins</strong>: application.yml에서 환경별로 주입</li>
     *   <li><strong>allowedMethods</strong>: GET, POST, PUT, DELETE, OPTIONS (Preflight)</li>
     *   <li><strong>allowedHeaders</strong>: 모든 헤더 허용 (Authorization, Content-Type 등)</li>
     *   <li><strong>allowCredentials</strong>: true (쿠키, Authorization 헤더 포함)</li>
     *   <li><strong>maxAge</strong>: 3600초 (Preflight 응답 캐시)</li>
     * </ul>
     *
     * @return CORS 설정이 적용된 CorsConfigurationSource
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 Origin (쉼표로 구분된 문자열을 List로 변환)
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));

        // 2. 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. 허용할 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 인증 정보 포함 허용 (Authorization 헤더, 쿠키)
        configuration.setAllowCredentials(true);

        // 5. Preflight 응답 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        // 6. 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * BCrypt 비밀번호 인코더 Bean
     *
     * <p>회원가입 시 비밀번호 해싱, 로그인 시 비밀번호 검증에 사용됩니다.</p>
     * <ul>
     *   <li>단방향 해싱: 원문 복원 불가</li>
     *   <li>솔트(salt) 자동 생성: 같은 비밀번호도 매번 다른 해시값</li>
     *   <li>강도(strength) 기본값 10: 2^10 = 1024 라운드 해싱</li>
     * </ul>
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager Bean — DB 기반 사용자 인증 수행
     *
     * <p>{@link DaoAuthenticationProvider}를 사용하여
     * {@code CustomUserDetailsService}에서 사용자를 조회하고,
     * {@code BCryptPasswordEncoder}로 비밀번호를 검증합니다.</p>
     *
     * <h4>Spring Security 7.x 방식</h4>
     * <p>Spring Security 7.x에서는 {@code AuthenticationManagerBuilder} 대신
     * {@link ProviderManager}를 직접 생성하는 방식을 권장합니다.</p>
     *
     * @param userDetailsService CustomUserDetailsService (이메일로 사용자 조회)
     * @param passwordEncoder    BCryptPasswordEncoder (비밀번호 해시 비교)
     * @return 구성 완료된 AuthenticationManager
     */
    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);         // 비밀번호 검증 위임
        return new ProviderManager(provider);
    }

    /**
     * HTTP 보안 필터 체인 설정 — CSRF, CORS, 세션, 인가 규칙, JWT 필터, 예외 핸들러
     *
     * <p>모든 HTTP 요청은 이 필터 체인을 통과한 후 컨트롤러에 도달합니다.</p>
     *
     * <h4>필터 실행 순서</h4>
     * <pre>
     * 1. CorsFilter (Spring Security가 자동 생성, cors() 설정 기반)
     * 2. JwtAuthenticationFilter (커스텀, addFilterBefore로 추가)
     * 3. UsernamePasswordAuthenticationFilter (Spring Security 기본, 비활성화 상태)
     * 4. ExceptionTranslationFilter (인증/인가 예외를 EntryPoint/Handler로 위임)
     * 5. AuthorizationFilter (authorizeHttpRequests 규칙 적용)
     * </pre>
     *
     * @param http                     Spring Security가 제공하는 HttpSecurity 빌더
     * @param corsConfigurationSource  CORS 설정 Bean (환경별 Origin 허용)
     * @return 구성 완료된 SecurityFilterChain
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                // 1. CORS 설정 (환경별 Origin 허용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // 2. CSRF 비활성화
                //    REST API는 토큰 기반 인증을 사용하므로 CSRF 보호가 불필요
                .csrf(AbstractHttpConfigurer::disable)

                // 3. 세션 관리: Stateless
                //    JWT 기반 인증이므로 서버 측 세션을 사용하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. 예외 처리 핸들러 등록
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401 처리
                        .accessDeniedHandler(jwtAccessDeniedHandler)            // 403 처리
                )

                // 5. 경로별 인가 규칙 (RBAC)
                .authorizeHttpRequests(auth -> auth
                        // === 인증 없이 접근 가능 ===
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/check-email",
                                "/api/v1/auth/refresh"
                        ).permitAll()

                        // Swagger / OpenAPI 문서
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Actuator 헬스 체크
                        .requestMatchers("/actuator/**").permitAll()

                        // === 관리자 전용 (ADMIN 역할) ===
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // === 뉴스: 조회는 인증된 사용자 모두, CUD는 ADMIN만 ===
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/news/*/view").authenticated()  // 조회수 증가는 인증된 사용자 모두
                        .requestMatchers(HttpMethod.POST, "/api/v1/news/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/news/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/news/**").hasRole("ADMIN")

                        // === 종목: 조회는 인증된 사용자 모두, 등록/삭제는 ADMIN만 ===
                        .requestMatchers(HttpMethod.GET, "/api/v1/stocks/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/stocks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/stocks/**").hasRole("ADMIN")

                        // === 나머지 모든 요청은 인증 필요 ===
                        .anyRequest().authenticated()
                )

                // 6. JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                //    Spring Security 기본 폼 로그인 필터보다 먼저 JWT 인증 수행
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
