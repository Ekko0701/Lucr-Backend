package com.lucr.repository;

import com.lucr.entity.User;
import com.lucr.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User JPA Repository
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 이메일로 사용자 조회
     * - 로그인 시 사용
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     * - 회원가입 시 중복 체크용
     */
    boolean existsByEmail(String email);

    /**
     * 역할별 사용자 조회
     */
    List<User> findByRole(UserRole role);

    /**
     * 활성 상태별 사용자 조회
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * 이메일과 활성 상태로 사용자 조회
     * - 로그인 시 비활성 계정 필터링용
     */
    Optional<User> findByEmailAndIsActive(String email, Boolean isActive);
}
