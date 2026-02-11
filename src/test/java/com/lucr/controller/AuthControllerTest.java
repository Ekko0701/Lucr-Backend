package com.lucr.controller;

import tools.jackson.databind.ObjectMapper;
import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 단위 테스트
 *
 * @WebMvcTest: Controller 레이어만 로드 (가벼운 테스트)
 * MockMvc: HTTP 요청/응답 시뮬레이션
 * @MockitoBean: UserService를 Mock으로 대체
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

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRequest;
    private UserDetailResponse userDetailResponse;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        validRequest = RegisterRequest.builder()
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
                                    .content(objectMapper.writeValueAsString(validRequest))
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
                                    .content(objectMapper.writeValueAsString(validRequest))
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
}
