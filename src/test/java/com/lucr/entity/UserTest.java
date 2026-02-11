package com.lucr.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * User Entity 단위 테스트
 *
 * - Builder 기본값 검증
 * - 비즈니스 메서드 (deactivate, activate, promoteToAdmin) 검증
 * - JPA Lifecycle 콜백 검증
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@DisplayName("User Entity 테스트")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("$2a$10$hashedPassword")
                .name("테스트 사용자")
                .build();
    }

    // ========== Builder 기본값 테스트 ==========

    @Nested
    @DisplayName("Builder 기본값")
    class BuilderDefaultTests {

        @Test
        @DisplayName("role 기본값은 USER")
        void defaultRole_IsUser() {
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("isActive 기본값은 true")
        void defaultIsActive_IsTrue() {
            assertThat(user.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Builder로 role을 ADMIN으로 지정 가능")
        void builderRole_Admin() {
            User admin = User.builder()
                    .email("admin@example.com")
                    .password("password")
                    .name("관리자")
                    .role(UserRole.ADMIN)
                    .build();

            assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Builder로 isActive를 false로 지정 가능")
        void builderIsActive_False() {
            User inactive = User.builder()
                    .email("inactive@example.com")
                    .password("password")
                    .name("비활성 사용자")
                    .isActive(false)
                    .build();

            assertThat(inactive.getIsActive()).isFalse();
        }
    }

    // ========== 비즈니스 메서드 테스트 ==========

    @Nested
    @DisplayName("비즈니스 메서드")
    class BusinessMethodTests {

        @Test
        @DisplayName("deactivate() - isActive가 false로 변경")
        void deactivate_SetsIsActiveFalse() {
            // given
            assertThat(user.getIsActive()).isTrue();

            // when
            user.deactivate();

            // then
            assertThat(user.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("activate() - isActive가 true로 변경")
        void activate_SetsIsActiveTrue() {
            // given
            user.deactivate();
            assertThat(user.getIsActive()).isFalse();

            // when
            user.activate();

            // then
            assertThat(user.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("promoteToAdmin() - role이 ADMIN으로 변경")
        void promoteToAdmin_SetsRoleAdmin() {
            // given
            assertThat(user.getRole()).isEqualTo(UserRole.USER);

            // when
            user.promoteToAdmin();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        }
    }

    // ========== JPA Lifecycle 콜백 테스트 ==========

    @Nested
    @DisplayName("JPA Lifecycle 콜백")
    class LifecycleCallbackTests {

        @Test
        @DisplayName("onCreate() - createdAt, updatedAt 자동 설정")
        void onCreate_SetsTimestamps() {
            // given
            User newUser = User.builder()
                    .email("new@example.com")
                    .password("password")
                    .name("새 사용자")
                    .build();

            assertThat(newUser.getCreatedAt()).isNull();
            assertThat(newUser.getUpdatedAt()).isNull();

            // when
            newUser.onCreate();

            // then
            assertThat(newUser.getCreatedAt()).isNotNull();
            assertThat(newUser.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("onCreate() - 이미 값이 있으면 덮어쓰지 않음")
        void onCreate_DoesNotOverwriteExistingTimestamps() {
            // given
            LocalDateTime existingTime = LocalDateTime.of(2026, 1, 1, 0, 0);
            User userWithTimestamp = User.builder()
                    .email("existing@example.com")
                    .password("password")
                    .name("기존 사용자")
                    .createdAt(existingTime)
                    .updatedAt(existingTime)
                    .build();

            // when
            userWithTimestamp.onCreate();

            // then
            assertThat(userWithTimestamp.getCreatedAt()).isEqualTo(existingTime);
            assertThat(userWithTimestamp.getUpdatedAt()).isEqualTo(existingTime);
        }

        @Test
        @DisplayName("onUpdate() - updatedAt 갱신")
        void onUpdate_RefreshesUpdatedAt() {
            // given
            LocalDateTime before = LocalDateTime.of(2026, 1, 1, 0, 0);
            user.setUpdatedAt(before);

            // when
            user.onUpdate();

            // then
            assertThat(user.getUpdatedAt()).isAfter(before);
        }
    }

    // ========== Getter/Setter 테스트 ==========

    @Nested
    @DisplayName("Getter / Setter")
    class GetterSetterTests {

        @Test
        @DisplayName("모든 필드 정상 접근")
        void allFields_Accessible() {
            assertThat(user.getId()).isNotNull();
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getPassword()).isEqualTo("$2a$10$hashedPassword");
            assertThat(user.getName()).isEqualTo("테스트 사용자");
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("password setter로 변경 가능")
        void setPassword_Works() {
            // when
            user.setPassword("$2a$10$newHashedPassword");

            // then
            assertThat(user.getPassword()).isEqualTo("$2a$10$newHashedPassword");
        }
    }
}
