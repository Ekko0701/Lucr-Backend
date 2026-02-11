package com.lucr.service;

import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * User Service 인터페이스
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
public interface UserService {

    /**
     * 회원가입
     */
    UserDetailResponse register(RegisterRequest request);

    /**
     * ID로 사용자 조회
     */
    UserDetailResponse getUserById(UUID id);

    /**
     * 이메일로 사용자 조회
     */
    UserDetailResponse getUserByEmail(String email);

    /**
     * 전체 사용자 목록 조회 (관리자용)
     */
    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    /**
     * 이메일 중복 확인
     */
    boolean existsByEmail(String email);

    /**
     * 계정 비활성화
     */
    void deactivateUser(UUID id);

    /**
     * 계정 활성화
     */
    void activateUser(UUID id);
}
