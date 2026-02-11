package com.lucr.security;

import com.lucr.config.JwtProperties;
import com.lucr.exception.AuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 / 검증 / 파싱을 담당하는 핵심 컴포넌트
 *
 * <p>JJWT (io.jsonwebtoken) 0.12.x 라이브러리를 사용합니다.</p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>AccessToken 생성 — 사용자 인증 정보를 담은 단기 토큰</li>
 *   <li>RefreshToken 생성 — AccessToken 갱신용 장기 토큰</li>
 *   <li>토큰 유효성 검증 — 서명, 만료, 형식, 발급자(iss) 확인</li>
 *   <li>토큰에서 Claims 추출 — 사용자 ID, 이메일, 역할 파싱</li>
 * </ul>
 *
 * <h3>토큰 구조 (JWT Payload Claims)</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │ AccessToken Claims                                       │
 * ├──────────┬──────────────────────────────────────────────┤
 * │ sub      │ 사용자 ID (UUID)                               │
 * │ email    │ 사용자 이메일 (커스텀 클레임)                      │
 * │ role     │ 사용자 역할 — USER / ADMIN (커스텀 클레임)         │
 * │ iss      │ 발급자 — lucr-api                               │
 * │ iat      │ 발급 시간 (Unix timestamp)                      │
 * │ exp      │ 만료 시간 (Unix timestamp)                      │
 * │ jti      │ 토큰 고유 ID (UUID, 중복 방지)                   │
 * └──────────┴──────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ RefreshToken Claims                                      │
 * ├──────────┬──────────────────────────────────────────────┤
 * │ sub      │ 사용자 ID (UUID)                               │
 * │ type     │ "refresh" (커스텀 클레임, 토큰 종류 구분)          │
 * │ iss      │ 발급자 — lucr-api                               │
 * │ iat      │ 발급 시간                                       │
 * │ exp      │ 만료 시간                                       │
 * │ jti      │ 토큰 고유 ID                                    │
 * └──────────┴──────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>서명 알고리즘</h3>
 * <p>HMAC-SHA256 (HS256) 대칭키 방식을 사용합니다.
 * {@code signWith(secretKey)} 호출 시 JJWT가 키 길이를 기반으로 알고리즘을 자동 선택합니다.
 * (256비트 키 → HS256, 384비트 → HS384, 512비트 → HS512)</p>
 *
 * <h3>사용 흐름</h3>
 * <pre>
 * 1. 로그인 성공 → generateAccessToken() + generateRefreshToken()
 * 2. API 요청   → JwtAuthenticationFilter에서 validateToken() → getUserId(), getRole()
 * 3. 토큰 갱신  → validateToken(refreshToken) → generateAccessToken()
 * </pre>
 *
 * <h3>JJWT 0.12.x API 참고</h3>
 * <p>0.11.x에서 deprecated된 메서드 대신 새 API를 사용합니다:</p>
 * <ul>
 *   <li>{@code setSubject()} → {@code subject()}</li>
 *   <li>{@code setIssuedAt()} → {@code issuedAt()}</li>
 *   <li>{@code setExpiration()} → {@code expiration()}</li>
 *   <li>{@code signWith(key, algorithm)} → {@code signWith(key)} (알고리즘 자동 감지)</li>
 *   <li>{@code parseClaimsJws()} → {@code parseSignedClaims()}</li>
 *   <li>{@code getBody()} → {@code getPayload()}</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see JwtProperties
 * @see AuthenticationException
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** JWT 설정값 (secret, expiration, issuer 등) */
    private final JwtProperties jwtProperties;

    /**
     * HMAC-SHA256 서명/검증에 사용하는 대칭 키
     *
     * <p>{@code @PostConstruct}에서 한 번만 초기화되며,
     * 이후 모든 토큰 생성/검증에 재사용됩니다.</p>
     */
    private SecretKey secretKey;

    /**
     * SecretKey 초기화 (Bean 생성 직후 1회 실행)
     *
     * <p>{@link JwtProperties#getSecret()}에서 문자열을 가져와
     * HMAC-SHA 알고리즘에 적합한 {@link SecretKey}로 변환합니다.</p>
     *
     * <p>{@code Keys.hmacShaKeyFor()}는 바이트 배열 길이가
     * 256비트(32바이트) 미만이면 {@link io.jsonwebtoken.security.WeakKeyException}을 던집니다.</p>
     *
     * <h4>왜 @PostConstruct인가?</h4>
     * <ul>
     *   <li>생성자에서 초기화하면 {@code jwtProperties}가 아직 주입 전일 수 있음</li>
     *   <li>{@code @PostConstruct}는 의존성 주입 완료 후 호출이 보장됨</li>
     *   <li>서버 시작 시 한 번만 실행 → 매 요청마다 키를 생성하지 않아 성능 최적화</li>
     * </ul>
     */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    // ===== 토큰 생성 =====

    /**
     * AccessToken 생성
     *
     * <p>사용자 인증 정보(ID, 이메일, 역할)를 Claims에 담아 JWT를 생성합니다.
     * 이 토큰은 API 요청 시 {@code Authorization: Bearer {token}} 헤더에 포함됩니다.</p>
     *
     * <h4>Claims 구성</h4>
     * <ul>
     *   <li>{@code sub} — 사용자 UUID (토큰의 주체)</li>
     *   <li>{@code email} — 사용자 이메일 (로그 등 식별용)</li>
     *   <li>{@code role} — 사용자 역할 (JwtAuthenticationFilter에서 권한 부여에 사용)</li>
     *   <li>{@code iss} — 발급자 (lucr-api)</li>
     *   <li>{@code iat} — 발급 시각</li>
     *   <li>{@code exp} — 만료 시각 (현재 시각 + accessTokenExpiration)</li>
     *   <li>{@code jti} — 토큰 고유 ID (랜덤 UUID, 토큰 중복 방지)</li>
     * </ul>
     *
     * <h4>왜 email과 role을 Claims에 포함하는가?</h4>
     * <p>JwtAuthenticationFilter에서 토큰만으로 사용자 정보와 권한을 파악할 수 있어,
     * 매 요청마다 DB를 조회하지 않아도 됩니다 (Stateless 인증의 핵심).</p>
     *
     * @param userId 사용자 UUID ({@code User.getId()})
     * @param email  사용자 이메일 ({@code User.getEmail()})
     * @param role   사용자 역할 문자열 ({@code UserRole.name()} → "USER" 또는 "ADMIN")
     * @return 서명된 JWT 문자열 (Header.Payload.Signature 형식)
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())          // sub: 사용자 ID (UUID → 문자열)
                .claim("email", email)               // 커스텀 클레임: 이메일
                .claim("role", role)                  // 커스텀 클레임: 역할 (RBAC에 사용)
                .issuer(jwtProperties.getIssuer())   // iss: 발급자 (lucr-api)
                .issuedAt(now)                        // iat: 발급 시간
                .expiration(expiration)               // exp: 만료 시간
                .id(UUID.randomUUID().toString())     // jti: 토큰 고유 ID (재사용 공격 방지)
                .signWith(secretKey)                  // 서명 (HS256 자동 적용, 키 길이 기반)
                .compact();                           // Header.Payload.Signature 형태로 직렬화
    }

    /**
     * RefreshToken 생성
     *
     * <p>AccessToken 갱신 전용 장기 토큰입니다.
     * AccessToken과 달리 민감 정보(email, role)를 포함하지 않습니다.</p>
     *
     * <h4>왜 RefreshToken에는 email/role이 없는가?</h4>
     * <ul>
     *   <li>RefreshToken은 수명이 길어(7일) 탈취 시 위험도가 높음</li>
     *   <li>최소한의 정보(사용자 ID)만 포함하여 노출 피해를 최소화</li>
     *   <li>{@code type: "refresh"} 클레임으로 AccessToken과 구분</li>
     * </ul>
     *
     * <h4>저장 위치</h4>
     * <p>생성된 RefreshToken은 {@code RefreshToken} 엔티티로 DB에 저장되며,
     * 갱신/로그아웃 시 DB 조회를 통해 유효성을 검증합니다.</p>
     *
     * @param userId 사용자 UUID ({@code User.getId()})
     * @return 서명된 JWT 문자열
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())           // sub: 사용자 ID
                .claim("type", "refresh")             // 커스텀 클레임: 토큰 종류 구분
                .issuer(jwtProperties.getIssuer())    // iss: 발급자
                .issuedAt(now)                         // iat: 발급 시간
                .expiration(expiration)                // exp: 만료 시간 (7일)
                .id(UUID.randomUUID().toString())      // jti: 토큰 고유 ID
                .signWith(secretKey)                   // 서명
                .compact();
    }

    // ===== 토큰 검증 =====

    /**
     * JWT 토큰 유효성 검증
     *
     * <p>JJWT의 {@code parseSignedClaims()}가 다음 항목을 한 번에 검증합니다:</p>
     * <ol>
     *   <li><b>서명 검증</b> — secretKey로 서명을 재계산하여 토큰의 서명과 비교</li>
     *   <li><b>만료 검증</b> — exp 클레임이 현재 시각 이전이면 ExpiredJwtException</li>
     *   <li><b>형식 검증</b> — Header.Payload.Signature 3파트 구조 확인</li>
     *   <li><b>발급자 검증</b> — {@code requireIssuer()}로 iss 클레임 일치 확인</li>
     * </ol>
     *
     * <h4>예외별 의미</h4>
     * <ul>
     *   <li>{@link ExpiredJwtException} — 토큰 만료 (클라이언트는 RefreshToken으로 갱신 필요)</li>
     *   <li>{@link SecurityException} / {@link MalformedJwtException} — 서명 위변조 또는 형식 오류</li>
     *   <li>{@link UnsupportedJwtException} — 지원하지 않는 JWT 형식 (예: 암호화된 JWE)</li>
     *   <li>{@link IllegalArgumentException} — 토큰 문자열이 null이거나 비어있음</li>
     * </ul>
     *
     * <h4>사용처</h4>
     * <p>{@code JwtAuthenticationFilter.doFilterInternal()}에서 매 요청마다 호출됩니다.
     * 검증 실패 시 {@code false}를 반환하고, 필터는 인증을 설정하지 않습니다.</p>
     *
     * @param token JWT 문자열 (Bearer 접두사 제거된 순수 토큰)
     * @return {@code true} — 유효한 토큰, {@code false} — 무효한 토큰
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)                          // 서명 검증에 사용할 키 설정
                    .requireIssuer(jwtProperties.getIssuer())      // iss 클레임 값이 일치하는지 검증
                    .build()
                    .parseSignedClaims(token);                     // 파싱 + 서명 + 만료 + iss 한 번에 검증
            return true;
        } catch (ExpiredJwtException e) {
            // 토큰 만료 — 클라이언트는 /api/v1/auth/refresh로 갱신 요청
            log.warn("JWT 토큰 만료: {}", e.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // 서명 불일치 또는 토큰 형식 오류 — 위변조 의심
            log.warn("JWT 토큰 서명/형식 오류: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 JWT 형식 (예: JWE 암호화 토큰)
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // 토큰 문자열이 null 또는 빈 문자열
            log.warn("JWT claims 문자열이 비어있음: {}", e.getMessage());
        }
        return false;
    }

    // ===== Claims 추출 (public) =====

    /**
     * 토큰에서 사용자 ID 추출 (sub 클레임)
     *
     * <p>토큰의 subject (sub) 클레임에 저장된 UUID를 파싱하여 반환합니다.
     * 이 메서드는 {@link #validateToken(String)}으로 유효성이 확인된 토큰에 대해 호출해야 합니다.</p>
     *
     * @param token 유효성 검증 완료된 JWT 문자열
     * @return 사용자 UUID
     * @throws AuthenticationException 토큰 파싱 실패 시 (INVALID_TOKEN)
     */
    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    /**
     * 토큰에서 이메일 추출 (email 커스텀 클레임)
     *
     * <p>AccessToken에만 포함됩니다. RefreshToken에서 호출하면 {@code null}을 반환합니다.</p>
     *
     * @param token 유효성 검증 완료된 JWT 문자열
     * @return 사용자 이메일, 또는 {@code null} (RefreshToken인 경우)
     * @throws AuthenticationException 토큰 파싱 실패 시 (INVALID_TOKEN)
     */
    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * 토큰에서 역할 추출 (role 커스텀 클레임)
     *
     * <p>AccessToken에만 포함됩니다. JwtAuthenticationFilter에서
     * {@code SimpleGrantedAuthority("ROLE_" + role)}로 변환하여 Spring Security 인가에 사용합니다.</p>
     *
     * @param token 유효성 검증 완료된 JWT 문자열
     * @return 역할 문자열 ("USER" 또는 "ADMIN"), 또는 {@code null} (RefreshToken인 경우)
     * @throws AuthenticationException 토큰 파싱 실패 시 (INVALID_TOKEN)
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * AccessToken 만료 시간을 초(seconds) 단위로 반환
     *
     * <p>TokenResponse에 포함하여 클라이언트가 토큰 갱신 타이밍을 계산할 수 있게 합니다.</p>
     *
     * @return 만료 시간 (초 단위, 예: 1800 = 30분)
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpiration() / 1000;
    }

    // ===== Private 헬퍼 메서드 =====

    /**
     * 토큰에서 Claims(페이로드) 추출
     *
     * <p>{@code parseSignedClaims()}가 내부적으로 서명 + 만료를 검증하므로,
     * 이 메서드 호출 전에 {@link #validateToken(String)}으로 유효성을 확인하는 것이 권장됩니다.</p>
     *
     * <p>만약 토큰이 유효하지 않으면 {@link JwtException}이 발생하고,
     * 이를 {@link AuthenticationException#invalidToken()}으로 변환하여 던집니다.</p>
     *
     * <h4>호출 체인</h4>
     * <pre>
     * getUserId(token)  ─┐
     * getEmail(token)   ─┤── getClaims(token) → Jwts.parser()...parseSignedClaims()
     * getRole(token)    ─┘                         ↓
     *                                          Claims 객체 반환 (또는 AuthenticationException)
     * </pre>
     *
     * @param token JWT 문자열
     * @return 파싱된 Claims 객체 (sub, email, role 등 모든 클레임 포함)
     * @throws AuthenticationException 토큰이 유효하지 않을 때 (INVALID_TOKEN)
     */
    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)     // 서명 검증 키 설정
                    .build()
                    .parseSignedClaims(token)  // 파싱 + 서명 검증 + 만료 검증
                    .getPayload();             // Claims 객체 반환 (0.12.x: getPayload(), 구버전: getBody())
        } catch (JwtException e) {
            // 모든 JWT 예외를 AuthenticationException으로 통합 변환
            throw AuthenticationException.invalidToken();
        }
    }
}
