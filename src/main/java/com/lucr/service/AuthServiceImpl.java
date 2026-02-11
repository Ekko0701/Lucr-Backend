package com.lucr.service;

import com.lucr.config.JwtProperties;
import com.lucr.dto.request.LoginRequest;
import com.lucr.dto.request.TokenRefreshRequest;
import com.lucr.dto.response.TokenResponse;
import com.lucr.entity.RefreshToken;
import com.lucr.entity.User;
import com.lucr.exception.AuthenticationException;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.repository.RefreshTokenRepository;
import com.lucr.repository.UserRepository;
import com.lucr.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 인증 서비스 구현체
 *
 * <p>로그인, 토큰 갱신, 로그아웃 비즈니스 로직을 담당합니다.</p>
 *
 * <h3>의존성</h3>
 * <ul>
 *   <li>{@code UserRepository} — 사용자 조회 (이메일 + 활성 상태)</li>
 *   <li>{@code RefreshTokenRepository} — RefreshToken CRUD</li>
 *   <li>{@code JwtTokenProvider} — JWT 생성/검증</li>
 *   <li>{@code JwtProperties} — RefreshToken 만료 시간 설정값</li>
 *   <li>{@code PasswordEncoder} — BCrypt 비밀번호 검증</li>
 * </ul>
 *
 * <h3>트랜잭션 전략</h3>
 * <ul>
 *   <li>클래스 레벨: {@code @Transactional(readOnly = true)} — 기본 읽기 전용</li>
 *   <li>쓰기 메서드: {@code @Transactional} — 개별 오버라이드</li>
 * </ul>
 *
 * <h3>RefreshToken 전략</h3>
 * <p>단일 디바이스 정책: 로그인 시 기존 RefreshToken을 모두 삭제하고 새로 발급합니다.
 * 다중 디바이스 지원이 필요하면 디바이스 식별자를 추가하여 확장할 수 있습니다.</p>
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인 — 이메일/비밀번호 검증 후 토큰 쌍 발급
     *
     * <p>처리 흐름:</p>
     * <pre>
     * 1. 이메일 + 활성 상태로 사용자 조회
     *    └── 없으면 ResourceNotFoundException (USER_NOT_FOUND)
     * 2. BCrypt로 비밀번호 비교
     *    └── 불일치하면 AuthenticationException (INVALID_PASSWORD)
     * 3. AccessToken 생성 (userId, email, role → Claims)
     * 4. RefreshToken 생성 (userId → sub)
     * 5. 기존 RefreshToken 삭제 → 새 RefreshToken DB 저장
     * 6. TokenResponse 반환
     * </pre>
     *
     * @param request 이메일 + 비밀번호
     * @return AccessToken + RefreshToken + 만료시간
     */
    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());

        // 1. 사용자 조회 (이메일 + 활성 상태)
        User user = userRepository.findByEmailAndIsActive(request.getEmail(), true)
                .orElseThrow(() -> {
                    log.warn("로그인 실패 — 사용자 없음 또는 비활성: email={}", request.getEmail());
                    return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                });

        // 2. 비밀번호 검증 (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("로그인 실패 — 비밀번호 불일치: email={}", request.getEmail());
            throw AuthenticationException.invalidPassword();
        }

        // 3. AccessToken 생성 (userId, email, role을 Claims에 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        // 4. RefreshToken 생성 (userId만 sub에 포함)
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 5. 기존 RefreshToken 삭제 후 새로 저장 (단일 디바이스 정책)
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("로그인 성공: userId={}, email={}", user.getId(), user.getEmail());

        // 6. 응답 조립
        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * 토큰 갱신 — RefreshToken 검증 후 새 AccessToken 발급
     *
     * <p>이중 검증 수행:</p>
     * <pre>
     * 1. DB에서 RefreshToken 조회
     *    └── 없으면 ResourceNotFoundException (REFRESH_TOKEN_NOT_FOUND)
     * 2. DB의 expiresAt 기준 만료 확인
     *    └── 만료 → 삭제 + AuthenticationException (EXPIRED_TOKEN)
     * 3. JWT 서명 검증 (jwtTokenProvider.validateToken)
     *    └── 실패 → 삭제 + AuthenticationException (INVALID_TOKEN)
     * 4. 사용자 정보 추출 → 새 AccessToken 발급
     * </pre>
     *
     * @param request RefreshToken 문자열
     * @return 새 AccessToken + 만료시간
     */
    @Override
    @Transactional
    public TokenResponse refresh(TokenRefreshRequest request) {
        log.info("토큰 갱신 요청");

        // 1. RefreshToken DB 조회
        RefreshToken refreshTokenEntity = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 — RefreshToken을 찾을 수 없음");
                    return new ResourceNotFoundException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

        // 2. DB 기준 만료 확인
        if (refreshTokenEntity.isExpired()) {
            log.warn("토큰 갱신 실패 — RefreshToken 만료: userId={}", refreshTokenEntity.getUser().getId());
            refreshTokenRepository.delete(refreshTokenEntity);
            throw AuthenticationException.expiredToken();
        }

        // 3. JWT 서명 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            log.warn("토큰 갱신 실패 — RefreshToken 서명 검증 실패");
            refreshTokenRepository.delete(refreshTokenEntity);
            throw AuthenticationException.invalidToken();
        }

        // 4. 새 AccessToken 발급
        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        log.info("토큰 갱신 성공: userId={}", user.getId());

        return TokenResponse.ofAccessToken(
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * 로그아웃 — 사용자의 모든 RefreshToken 삭제
     *
     * <p>AccessToken은 Stateless이므로 만료 시까지 유효합니다.
     * RefreshToken만 DB에서 삭제하여 토큰 갱신을 차단합니다.</p>
     *
     * <p>추후 AccessToken 즉시 무효화가 필요하면 블랙리스트(Redis) 도입을 고려하세요.</p>
     *
     * @param userId 로그아웃할 사용자 ID
     */
    @Override
    @Transactional
    public void logout(UUID userId) {
        log.info("로그아웃 요청: userId={}", userId);

        refreshTokenRepository.deleteByUserId(userId);

        log.info("로그아웃 완료: userId={}", userId);
    }
}
