package com.lucr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 *
 * <p>클라이언트가 {@code POST /api/v1/auth/login} 엔드포인트로 전송하는 JSON 요청 본문입니다.</p>
 *
 * <h3>요청 예시</h3>
 * <pre>
 * POST /api/v1/auth/login
 * Content-Type: application/json
 *
 * {
 *   "email": "user@example.com",
 *   "password": "Password1!"
 * }
 * </pre>
 *
 * <h3>처리 흐름</h3>
 * <pre>
 * Client → AuthController.login(LoginRequest)
 *              ↓
 *          AuthService.login()
 *              ↓
 *          1. UserRepository.findByEmail(email) — 사용자 조회
 *          2. PasswordEncoder.matches(password, user.getPassword()) — 비밀번호 검증
 *          3. JwtTokenProvider.generateAccessToken() + generateRefreshToken() — 토큰 발급
 *              ↓
 *          TokenResponse (accessToken, refreshToken, expiresIn)
 * </pre>
 *
 * <h3>유효성 검증</h3>
 * <ul>
 *   <li>{@code email} — null/빈 문자열 불가, 이메일 형식 필수</li>
 *   <li>{@code password} — null/빈 문자열 불가</li>
 * </ul>
 *
 * <p>유효성 검증 실패 시 {@code GlobalExceptionHandler}에서
 * {@code MethodArgumentNotValidException}을 처리하여 400 응답을 반환합니다.</p>
 *
 * <h3>보안 참고</h3>
 * <p>로그인 요청에서는 비밀번호 형식 검증(@Pattern)을 하지 않습니다.
 * 회원가입({@link RegisterRequest})에서만 비밀번호 정책을 검증하며,
 * 로그인에서는 DB에 저장된 해시값과의 비교만 수행합니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see RegisterRequest
 */
@Schema(description = "로그인 요청")
@Getter
@NoArgsConstructor   // Jackson 역직렬화용 기본 생성자
@AllArgsConstructor  // 테스트에서 직접 생성용
@Builder             // 테스트에서 빌더 패턴 사용
public class LoginRequest {

    /**
     * 로그인 이메일
     *
     * <p>회원가입 시 등록한 이메일 주소입니다.
     * {@code UserRepository.findByEmail()}로 사용자를 조회하는 데 사용됩니다.</p>
     */
    @Schema(
            description = "이메일 주소",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    /**
     * 로그인 비밀번호 (평문)
     *
     * <p>서버에서 {@code BCryptPasswordEncoder.matches(rawPassword, encodedPassword)}로
     * DB에 저장된 해시값과 비교합니다. 평문 비밀번호는 절대 저장되거나 로깅되지 않습니다.</p>
     */
    @Schema(
            description = "비밀번호",
            example = "Password@123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
