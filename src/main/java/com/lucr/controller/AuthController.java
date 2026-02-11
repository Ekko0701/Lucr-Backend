package com.lucr.controller;

import com.lucr.common.ApiResponse;
import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 REST API 컨트롤러
 *
 * - POST /api/v1/auth/register : 회원가입
 * - GET  /api/v1/auth/check-email : 이메일 중복 확인
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    /**
     * 회원가입
     *
     * @param request 회원가입 요청 (email, password, name)
     * @return 201 Created + 생성된 사용자 상세 정보
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDetailResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("회원가입 요청: email={}", request.getEmail());

        UserDetailResponse data = userService.register(request);

        log.info("회원가입 완료: id={}, email={}", data.getId(), data.getEmail());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 성공적으로 완료되었습니다.", data));
    }

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일 주소
     * @return 200 OK + 중복 여부 (true: 이미 존재, false: 사용 가능)
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        log.info("이메일 중복 확인 요청: email={}", email);

        boolean exists = userService.existsByEmail(email);
        String message = exists ?
                "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";

        log.info("이메일 중복 확인 완료: email={}, exists={}", email, exists);
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }
}
