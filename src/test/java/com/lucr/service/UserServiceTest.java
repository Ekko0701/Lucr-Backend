package com.lucr.service;

import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.dto.response.UserResponse;
import com.lucr.entity.User;
import com.lucr.entity.UserRole;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.UserMapper;
import com.lucr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * UserService 비즈니스 로직 테스트
 *
 * Mock 기반 단위 테스트:
 * - Repository, Mapper, PasswordEncoder를 Mock으로 대체
 * - 비즈니스 로직과 예외 처리 집중 테스트
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private User savedUser;
    private UserDetailResponse userDetailResponse;
    private UserResponse userResponse;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .name("테스트 사용자")
                .build();

        savedUser = User.builder()
                .id(testId)
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .name("테스트 사용자")
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userDetailResponse = UserDetailResponse.builder()
                .id(testId)
                .email("test@example.com")
                .name("테스트 사용자")
                .role("USER")
                .isActive(true)
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();

        userResponse = UserResponse.builder()
                .id(testId)
                .email("test@example.com")
                .name("테스트 사용자")
                .role("USER")
                .isActive(true)
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // ========== register ==========

    @Nested
    @DisplayName("register() - 회원가입")
    class RegisterTests {

        @Test
        @DisplayName("성공 - 정상 회원가입")
        void register_Success() {
            // given
            given(userRepository.existsByEmail("test@example.com")).willReturn(false);
            given(userMapper.toEntity(registerRequest)).willReturn(
                    User.builder().email("test@example.com").name("테스트 사용자").build()
            );
            given(passwordEncoder.encode("Test@1234")).willReturn("$2a$10$encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDetailResponse(savedUser)).willReturn(userDetailResponse);

            // when
            UserDetailResponse result = userService.register(registerRequest);

            // then
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getName()).isEqualTo("테스트 사용자");
            assertThat(result.getRole()).isEqualTo("USER");
            assertThat(result.getIsActive()).isTrue();

            then(userRepository).should(times(1)).existsByEmail("test@example.com");
            then(passwordEncoder).should(times(1)).encode("Test@1234");
            then(userRepository).should(times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("실패 - 이메일 중복 시 DuplicateResourceException")
        void register_DuplicateEmail_ThrowsException() {
            // given
            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.register(registerRequest))
                    .isInstanceOf(DuplicateResourceException.class);

            then(userRepository).should(times(1)).existsByEmail("test@example.com");
            then(userRepository).should(never()).save(any(User.class));
            then(passwordEncoder).should(never()).encode(anyString());
        }
    }

    // ========== getUserById ==========

    @Nested
    @DisplayName("getUserById() - ID로 조회")
    class GetUserByIdTests {

        @Test
        @DisplayName("성공 - 존재하는 ID")
        void getUserById_Success() {
            // given
            given(userRepository.findById(testId)).willReturn(Optional.of(savedUser));
            given(userMapper.toDetailResponse(savedUser)).willReturn(userDetailResponse);

            // when
            UserDetailResponse result = userService.getUserById(testId);

            // then
            assertThat(result.getId()).isEqualTo(testId);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID → ResourceNotFoundException")
        void getUserById_NotFound_ThrowsException() {
            // given
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== getUserByEmail ==========

    @Nested
    @DisplayName("getUserByEmail() - 이메일로 조회")
    class GetUserByEmailTests {

        @Test
        @DisplayName("성공 - 존재하는 이메일")
        void getUserByEmail_Success() {
            // given
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(savedUser));
            given(userMapper.toDetailResponse(savedUser)).willReturn(userDetailResponse);

            // when
            UserDetailResponse result = userService.getUserByEmail("test@example.com");

            // then
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일 → ResourceNotFoundException")
        void getUserByEmail_NotFound_ThrowsException() {
            // given
            given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserByEmail("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== getAllUsers ==========

    @Nested
    @DisplayName("getAllUsers() - 전체 목록 조회")
    class GetAllUsersTests {

        @Test
        @DisplayName("성공 - 페이징 결과 반환")
        void getAllUsers_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(savedUser), pageable, 1);

            given(userRepository.findAll(pageable)).willReturn(userPage);
            given(userMapper.toResponse(savedUser)).willReturn(userResponse);

            // when
            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("빈 결과 - 사용자 없음")
        void getAllUsers_Empty() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(userRepository.findAll(pageable)).willReturn(emptyPage);

            // when
            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
        }
    }

    // ========== existsByEmail ==========

    @Nested
    @DisplayName("existsByEmail() - 이메일 중복 확인")
    class ExistsByEmailTests {

        @Test
        @DisplayName("존재하는 이메일 - true")
        void existsByEmail_Exists_ReturnsTrue() {
            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            assertThat(userService.existsByEmail("test@example.com")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일 - false")
        void existsByEmail_NotExists_ReturnsFalse() {
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);

            assertThat(userService.existsByEmail("new@example.com")).isFalse();
        }
    }

    // ========== deactivateUser ==========

    @Nested
    @DisplayName("deactivateUser() - 계정 비활성화")
    class DeactivateUserTests {

        @Test
        @DisplayName("성공 - isActive false로 변경")
        void deactivateUser_Success() {
            // given
            given(userRepository.findById(testId)).willReturn(Optional.of(savedUser));

            // when
            userService.deactivateUser(testId);

            // then
            assertThat(savedUser.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID → ResourceNotFoundException")
        void deactivateUser_NotFound_ThrowsException() {
            // given
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deactivateUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== activateUser ==========

    @Nested
    @DisplayName("activateUser() - 계정 활성화")
    class ActivateUserTests {

        @Test
        @DisplayName("성공 - isActive true로 변경")
        void activateUser_Success() {
            // given
            savedUser.deactivate();  // 먼저 비활성화
            given(userRepository.findById(testId)).willReturn(Optional.of(savedUser));

            // when
            userService.activateUser(testId);

            // then
            assertThat(savedUser.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID → ResourceNotFoundException")
        void activateUser_NotFound_ThrowsException() {
            // given
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.activateUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
