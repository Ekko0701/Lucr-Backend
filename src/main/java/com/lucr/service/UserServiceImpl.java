package com.lucr.service;

import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.dto.response.UserResponse;
import com.lucr.entity.User;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.UserMapper;
import com.lucr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Service 구현체
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDetailResponse register(RegisterRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());

        // 1. 이메일 중복 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("중복된 이메일로 회원가입 시도: email={}", request.getEmail());
            throw DuplicateResourceException.duplicateEmail(request.getEmail());
        }

        // 2. Entity 변환
        User user = userMapper.toEntity(request);

        // 3. 비밀번호 BCrypt 해싱
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. 저장
        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toDetailResponse(savedUser);
    }

    @Override
    public UserDetailResponse getUserById(UUID id) {
        log.debug("사용자 조회 요청: id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.userNotFound(id.toString());
                });

        return userMapper.toDetailResponse(user);
    }

    @Override
    public UserDetailResponse getUserByEmail(String email) {
        log.debug("사용자 이메일 조회 요청: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음: email={}", email);
                    return ResourceNotFoundException.userNotFound(email);
                });

        return userMapper.toDetailResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("사용자 목록 조회 요청: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findAll(pageable);

        List<UserResponse> responses = userPage.getContent().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(userPage, responses);
    }

    @Override
    public boolean existsByEmail(String email) {
        log.debug("이메일 중복 확인: email={}", email);
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        log.info("계정 비활성화 요청: id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("비활성화할 사용자를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.userNotFound(id.toString());
                });

        user.deactivate();
        log.info("계정 비활성화 완료: id={}", id);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        log.info("계정 활성화 요청: id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("활성화할 사용자를 찾을 수 없음: id={}", id);
                    return ResourceNotFoundException.userNotFound(id.toString());
                });

        user.activate();
        log.info("계정 활성화 완료: id={}", id);
    }
}
