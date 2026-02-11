package com.lucr.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 요청 DTO
 *
 * <p>AccessToken이 만료되었을 때 클라이언트가 {@code POST /api/v1/auth/refresh}
 * 엔드포인트로 전송하는 JSON 요청 본문입니다.</p>
 *
 * <h3>요청 예시</h3>
 * <pre>
 * POST /api/v1/auth/refresh
 * Content-Type: application/json
 *
 * {
 *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
 * }
 * </pre>
 *
 * <h3>처리 흐름</h3>
 * <pre>
 * Client → AuthController.refresh(TokenRefreshRequest)
 *              ↓
 *          AuthService.refreshToken()
 *              ↓
 *          1. JwtTokenProvider.validateToken(refreshToken) — JWT 유효성 검증
 *          2. RefreshTokenRepository.findByToken(refreshToken) — DB 존재 여부 확인
 *          3. RefreshToken.isExpired() — DB 저장된 만료 시간 검증
 *          4. JwtTokenProvider.generateAccessToken() — 새 AccessToken 발급
 *              ↓
 *          TokenResponse (새 accessToken, expiresIn)
 * </pre>
 *
 * <h3>왜 RefreshToken을 Body로 전송하는가?</h3>
 * <ul>
 *   <li>Authorization 헤더는 AccessToken 전용으로 사용</li>
 *   <li>Body에 포함하여 역할을 명확히 분리</li>
 *   <li>추후 HttpOnly Cookie 방식으로 전환 가능 (보안 강화)</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Getter
@NoArgsConstructor   // Jackson 역직렬화용 기본 생성자
@AllArgsConstructor  // 테스트에서 직접 생성용
@Builder             // 테스트에서 빌더 패턴 사용
public class TokenRefreshRequest {

    /**
     * 리프레시 토큰 (JWT 문자열)
     *
     * <p>로그인 성공 시 발급받은 RefreshToken을 그대로 전송합니다.
     * 서버는 이 토큰의 JWT 유효성과 DB 존재 여부를 모두 검증합니다.</p>
     *
     * <h4>이중 검증 이유</h4>
     * <ul>
     *   <li>JWT 검증 — 서명 위변조, 만료 확인</li>
     *   <li>DB 검증 — 로그아웃으로 삭제된 토큰인지 확인 (JWT만으로는 무효화 불가)</li>
     * </ul>
     */
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
