package com.lucr.controller;

import com.lucr.common.ApiResponse;
import com.lucr.dto.request.LoginRequest;
import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.request.TokenRefreshRequest;
import com.lucr.dto.response.TokenResponse;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.service.AuthService;
import com.lucr.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 인증 관련 REST API 컨트롤러
 *
 * <h3>엔드포인트 목록</h3>
 * <pre>
 * POST  /api/v1/auth/register     — 회원가입          (인증 불필요)
 * GET   /api/v1/auth/check-email  — 이메일 중복 확인   (인증 불필요)
 * POST  /api/v1/auth/login        — 로그인 → 토큰 발급 (인증 불필요)
 * POST  /api/v1/auth/refresh      — 토큰 갱신          (인증 불필요)
 * POST  /api/v1/auth/logout       — 로그아웃           (인증 필요)
 * </pre>
 *
 * <h3>인증 정책</h3>
 * <ul>
 *   <li>register, check-email, login, refresh — SecurityConfig에서 permitAll</li>
 *   <li>logout — anyRequest().authenticated()에 포함 (유효한 AccessToken 필요)</li>
 * </ul>
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
    private final AuthService authService;

    // ==================== 회원가입 ====================

    /**
     * 회원가입
     *
     * <p>POST /api/v1/auth/register</p>
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

    // ==================== 이메일 중복 확인 ====================

    /**
     * 이메일 중복 확인
     *
     * <p>GET /api/v1/auth/check-email?email=xxx@xxx.com</p>
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

    // ==================== 로그인 ====================

    /**
     * 로그인 — 이메일/비밀번호 검증 후 JWT 토큰 쌍 발급
     *
     * <p>POST /api/v1/auth/login</p>
     *
     * <h4>요청 예시</h4>
     * <pre>
     * {
     *   "email": "user@example.com",
     *   "password": "Password@123"
     * }
     * </pre>
     *
     * <h4>성공 응답 (200 OK)</h4>
     * <pre>
     * {
     *   "success": true,
     *   "message": "로그인이 성공적으로 완료되었습니다.",
     *   "data": {
     *     "accessToken": "eyJhbGciOi...",
     *     "refreshToken": "eyJhbGciOi...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 1800
     *   }
     * }
     * </pre>
     *
     * @param request 로그인 요청 (email, password)
     * @return 200 OK + AccessToken + RefreshToken
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("로그인 요청: email={}", request.getEmail());

        TokenResponse data = authService.login(request);

        log.info("로그인 완료: email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("로그인이 성공적으로 완료되었습니다.", data));
    }

    // ==================== 토큰 갱신 ====================

    /**
     * 토큰 갱신 — RefreshToken으로 새 AccessToken 발급
     *
     * <p>POST /api/v1/auth/refresh</p>
     *
     * <h4>요청 예시</h4>
     * <pre>
     * {
     *   "refreshToken": "eyJhbGciOi..."
     * }
     * </pre>
     *
     * <h4>성공 응답 (200 OK)</h4>
     * <pre>
     * {
     *   "success": true,
     *   "message": "토큰이 성공적으로 갱신되었습니다.",
     *   "data": {
     *     "accessToken": "eyJhbGciOi...(새 토큰)",
     *     "refreshToken": null,
     *     "tokenType": "Bearer",
     *     "expiresIn": 1800
     *   }
     * }
     * </pre>
     *
     * @param request RefreshToken
     * @return 200 OK + 새 AccessToken
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        log.info("토큰 갱신 요청");

        TokenResponse data = authService.refresh(request);

        log.info("토큰 갱신 완료");
        return ResponseEntity.ok(ApiResponse.success("토큰이 성공적으로 갱신되었습니다.", data));
    }

    // ==================== 로그아웃 ====================

    /**
     * 로그아웃 — 인증된 사용자의 RefreshToken 삭제
     *
     * <p>POST /api/v1/auth/logout</p>
     * <p>Authorization: Bearer {accessToken} 헤더 필요</p>
     *
     * <p>SecurityContext에서 인증된 사용자 ID를 추출하여
     * 해당 사용자의 모든 RefreshToken을 DB에서 삭제합니다.</p>
     *
     * <h4>주의사항</h4>
     * <ul>
     *   <li>AccessToken은 Stateless이므로 만료 시까지 유효합니다</li>
     *   <li>RefreshToken만 무효화하여 토큰 갱신을 차단합니다</li>
     *   <li>즉시 무효화가 필요하면 추후 블랙리스트(Redis) 도입 검토</li>
     * </ul>
     *
     * @return 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // SecurityContext에서 인증된 사용자 ID 추출
        // JwtAuthenticationFilter에서 principal = UUID userId로 설정됨
        UUID userId = (UUID) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        log.info("로그아웃 요청: userId={}", userId);

        authService.logout(userId);

        log.info("로그아웃 완료: userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 성공적으로 완료되었습니다."));
    }
}
