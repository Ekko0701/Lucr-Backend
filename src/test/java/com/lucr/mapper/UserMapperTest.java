package com.lucr.mapper;

import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.dto.response.UserResponse;
import com.lucr.entity.User;
import com.lucr.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * UserMapper 단위 테스트
 *
 * - RegisterRequest → User Entity 변환
 * - User Entity → UserResponse 변환
 * - User Entity → UserDetailResponse 변환
 * - password가 매퍼에서 설정되지 않는지 검증
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@DisplayName("UserMapper 테스트")
class UserMapperTest {

    private UserMapper userMapper;
    private User testUser;
    private RegisterRequest testRegisterRequest;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("$2a$10$hashedPassword")
                .name("테스트 사용자")
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 2, 11, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 2, 11, 10, 0))
                .build();

        testRegisterRequest = RegisterRequest.builder()
                .email("new@example.com")
                .password("Test@1234")
                .name("새 사용자")
                .build();
    }

    // ========== toEntity() 테스트 ==========

    @Nested
    @DisplayName("toEntity() - RegisterRequest → User Entity")
    class ToEntityTests {

        @Test
        @DisplayName("정상 변환 - email, name 매핑")
        void toEntity_Success() {
            // when
            User entity = userMapper.toEntity(testRegisterRequest);

            // then
            assertThat(entity.getEmail()).isEqualTo("new@example.com");
            assertThat(entity.getName()).isEqualTo("새 사용자");
        }

        @Test
        @DisplayName("password는 매퍼에서 설정하지 않음 (null)")
        void toEntity_PasswordIsNull() {
            // when
            User entity = userMapper.toEntity(testRegisterRequest);

            // then - password는 Service에서 BCrypt 해싱 후 별도 설정
            assertThat(entity.getPassword()).isNull();
        }

        @Test
        @DisplayName("role 기본값은 USER (Builder.Default)")
        void toEntity_DefaultRoleIsUser() {
            // when
            User entity = userMapper.toEntity(testRegisterRequest);

            // then
            assertThat(entity.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("isActive 기본값은 true (Builder.Default)")
        void toEntity_DefaultIsActiveTrue() {
            // when
            User entity = userMapper.toEntity(testRegisterRequest);

            // then
            assertThat(entity.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("id는 null (JPA가 save 시 생성)")
        void toEntity_IdIsNull() {
            // when
            User entity = userMapper.toEntity(testRegisterRequest);

            // then
            assertThat(entity.getId()).isNull();
        }
    }

    // ========== toResponse() 테스트 ==========

    @Nested
    @DisplayName("toResponse() - User Entity → UserResponse")
    class ToResponseTests {

        @Test
        @DisplayName("정상 변환 - 모든 필드 매핑")
        void toResponse_AllFields_Success() {
            // when
            UserResponse response = userMapper.toResponse(testUser);

            // then
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getName()).isEqualTo("테스트 사용자");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getIsActive()).isTrue();
            assertThat(response.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
        }

        @Test
        @DisplayName("role은 enum.name() 문자열로 변환")
        void toResponse_RoleAsString() {
            // given
            User admin = User.builder()
                    .id(UUID.randomUUID())
                    .email("admin@example.com")
                    .password("password")
                    .name("관리자")
                    .role(UserRole.ADMIN)
                    .build();

            // when
            UserResponse response = userMapper.toResponse(admin);

            // then
            assertThat(response.getRole()).isEqualTo("ADMIN");
        }
    }

    // ========== toDetailResponse() 테스트 ==========

    @Nested
    @DisplayName("toDetailResponse() - User Entity → UserDetailResponse")
    class ToDetailResponseTests {

        @Test
        @DisplayName("정상 변환 - 모든 필드 매핑 (updatedAt 포함)")
        void toDetailResponse_AllFields_Success() {
            // when
            UserDetailResponse response = userMapper.toDetailResponse(testUser);

            // then
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getName()).isEqualTo("테스트 사용자");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getIsActive()).isTrue();
            assertThat(response.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testUser.getUpdatedAt());
        }

        @Test
        @DisplayName("toResponse와 toDetailResponse의 차이 - updatedAt 포함 여부")
        void toDetailResponse_HasUpdatedAt_UnlikeToResponse() {
            // when
            UserResponse response = userMapper.toResponse(testUser);
            UserDetailResponse detail = userMapper.toDetailResponse(testUser);

            // then - 공통 필드는 동일
            assertThat(response.getId()).isEqualTo(detail.getId());
            assertThat(response.getEmail()).isEqualTo(detail.getEmail());

            // detail에만 updatedAt 존재
            assertThat(detail.getUpdatedAt()).isNotNull();
        }
    }
}
