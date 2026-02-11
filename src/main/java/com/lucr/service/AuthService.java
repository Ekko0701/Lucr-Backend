package com.lucr.service;

import com.lucr.dto.request.LoginRequest;
import com.lucr.dto.request.TokenRefreshRequest;
import com.lucr.dto.response.TokenResponse;

import java.util.UUID;

/**
 * 인증 서비스 인터페이스
 *
 * <p>JWT 기반 인증의 핵심 비즈니스 로직을 정의합니다.</p>
 *
 * <h3>인증 흐름</h3>
 * <pre>
 * 1. 로그인  — 이메일/비밀번호 → AccessToken + RefreshToken 발급
 * 2. 갱신    — RefreshToken → 새 AccessToken 발급
 * 3. 로그아웃 — RefreshToken DB에서 삭제 (무효화)
 * </pre>
 *
 * <h3>토큰 전략</h3>
 * <ul>
 *   <li><b>AccessToken</b>: 30분 유효, API 인증에 사용, Stateless</li>
 *   <li><b>RefreshToken</b>: 7일 유효, DB 저장, AccessToken 갱신에 사용</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see com.lucr.security.JwtTokenProvider
 */
public interface AuthService {

    /**
     * 로그인 — 이메일/비밀번호 검증 후 토큰 쌍 발급
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>이메일로 활성 사용자 조회</li>
     *   <li>비밀번호 일치 여부 검증 (BCrypt)</li>
     *   <li>AccessToken + RefreshToken 생성</li>
     *   <li>기존 RefreshToken 삭제 → 새 RefreshToken DB 저장</li>
     * </ol>
     *
     * @param request 이메일 + 비밀번호
     * @return AccessToken + RefreshToken + 만료시간
     * @throws com.lucr.exception.ResourceNotFoundException 사용자를 찾을 수 없거나 비활성 계정
     * @throws com.lucr.exception.AuthenticationException   비밀번호 불일치
     */
    TokenResponse login(LoginRequest request);

    /**
     * 토큰 갱신 — RefreshToken 검증 후 새 AccessToken 발급
     *
     * <p>이중 검증:</p>
     * <ol>
     *   <li>DB에 해당 RefreshToken 존재 확인</li>
     *   <li>DB 만료 시간 확인 ({@code isExpired()})</li>
     *   <li>JWT 서명 검증 ({@code validateToken()})</li>
     * </ol>
     *
     * @param request RefreshToken 문자열
     * @return 새 AccessToken + 만료시간 (RefreshToken은 null)
     * @throws com.lucr.exception.ResourceNotFoundException RefreshToken을 찾을 수 없음
     * @throws com.lucr.exception.AuthenticationException   만료되었거나 유효하지 않은 RefreshToken
     */
    TokenResponse refresh(TokenRefreshRequest request);

    /**
     * 로그아웃 — 사용자의 모든 RefreshToken 삭제
     *
     * <p>AccessToken은 Stateless이므로 만료 시까지 유효합니다.
     * RefreshToken만 DB에서 삭제하여 토큰 갱신을 차단합니다.</p>
     *
     * @param userId 로그아웃할 사용자 ID (SecurityContext에서 추출)
     */
    void logout(UUID userId);
}
