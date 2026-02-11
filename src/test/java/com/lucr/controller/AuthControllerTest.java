package com.lucr.controller;

import tools.jackson.databind.ObjectMapper;
import com.lucr.dto.request.LoginRequest;
import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.request.TokenRefreshRequest;
import com.lucr.dto.response.TokenResponse;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.exception.AuthenticationException;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.service.AuthService;
import com.lucr.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 단위 테스트
 *
 * <p>회원가입, 이메일 중복 확인, 로그인, 토큰 갱신, 로그아웃 API 테스트</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    // Security 의존성 Mock (SecurityConfig가 주입받는 빈)
    @MockitoBean
    private com.lucr.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.lucr.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private com.lucr.security.JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private UserDetailResponse userDetailResponse;
    private LoginRequest validLoginRequest;
    private TokenRefreshRequest validRefreshRequest;
    private TokenResponse tokenResponse;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .name("테스트 사용자")
                .build();

        userDetailResponse = UserDetailResponse.builder()
                .id(testId)
                .email("test@example.com")
                .name("테스트 사용자")
                .role("USER")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .build();

        validRefreshRequest = TokenRefreshRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        tokenResponse = TokenResponse.of(
                "access-token-value",
                "refresh-token-value",
                1800
        );
    }

    // ========== POST /api/v1/auth/register ==========

    @Nested
    @DisplayName("POST /api/v1/auth/register - 회원가입")
    class RegisterTests {

        @Test
        @DisplayName("성공 - 201 Created + 사용자 정보 반환")
        void register_Success() throws Exception {
            // given
            given(userService.register(any(RegisterRequest.class))).willReturn(userDetailResponse);

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRegisterRequest))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(testId.toString()))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.name").value("테스트 사용자"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.isActive").value(true))
                    .andExpect(jsonPath("$.timestamp").exists());

            then(userService).should(times(1)).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("실패 - 이메일 중복 (409 Conflict)")
        void register_DuplicateEmail_Conflict() throws Exception {
            // given
            given(userService.register(any(RegisterRequest.class)))
                    .willThrow(DuplicateResourceException.duplicateEmail("test@example.com"));

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRegisterRequest))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("E409004"))
                    .andExpect(jsonPath("$.message").exists());

            then(userService).should(times(1)).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("실패 - 이메일 누락 (400 Bad Request)")
        void register_MissingEmail_BadRequest() throws Exception {
            // given
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .password("Test@1234")
                    .name("테스트 사용자")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(userService).should(never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("실패 - 비밀번호 누락 (400 Bad Request)")
        void register_MissingPassword_BadRequest() throws Exception {
            // given
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(userService).should(never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("실패 - 이름 누락 (400 Bad Request)")
        void register_MissingName_BadRequest() throws Exception {
            // given
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("Test@1234")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(userService).should(never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("실패 - 잘못된 이메일 형식 (400 Bad Request)")
        void register_InvalidEmailFormat_BadRequest() throws Exception {
            // given
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("invalid-email")
                    .password("Test@1234")
                    .name("테스트 사용자")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(userService).should(never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("실패 - 빈 요청 본문 (400 Bad Request)")
        void register_EmptyBody_BadRequest() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}")
                    )
                    .andExpect(status().isBadRequest());

            then(userService).should(never()).register(any(RegisterRequest.class));
        }
    }

    // ========== GET /api/v1/auth/check-email ==========

    @Nested
    @DisplayName("GET /api/v1/auth/check-email - 이메일 중복 확인")
    class CheckEmailTests {

        @Test
        @DisplayName("이메일 존재 - true 반환")
        void checkEmail_Exists_ReturnsTrue() throws Exception {
            // given
            given(userService.existsByEmail("test@example.com")).willReturn(true);

            // when & then
            mockMvc.perform(
                            get("/api/v1/auth/check-email")
                                    .param("email", "test@example.com")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                    .andExpect(jsonPath("$.data").value(true));

            then(userService).should(times(1)).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("이메일 미존재 - false 반환")
        void checkEmail_NotExists_ReturnsFalse() throws Exception {
            // given
            given(userService.existsByEmail("new@example.com")).willReturn(false);

            // when & then
            mockMvc.perform(
                            get("/api/v1/auth/check-email")
                                    .param("email", "new@example.com")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다."))
                    .andExpect(jsonPath("$.data").value(false));

            then(userService).should(times(1)).existsByEmail("new@example.com");
        }

        @Test
        @DisplayName("실패 - email 파라미터 누락 (400 Bad Request)")
        void checkEmail_MissingParam_BadRequest() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/auth/check-email"))
                    .andExpect(status().isBadRequest());

            then(userService).should(never()).existsByEmail(anyString());
        }
    }

    // ========== POST /api/v1/auth/login ==========

    @Nested
    @DisplayName("POST /api/v1/auth/login - 로그인")
    class LoginTests {

        @Test
        @DisplayName("성공 - 200 OK + 토큰 반환")
        void login_Success() throws Exception {
            // given
            given(authService.login(any(LoginRequest.class))).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validLoginRequest))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("로그인이 성공적으로 완료되었습니다."))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-value"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-value"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(1800));

            then(authService).should(times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("실패 - 이메일 누락 (400 Bad Request)")
        void login_MissingEmail_BadRequest() throws Exception {
            // given
            LoginRequest invalidRequest = LoginRequest.builder()
                    .password("Test@1234")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(authService).should(never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치 (401 Unauthorized)")
        void login_InvalidPassword_Unauthorized() throws Exception {
            // given
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(AuthenticationException.invalidPassword());

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validLoginRequest))
                    )
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("E401001"))
                    .andExpect(jsonPath("$.message").exists());

            then(authService).should(times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 (404 Not Found)")
        void login_UserNotFound() throws Exception {
            // given
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validLoginRequest))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());

            then(authService).should(times(1)).login(any(LoginRequest.class));
        }
    }

    // ========== POST /api/v1/auth/refresh ==========

    @Nested
    @DisplayName("POST /api/v1/auth/refresh - 토큰 갱신")
    class RefreshTests {

        @Test
        @DisplayName("성공 - 200 OK + 새 AccessToken 반환")
        void refresh_Success() throws Exception {
            // given
            TokenResponse refreshResponse = TokenResponse.ofAccessToken("new-access-token", 1800);
            given(authService.refresh(any(TokenRefreshRequest.class))).willReturn(refreshResponse);

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRefreshRequest))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("토큰이 성공적으로 갱신되었습니다."))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.expiresIn").value(1800));

            then(authService).should(times(1)).refresh(any(TokenRefreshRequest.class));
        }

        @Test
        @DisplayName("실패 - RefreshToken 누락 (400 Bad Request)")
        void refresh_MissingToken_BadRequest() throws Exception {
            // given
            TokenRefreshRequest invalidRequest = TokenRefreshRequest.builder().build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(authService).should(never()).refresh(any(TokenRefreshRequest.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 RefreshToken (404 Not Found)")
        void refresh_TokenNotFound() throws Exception {
            // given
            given(authService.refresh(any(TokenRefreshRequest.class)))
                    .willThrow(new ResourceNotFoundException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRefreshRequest))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());

            then(authService).should(times(1)).refresh(any(TokenRefreshRequest.class));
        }

        @Test
        @DisplayName("실패 - 만료된 RefreshToken (401 Unauthorized)")
        void refresh_ExpiredToken() throws Exception {
            // given
            given(authService.refresh(any(TokenRefreshRequest.class)))
                    .willThrow(AuthenticationException.expiredToken());

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRefreshRequest))
                    )
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("E401002"))
                    .andExpect(jsonPath("$.message").exists());

            then(authService).should(times(1)).refresh(any(TokenRefreshRequest.class));
        }
    }

    // ========== POST /api/v1/auth/logout ==========

    @Nested
    @DisplayName("POST /api/v1/auth/logout - 로그아웃")
    class LogoutTests {

        @Test
        @DisplayName("성공 - 200 OK")
        void logout_Success() throws Exception {
            // given — JwtAuthenticationFilter가 Bearer 토큰을 검증하여 SecurityContext를 설정하도록 구성
            // STATELESS 세션 정책에서는 authentication() post-processor가 동작하지 않으므로,
            // 실제 필터 흐름과 동일하게 Authorization 헤더에 토큰을 전달하고
            // mock된 JwtTokenProvider가 해당 토큰을 검증/파싱하도록 설정
            UUID userId = UUID.randomUUID();
            String fakeToken = "fake-jwt-token-for-test";

            given(jwtTokenProvider.validateToken(fakeToken)).willReturn(true);
            given(jwtTokenProvider.getUserId(fakeToken)).willReturn(userId);
            given(jwtTokenProvider.getEmail(fakeToken)).willReturn("test@example.com");
            given(jwtTokenProvider.getRole(fakeToken)).willReturn("USER");
            willDoNothing().given(authService).logout(any(UUID.class));

            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + fakeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("로그아웃이 성공적으로 완료되었습니다."));

            then(authService).should(times(1)).logout(userId);
        }
    }
}
