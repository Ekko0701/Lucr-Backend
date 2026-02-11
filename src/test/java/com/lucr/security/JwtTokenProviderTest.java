package com.lucr.security;

import com.lucr.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 단위 테스트
 *
 * <p>JWT 토큰의 생성, 검증, 파싱, 만료 처리를 검증합니다.</p>
 * <p>Spring Context 없이 순수 단위 테스트로 작성합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    // 테스트용 설정값
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-characters-long-for-hmac-sha256";
    private static final long ACCESS_TOKEN_EXPIRATION = 1_800_000L;    // 30분
    private static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L; // 7일
    private static final String TEST_ISSUER = "lucr-api-test";

    // 테스트용 사용자 데이터
    private UUID testUserId;
    private String testEmail;
    private String testRole;

    @BeforeEach
    void setUp() {
        // JwtProperties 수동 생성 (Spring Context 없이)
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setAccessTokenExpiration(ACCESS_TOKEN_EXPIRATION);
        jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION);
        jwtProperties.setIssuer(TEST_ISSUER);

        // JwtTokenProvider 생성 + 초기화
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init(); // @PostConstruct 수동 호출

        // 테스트 사용자 데이터
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testRole = "USER";
    }

    // ========== AccessToken 생성 및 검증 ==========

    @Nested
    @DisplayName("AccessToken 생성/검증")
    class AccessTokenTests {

        @Test
        @DisplayName("AccessToken 생성 → 검증 성공")
        void generateAccessToken_thenValidate_success() {
            // when
            String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

            // then
            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("AccessToken에서 userId 추출")
        void generateAccessToken_thenGetUserId_success() {
            // given
            String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

            // when
            UUID extractedUserId = jwtTokenProvider.getUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(testUserId);
        }

        @Test
        @DisplayName("AccessToken에서 email 추출")
        void generateAccessToken_thenGetEmail_success() {
            // given
            String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

            // when
            String extractedEmail = jwtTokenProvider.getEmail(token);

            // then
            assertThat(extractedEmail).isEqualTo(testEmail);
        }

        @Test
        @DisplayName("AccessToken에서 role 추출")
        void generateAccessToken_thenGetRole_success() {
            // given
            String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

            // when
            String extractedRole = jwtTokenProvider.getRole(token);

            // then
            assertThat(extractedRole).isEqualTo(testRole);
        }
    }

    // ========== RefreshToken 생성 및 검증 ==========

    @Nested
    @DisplayName("RefreshToken 생성/검증")
    class RefreshTokenTests {

        @Test
        @DisplayName("RefreshToken 생성 → 검증 성공")
        void generateRefreshToken_thenValidate_success() {
            // when
            String token = jwtTokenProvider.generateRefreshToken(testUserId);

            // then
            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("RefreshToken에서 userId 추출")
        void generateRefreshToken_thenGetUserId_success() {
            // given
            String token = jwtTokenProvider.generateRefreshToken(testUserId);

            // when
            UUID extractedUserId = jwtTokenProvider.getUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(testUserId);
        }
    }

    // ========== 토큰 만료 ==========

    @Nested
    @DisplayName("토큰 만료 처리")
    class TokenExpirationTests {

        @Test
        @DisplayName("만료된 토큰 → validateToken() false")
        void expiredToken_validateToken_returnsFalse() {
            // given — 만료 시간을 0ms로 설정하여 즉시 만료되는 토큰 생성
            JwtProperties expiredProperties = new JwtProperties();
            expiredProperties.setSecret(TEST_SECRET);
            expiredProperties.setAccessTokenExpiration(0L); // 즉시 만료
            expiredProperties.setRefreshTokenExpiration(0L);
            expiredProperties.setIssuer(TEST_ISSUER);

            JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
            expiredProvider.init();

            String token = expiredProvider.generateAccessToken(testUserId, testEmail, testRole);

            // when & then — 토큰이 이미 만료됨
            assertThat(expiredProvider.validateToken(token)).isFalse();
        }
    }

    // ========== 유효하지 않은 토큰 ==========

    @Nested
    @DisplayName("유효하지 않은 토큰 처리")
    class InvalidTokenTests {

        @Test
        @DisplayName("조작된 토큰 → validateToken() false")
        void tamperedToken_validateToken_returnsFalse() {
            // given — 정상 토큰을 조작
            String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);
            String tamperedToken = token + "tampered";

            // when & then
            assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("완전히 잘못된 문자열 → validateToken() false")
        void randomString_validateToken_returnsFalse() {
            // when & then
            assertThat(jwtTokenProvider.validateToken("not.a.jwt.token")).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 → validateToken() false")
        void emptyString_validateToken_returnsFalse() {
            // when & then
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("다른 서명키로 생성된 토큰 → validateToken() false")
        void differentKey_validateToken_returnsFalse() {
            // given — 다른 비밀키로 토큰 생성
            String differentSecret = "another-secret-key-that-is-at-least-32-characters-long";
            SecretKey differentKey = Keys.hmacShaKeyFor(
                    Base64.getEncoder().encode(differentSecret.getBytes(StandardCharsets.UTF_8))
            );

            String tokenWithDifferentKey = Jwts.builder()
                    .subject(testUserId.toString())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                    .signWith(differentKey)
                    .compact();

            // when & then — 서명 불일치
            assertThat(jwtTokenProvider.validateToken(tokenWithDifferentKey)).isFalse();
        }
    }

    // ========== 만료 시간 ==========

    @Nested
    @DisplayName("만료 시간 조회")
    class ExpirationTimeTests {

        @Test
        @DisplayName("AccessToken 만료 시간 (초) 조회")
        void getAccessTokenExpirationSeconds_returnsCorrectValue() {
            // when
            long expirationSeconds = jwtTokenProvider.getAccessTokenExpirationSeconds();

            // then — 1,800,000ms = 1800초
            assertThat(expirationSeconds).isEqualTo(1800L);
        }
    }
}
