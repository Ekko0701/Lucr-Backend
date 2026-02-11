package com.lucr.service;

import com.lucr.config.JwtProperties;
import com.lucr.dto.request.LoginRequest;
import com.lucr.dto.request.TokenRefreshRequest;
import com.lucr.dto.response.TokenResponse;
import com.lucr.entity.RefreshToken;
import com.lucr.entity.User;
import com.lucr.entity.UserRole;
import com.lucr.exception.AuthenticationException;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.repository.RefreshTokenRepository;
import com.lucr.repository.UserRepository;
import com.lucr.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

/**
 * AuthServiceImpl 단위 테스트
 *
 * <p>Mockito를 사용하여 의존성을 Mock으로 대체하고,
 * 로그인/토큰 갱신/로그아웃 비즈니스 로직을 검증합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    // 테스트 데이터
    private User testUser;
    private UUID testUserId;
    private String testEmail;
    private String testPassword;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testPassword = "Test@1234";
        encodedPassword = "$2a$10$encodedPasswordHash";

        testUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .password(encodedPassword)
                .name("테스트 사용자")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    // ========== 로그인 테스트 ==========

    @Nested
    @DisplayName("login() — 로그인")
    class LoginTests {

        @Test
        @DisplayName("성공 — TokenResponse 반환 (accessToken, refreshToken 존재)")
        void login_success_returnsTokenResponse() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .email(testEmail)
                    .password(testPassword)
                    .build();

            given(userRepository.findByEmailAndIsActive(testEmail, true))
                    .willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(testPassword, encodedPassword))
                    .willReturn(true);
            given(jwtTokenProvider.generateAccessToken(testUserId, testEmail, "USER"))
                    .willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(testUserId))
                    .willReturn("refresh-token");
            given(jwtTokenProvider.getAccessTokenExpirationSeconds())
                    .willReturn(1800L);
            given(jwtProperties.getRefreshTokenExpiration())
                    .willReturn(604_800_000L);
            willDoNothing().given(refreshTokenRepository).deleteByUserId(testUserId);
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willReturn(RefreshToken.builder().build());

            // when
            TokenResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(1800L);

            // 호출 검증
            then(userRepository).should(times(1)).findByEmailAndIsActive(testEmail, true);
            then(passwordEncoder).should(times(1)).matches(testPassword, encodedPassword);
            then(refreshTokenRepository).should(times(1)).deleteByUserId(testUserId);
            then(refreshTokenRepository).should(times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 이메일 → ResourceNotFoundException")
        void login_userNotFound_throwsResourceNotFoundException() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .email("unknown@example.com")
                    .password(testPassword)
                    .build();

            given(userRepository.findByEmailAndIsActive("unknown@example.com", true))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class);

            // 비밀번호 검증은 호출되지 않아야 함
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패 — 비밀번호 불일치 → AuthenticationException")
        void login_invalidPassword_throwsAuthenticationException() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .email(testEmail)
                    .password("wrong-password")
                    .build();

            given(userRepository.findByEmailAndIsActive(testEmail, true))
                    .willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrong-password", encodedPassword))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class);

            // 토큰 생성은 호출되지 않아야 함
            then(jwtTokenProvider).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패 — 비활성 계정 → ResourceNotFoundException")
        void login_inactiveAccount_throwsResourceNotFoundException() {
            // given — isActive=false인 사용자는 조회되지 않음
            LoginRequest request = LoginRequest.builder()
                    .email(testEmail)
                    .password(testPassword)
                    .build();

            given(userRepository.findByEmailAndIsActive(testEmail, true))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== 토큰 갱신 테스트 ==========

    @Nested
    @DisplayName("refresh() — 토큰 갱신")
    class RefreshTests {

        @Test
        @DisplayName("성공 — 새 AccessToken 반환")
        void refresh_success_returnsNewAccessToken() {
            // given
            String refreshTokenStr = "valid-refresh-token";
            TokenRefreshRequest request = TokenRefreshRequest.builder()
                    .refreshToken(refreshTokenStr)
                    .build();

            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .token(refreshTokenStr)
                    .user(testUser)
                    .expiresAt(LocalDateTime.now().plusDays(7)) // 만료되지 않음
                    .build();

            given(refreshTokenRepository.findByToken(refreshTokenStr))
                    .willReturn(Optional.of(refreshTokenEntity));
            given(jwtTokenProvider.validateToken(refreshTokenStr))
                    .willReturn(true);
            given(jwtTokenProvider.generateAccessToken(testUserId, testEmail, "USER"))
                    .willReturn("new-access-token");
            given(jwtTokenProvider.getAccessTokenExpirationSeconds())
                    .willReturn(1800L);

            // when
            TokenResponse response = authService.refresh(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getExpiresIn()).isEqualTo(1800L);
            // RefreshToken은 갱신하지 않으므로 null
            assertThat(response.getRefreshToken()).isNull();
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 RefreshToken → ResourceNotFoundException")
        void refresh_tokenNotFound_throwsResourceNotFoundException() {
            // given
            TokenRefreshRequest request = TokenRefreshRequest.builder()
                    .refreshToken("non-existent-token")
                    .build();

            given(refreshTokenRepository.findByToken("non-existent-token"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("실패 — 만료된 RefreshToken → AuthenticationException + DB 삭제")
        void refresh_expiredToken_throwsAuthenticationException() {
            // given
            String refreshTokenStr = "expired-refresh-token";
            TokenRefreshRequest request = TokenRefreshRequest.builder()
                    .refreshToken(refreshTokenStr)
                    .build();

            RefreshToken expiredEntity = RefreshToken.builder()
                    .token(refreshTokenStr)
                    .user(testUser)
                    .expiresAt(LocalDateTime.now().minusDays(1)) // 이미 만료
                    .build();

            given(refreshTokenRepository.findByToken(refreshTokenStr))
                    .willReturn(Optional.of(expiredEntity));

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthenticationException.class);

            // 만료된 토큰은 DB에서 삭제됨
            then(refreshTokenRepository).should(times(1)).delete(expiredEntity);
        }
    }

    // ========== 로그아웃 테스트 ==========

    @Nested
    @DisplayName("logout() — 로그아웃")
    class LogoutTests {

        @Test
        @DisplayName("성공 — RefreshToken 삭제")
        void logout_success_deletesRefreshToken() {
            // given
            willDoNothing().given(refreshTokenRepository).deleteByUserId(testUserId);

            // when
            authService.logout(testUserId);

            // then
            then(refreshTokenRepository).should(times(1)).deleteByUserId(testUserId);
        }
    }
}
