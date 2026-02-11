package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Entity - 사용자 정보
 *
 * 계획서 스키마:
 * - id: UUID PK
 * - email: UNIQUE, NOT NULL
 * - password: BCrypt 해싱된 비밀번호
 * - name: 사용자 이름
 * - role: USER / ADMIN
 * - is_active: 활성 상태
 *
 * 테이블명을 "users"로 지정한 이유:
 * - PostgreSQL에서 "user"는 예약어이므로 큰따옴표 없이 사용 불가
 * - "users"로 지정하여 H2, PostgreSQL 모두 호환
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_role", columnList = "role")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * 사용자 고유 ID (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 이메일 (로그인 ID로 사용)
     *
     * - unique = true: 이메일 중복 방지
     * - 회원가입 시 중복 검증 필수
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * 비밀번호 (BCrypt 해싱)
     *
     * - BCrypt 해시 결과는 60자이지만, 향후 알고리즘 변경 대비 255자로 설정
     * - 평문 비밀번호는 절대 저장하지 않음
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 사용자 이름
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 사용자 역할 (USER / ADMIN)
     *
     * - @Enumerated(EnumType.STRING): DB에 "USER", "ADMIN" 문자열로 저장
     * - @Builder.Default: Builder 사용 시 기본값 USER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    /**
     * 계정 활성 상태
     *
     * - true: 활성 (로그인 가능)
     * - false: 비활성 (로그인 차단)
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 생성 시간 (자동 생성)
     *
     * - updatable = false: UPDATE 시 변경 불가
     * - @CreationTimestamp: INSERT 시 자동 저장
     */
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 수정 시간 (자동 업데이트)
     *
     * - @UpdateTimestamp: UPDATE 시 자동 갱신
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * JPA Lifecycle 콜백 - INSERT 전 실행
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * JPA Lifecycle 콜백 - UPDATE 전 실행
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 계정 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 관리자 권한 부여
     */
    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
    }
}
