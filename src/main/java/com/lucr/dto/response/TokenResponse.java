package com.lucr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 *
 * <p>로그인 성공 또는 토큰 갱신 시 클라이언트에게 반환되는 응답 본문입니다.</p>
 *
 * <h3>응답 예시 — 로그인 성공</h3>
 * <pre>
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 1800
 * }
 * </pre>
 *
 * <h3>응답 예시 — 토큰 갱신</h3>
 * <pre>
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...(새 토큰)",
 *   "refreshToken": null,
 *   "tokenType": "Bearer",
 *   "expiresIn": 1800
 * }
 * </pre>
 *
 * <h3>클라이언트 사용 방법</h3>
 * <pre>
 * // 1. 로그인 후 토큰 저장
 * localStorage.setItem("accessToken", response.accessToken);
 * localStorage.setItem("refreshToken", response.refreshToken);
 *
 * // 2. API 요청 시 AccessToken을 헤더에 포함
 * Authorization: Bearer {accessToken}
 *
 * // 3. AccessToken 만료 시 (expiresIn 초 후) RefreshToken으로 갱신
 * POST /api/v1/auth/refresh  { "refreshToken": "..." }
 * </pre>
 *
 * <h3>필드 설명</h3>
 * <ul>
 *   <li>{@code accessToken} — API 요청 인증용 단기 토큰 (30분)</li>
 *   <li>{@code refreshToken} — AccessToken 갱신용 장기 토큰 (7일, 갱신 응답에서는 null)</li>
 *   <li>{@code tokenType} — 토큰 타입 (항상 "Bearer", OAuth 2.0 표준)</li>
 *   <li>{@code expiresIn} — AccessToken 만료까지 남은 시간 (초 단위)</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Schema(description = "토큰 응답")
@Getter
@NoArgsConstructor   // Jackson 직렬화용 기본 생성자
@AllArgsConstructor  // Builder 내부에서 사용
@Builder
public class TokenResponse {

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "JWT 리프레시 토큰 (로그인 응답에서만 포함, 토큰 갱신 응답에서는 null)", nullable = true)
    private String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "액세스 토큰 만료 시간 (초)", example = "1800")
    private long expiresIn;

    /**
     * 로그인 응답용 팩토리 메서드 (AccessToken + RefreshToken 모두 포함)
     *
     * <p>{@code AuthService.login()} 성공 시 호출됩니다.</p>
     *
     * @param accessToken  새로 발급된 AccessToken
     * @param refreshToken 새로 발급된 RefreshToken
     * @param expiresIn    AccessToken 만료 시간 (초)
     * @return 로그인 응답 TokenResponse
     */
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();
    }

    /**
     * 토큰 갱신 응답용 팩토리 메서드 (AccessToken만 포함)
     *
     * <p>{@code AuthService.refreshToken()} 성공 시 호출됩니다.
     * RefreshToken은 기존 것을 유지하므로 응답에 포함하지 않습니다.</p>
     *
     * @param accessToken 새로 발급된 AccessToken
     * @param expiresIn   AccessToken 만료 시간 (초)
     * @return 갱신 응답 TokenResponse (refreshToken은 null)
     */
    public static TokenResponse ofAccessToken(String accessToken, long expiresIn) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .build();
    }
}
