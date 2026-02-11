package com.lucr.dto.response;

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
@Getter
@NoArgsConstructor   // Jackson 직렬화용 기본 생성자
@AllArgsConstructor  // Builder 내부에서 사용
@Builder
public class TokenResponse {

    /**
     * JWT AccessToken
     *
     * <p>API 요청 시 {@code Authorization: Bearer {accessToken}} 헤더에 포함합니다.
     * 서버의 {@code JwtAuthenticationFilter}가 이 토큰을 추출하여 인증을 수행합니다.</p>
     */
    private String accessToken;

    /**
     * JWT RefreshToken
     *
     * <p>로그인 응답에서는 값이 포함되고, 토큰 갱신 응답에서는 {@code null}입니다.
     * 클라이언트는 이 토큰을 안전하게 저장하고 AccessToken 만료 시 갱신 요청에 사용합니다.</p>
     */
    private String refreshToken;

    /**
     * 토큰 타입 (OAuth 2.0 표준)
     *
     * <p>항상 "Bearer"입니다.
     * 클라이언트가 Authorization 헤더를 구성할 때 이 값을 접두사로 사용합니다.</p>
     *
     * <p>예: {@code Authorization: Bearer eyJhbGciOi...}</p>
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * AccessToken 만료까지 남은 시간 (초 단위)
     *
     * <p>클라이언트가 토큰 갱신 타이밍을 계산하는 데 사용합니다.
     * 예: 1800 → 30분 후 만료</p>
     *
     * <p>권장 전략: {@code expiresIn}의 80~90% 시점에 미리 갱신 요청</p>
     */
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
