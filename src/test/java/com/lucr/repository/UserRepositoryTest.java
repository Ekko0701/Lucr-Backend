package com.lucr.repository;

import com.lucr.entity.User;
import com.lucr.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserRepository 테스트
 *
 * @DataJpaTest:
 * - JPA 관련 컴포넌트만 로드 (경량 테스트)
 * - H2 인메모리 DB 자동 설정
 * - 각 테스트 메서드마다 트랜잭션 롤백 (테스트 격리)
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@DataJpaTest
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User activeUser;
    private User inactiveUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        activeUser = userRepository.save(
                User.builder()
                        .email("active@example.com")
                        .password("$2a$10$hashedPassword1")
                        .name("활성 사용자")
                        .role(UserRole.USER)
                        .isActive(true)
                        .build()
        );

        inactiveUser = userRepository.save(
                User.builder()
                        .email("inactive@example.com")
                        .password("$2a$10$hashedPassword2")
                        .name("비활성 사용자")
                        .role(UserRole.USER)
                        .isActive(false)
                        .build()
        );

        adminUser = userRepository.save(
                User.builder()
                        .email("admin@example.com")
                        .password("$2a$10$hashedPassword3")
                        .name("관리자")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build()
        );
    }

    // ========== findByEmail ==========

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTests {

        @Test
        @DisplayName("존재하는 이메일 - 사용자 반환")
        void findByEmail_Exists_ReturnsUser() {
            // when
            Optional<User> result = userRepository.findByEmail("active@example.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("활성 사용자");
        }

        @Test
        @DisplayName("존재하지 않는 이메일 - empty 반환")
        void findByEmail_NotExists_ReturnsEmpty() {
            // when
            Optional<User> result = userRepository.findByEmail("notexist@example.com");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========== existsByEmail ==========

    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmailTests {

        @Test
        @DisplayName("존재하는 이메일 - true 반환")
        void existsByEmail_Exists_ReturnsTrue() {
            assertThat(userRepository.existsByEmail("active@example.com")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일 - false 반환")
        void existsByEmail_NotExists_ReturnsFalse() {
            assertThat(userRepository.existsByEmail("notexist@example.com")).isFalse();
        }
    }

    // ========== findByRole ==========

    @Nested
    @DisplayName("findByRole()")
    class FindByRoleTests {

        @Test
        @DisplayName("USER 역할 - 2명 반환")
        void findByRole_User_ReturnsTwoUsers() {
            // when
            List<User> users = userRepository.findByRole(UserRole.USER);

            // then
            assertThat(users).hasSize(2);
            assertThat(users).extracting(User::getRole)
                    .containsOnly(UserRole.USER);
        }

        @Test
        @DisplayName("ADMIN 역할 - 1명 반환")
        void findByRole_Admin_ReturnsOneUser() {
            // when
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);

            // then
            assertThat(admins).hasSize(1);
            assertThat(admins.get(0).getEmail()).isEqualTo("admin@example.com");
        }
    }

    // ========== findByIsActive ==========

    @Nested
    @DisplayName("findByIsActive()")
    class FindByIsActiveTests {

        @Test
        @DisplayName("활성 사용자 - 2명 반환")
        void findByIsActive_True_ReturnsTwoUsers() {
            // when
            List<User> activeUsers = userRepository.findByIsActive(true);

            // then
            assertThat(activeUsers).hasSize(2);
            assertThat(activeUsers).extracting(User::getIsActive)
                    .containsOnly(true);
        }

        @Test
        @DisplayName("비활성 사용자 - 1명 반환")
        void findByIsActive_False_ReturnsOneUser() {
            // when
            List<User> inactiveUsers = userRepository.findByIsActive(false);

            // then
            assertThat(inactiveUsers).hasSize(1);
            assertThat(inactiveUsers.get(0).getEmail()).isEqualTo("inactive@example.com");
        }
    }

    // ========== findByEmailAndIsActive ==========

    @Nested
    @DisplayName("findByEmailAndIsActive()")
    class FindByEmailAndIsActiveTests {

        @Test
        @DisplayName("활성 이메일 - 사용자 반환")
        void findByEmailAndIsActive_ActiveEmail_ReturnsUser() {
            // when
            Optional<User> result = userRepository.findByEmailAndIsActive(
                    "active@example.com", true);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("활성 사용자");
        }

        @Test
        @DisplayName("비활성 이메일로 활성 조회 - empty 반환")
        void findByEmailAndIsActive_InactiveEmailWithActiveTrue_ReturnsEmpty() {
            // when
            Optional<User> result = userRepository.findByEmailAndIsActive(
                    "inactive@example.com", true);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========== CRUD 기본 동작 ==========

    @Nested
    @DisplayName("기본 CRUD")
    class CrudTests {

        @Test
        @DisplayName("저장 - ID 자동 생성")
        void save_GeneratesId() {
            // given
            User newUser = User.builder()
                    .email("new@example.com")
                    .password("$2a$10$hashedPassword")
                    .name("새 사용자")
                    .build();

            // when
            User saved = userRepository.save(newUser);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("이메일 unique 제약 - 중복 저장 시 예외")
        void save_DuplicateEmail_ThrowsException() {
            // given
            User duplicate = User.builder()
                    .email("active@example.com")  // 이미 존재
                    .password("$2a$10$hashedPassword")
                    .name("중복 사용자")
                    .build();

            // when & then
            assertThatThrownBy(() -> {
                userRepository.save(duplicate);
                userRepository.flush();  // 즉시 SQL 실행
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("전체 조회 - 3명 반환")
        void findAll_ReturnsThreeUsers() {
            assertThat(userRepository.findAll()).hasSize(3);
        }
    }
}
