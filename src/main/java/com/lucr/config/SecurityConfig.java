package com.lucr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * <p>현재 단계: 임시 permitAll 설정 (JWT 인증 구현 전)</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>Spring Security의 기본 보안 정책을 덮어씀 (기본값: 모든 요청에 인증 필요)</li>
 *   <li>REST API에 맞는 보안 정책 적용 (CSRF 비활성화, Stateless 세션)</li>
 *   <li>PasswordEncoder Bean 등록 (회원가입 시 비밀번호 해싱에 사용)</li>
 * </ul>
 *
 * <h3>왜 필요한가?</h3>
 * <p>spring-boot-starter-security 의존성이 classpath에 존재하면,
 * Spring Boot가 자동으로 모든 HTTP 엔드포인트에 Basic 인증을 적용합니다.
 * 이 설정이 없으면 모든 API가 401 Unauthorized를 반환합니다.
 * SecurityFilterChain Bean을 등록하면 기본 설정을 대체합니다.</p>
 *
 * <h3>JWT 인증 구현 후 변경 예정 사항 (1-2 작업)</h3>
 * <pre>
 * auth
 *     .requestMatchers("/api/v1/auth/**").permitAll()       // 회원가입, 로그인
 *     .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") // 관리자 전용
 *     .anyRequest().authenticated()                          // 나머지는 인증 필요
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Configuration
@EnableWebSecurity  // Spring Security 웹 보안 활성화 + Spring MVC 통합
public class SecurityConfig {

    /**
     * HTTP 보안 필터 체인 설정
     *
     * <p>Spring Security는 서블릿 필터 기반으로 동작합니다.
     * 모든 HTTP 요청은 이 필터 체인을 통과한 후 컨트롤러에 도달합니다.</p>
     *
     * <p>요청 흐름: Client → SecurityFilterChain → Controller</p>
     *
     * @param http Spring Security가 제공하는 HttpSecurity 빌더
     * @return 구성 완료된 SecurityFilterChain
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site Request Forgery) 보호 비활성화
                // - REST API는 브라우저 폼 기반이 아닌 토큰 기반 인증을 사용하므로 불필요
                // - CSRF 토큰이 활성화되면 POST/PUT/DELETE 요청 시 403 Forbidden 발생
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 관리 정책: STATELESS
                // - 서버가 세션(HttpSession)을 생성하지 않음
                // - JWT 토큰 기반 인증에서는 서버가 상태를 유지할 필요가 없음
                // - 각 요청마다 JWT 토큰으로 인증 (추후 구현)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청별 접근 권한 설정
                // - 현재: 모든 요청 허용 (임시, JWT 구현 전)
                // - 추후: 엔드포인트별 인증/인가 규칙 적용 예정
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * 비밀번호 인코더 Bean 등록
     *
     * <p>BCrypt 해싱 알고리즘을 사용하여 비밀번호를 안전하게 저장합니다.</p>
     * <ul>
     *   <li>단방향 해싱: 원문 복원 불가</li>
     *   <li>솔트(salt) 자동 생성: 같은 비밀번호도 매번 다른 해시값</li>
     *   <li>강도(strength) 기본값 10: 2^10 = 1024 라운드 해싱</li>
     * </ul>
     *
     * <p>사용처: UserServiceImpl.register()에서 주입받아 비밀번호 해싱</p>
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
