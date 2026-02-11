package com.lucr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 Properties
 *
 * <p>application.yml의 {@code jwt.*} 설정을 타입 안전하게 바인딩하는 클래스입니다.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>JWT 토큰 생성 및 검증에 필요한 설정값을 중앙 관리</li>
 *   <li>환경별(dev/prod) 설정 분리 지원 (application-{profile}.yml)</li>
 *   <li>환경 변수를 통한 민감 정보(secret) 주입</li>
 * </ul>
 *
 * <h3>동작 원리</h3>
 * <p>{@code @ConfigurationProperties(prefix = "jwt")}에 의해
 * application.yml에서 {@code jwt.} 접두사로 시작하는 프로퍼티가 자동으로 매핑됩니다.</p>
 *
 * <pre>
 * # application.yml 매핑 예시
 * jwt:
 *   secret: ${JWT_SECRET}                    → JwtProperties.secret
 *   access-token-expiration: 1800000         → JwtProperties.accessTokenExpiration
 *   refresh-token-expiration: 604800000      → JwtProperties.refreshTokenExpiration
 *   issuer: lucr-api                         → JwtProperties.issuer
 * </pre>
 *
 * <p>※ YAML의 kebab-case ({@code access-token-expiration})가
 * Java의 camelCase ({@code accessTokenExpiration})로 자동 변환됩니다.
 * 이는 Spring Boot의 Relaxed Binding 기능입니다.</p>
 *
 * <h3>사용처</h3>
 * <ul>
 *   <li>{@code JwtTokenProvider} — 토큰 생성 시 서명 키, 만료 시간, 발급자 정보 참조</li>
 *   <li>{@code AuthServiceImpl} — RefreshToken 만료 시간 확인 시 참조</li>
 * </ul>
 *
 * <h3>왜 @Component + @ConfigurationProperties 조합인가?</h3>
 * <p>{@code @Component}로 직접 Bean 등록하면 별도의
 * {@code @EnableConfigurationProperties(JwtProperties.class)} 선언이 불필요합니다.
 * 또한 다른 Bean에서 {@code @Autowired} 또는 생성자 주입으로 바로 사용할 수 있습니다.</p>
 *
 * <h3>왜 @Setter가 필요한가?</h3>
 * <p>Spring Boot의 {@code @ConfigurationProperties}는 기본적으로
 * setter 메서드를 통해 값을 주입합니다. Lombok의 {@code @Setter}가
 * 모든 필드에 대한 setter를 자동 생성합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see com.lucr.security.JwtTokenProvider
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC-SHA256 서명에 사용할 비밀 키
     *
     * <p>JWT 토큰의 무결성을 보장하기 위한 대칭 키입니다.
     * 이 키로 토큰에 서명하고, 동일한 키로 서명을 검증합니다.</p>
     *
     * <h4>보안 요구사항</h4>
     * <ul>
     *   <li>최소 256비트 (32바이트) 이상의 길이 필수 (HS256 알고리즘 요구)</li>
     *   <li>랜덤하고 예측 불가능한 문자열 사용</li>
     *   <li>절대 소스 코드에 하드코딩 금지 — 반드시 환경 변수로 주입</li>
     * </ul>
     *
     * <h4>환경 변수 매핑</h4>
     * <pre>
     * # application.yml
     * jwt:
     *   secret: ${JWT_SECRET}
     *
     * # .env 또는 시스템 환경 변수
     * JWT_SECRET=your-256-bit-secret-key-must-be-at-least-32-characters-long
     * </pre>
     *
     * @see com.lucr.security.JwtTokenProvider#init() — 이 값을 SecretKey로 변환
     */
    private String secret;

    /**
     * AccessToken 만료 시간 (밀리초 단위)
     *
     * <p>AccessToken은 API 요청 시 Authorization 헤더에 포함되는 단기 토큰입니다.
     * 만료되면 클라이언트는 RefreshToken으로 새 AccessToken을 발급받아야 합니다.</p>
     *
     * <h4>기본값: 1,800,000ms (30분)</h4>
     * <ul>
     *   <li>너무 짧으면 → 사용자가 자주 토큰 갱신해야 함 (UX 저하)</li>
     *   <li>너무 길면 → 토큰 탈취 시 악용 가능 시간 증가 (보안 위험)</li>
     *   <li>일반적 권장: 15분 ~ 1시간</li>
     * </ul>
     *
     * <p>application.yml에서 {@code jwt.access-token-expiration}으로 오버라이드 가능합니다.</p>
     */
    private long accessTokenExpiration = 1_800_000;

    /**
     * RefreshToken 만료 시간 (밀리초 단위)
     *
     * <p>RefreshToken은 AccessToken을 갱신하기 위한 장기 토큰입니다.
     * DB에 저장되며, 만료 시 사용자는 다시 로그인해야 합니다.</p>
     *
     * <h4>기본값: 604,800,000ms (7일)</h4>
     * <ul>
     *   <li>너무 짧으면 → 사용자가 자주 재로그인해야 함</li>
     *   <li>너무 길면 → 토큰 탈취 시 장기간 악용 가능</li>
     *   <li>일반적 권장: 7일 ~ 30일</li>
     * </ul>
     *
     * <p>application.yml에서 {@code jwt.refresh-token-expiration}으로 오버라이드 가능합니다.</p>
     */
    private long refreshTokenExpiration = 604_800_000;

    /**
     * 토큰 발급자 (JWT 표준 클레임: iss)
     *
     * <p>JWT payload에 포함되는 {@code iss} (issuer) 클레임 값입니다.
     * 토큰 검증 시 발급자가 일치하는지 확인하여 위변조를 방지합니다.</p>
     *
     * <h4>기본값: "lucr-api"</h4>
     *
     * <h4>JWT 표준 클레임 참고</h4>
     * <ul>
     *   <li>{@code iss} (issuer) — 토큰 발급자 (이 값)</li>
     *   <li>{@code sub} (subject) — 토큰 대상 (사용자 ID)</li>
     *   <li>{@code iat} (issued at) — 발급 시각</li>
     *   <li>{@code exp} (expiration) — 만료 시각</li>
     *   <li>{@code jti} (JWT ID) — 토큰 고유 식별자</li>
     * </ul>
     *
     * <p>application.yml에서 {@code jwt.issuer}로 오버라이드 가능합니다.</p>
     */
    private String issuer = "lucr-api";
}
