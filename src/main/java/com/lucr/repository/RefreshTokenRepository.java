package com.lucr.repository;

import com.lucr.entity.RefreshToken;
import com.lucr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshToken JPA Repository
 *
 * <p>JWT RefreshToken의 CRUD 및 정리 작업을 위한 데이터 접근 계층입니다.</p>
 *
 * <h3>주요 사용 시나리오</h3>
 * <pre>
 * ┌──────────────┬──────────────────────────────────┬──────────────────────────────────┐
 * │ 시나리오       │ 호출 메서드                         │ 호출 위치                          │
 * ├──────────────┼──────────────────────────────────┼──────────────────────────────────┤
 * │ 로그인        │ save(refreshToken)               │ AuthServiceImpl.login()          │
 * │ 토큰 갱신     │ findByToken(token)               │ AuthServiceImpl.refreshToken()   │
 * │ 로그아웃      │ deleteByUser(user)               │ AuthServiceImpl.logout()         │
 * │ 만료 토큰 정리 │ deleteByExpiresAtBefore(dateTime) │ Scheduled Task (추후 구현)        │
 * └──────────────┴──────────────────────────────────┴──────────────────────────────────┘
 * </pre>
 *
 * <h3>Spring Data JPA 쿼리 자동 생성</h3>
 * <p>메서드 이름 규칙에 따라 JPQL이 자동으로 생성됩니다:</p>
 * <ul>
 *   <li>{@code findByToken(token)} → {@code SELECT r FROM RefreshToken r WHERE r.token = :token}</li>
 *   <li>{@code deleteByUser(user)} → {@code DELETE FROM RefreshToken r WHERE r.user = :user}</li>
 *   <li>{@code deleteByUserId(userId)} → {@code DELETE FROM RefreshToken r WHERE r.user.id = :userId}</li>
 *   <li>{@code deleteByExpiresAtBefore(dateTime)} → {@code DELETE FROM RefreshToken r WHERE r.expiresAt < :dateTime}</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see RefreshToken
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * 토큰 문자열로 RefreshToken 조회
     *
     * <p>토큰 갱신 요청 시 클라이언트가 전송한 refreshToken 문자열로
     * DB에 저장된 RefreshToken 엔티티를 조회합니다.</p>
     *
     * <h4>조회 결과에 따른 처리</h4>
     * <ul>
     *   <li>{@code Optional.empty()} — 로그아웃으로 삭제되었거나 존재하지 않는 토큰
     *       → {@code REFRESH_TOKEN_NOT_FOUND} 에러</li>
     *   <li>{@code Optional.present()} → {@code isExpired()} 확인 후 갱신 진행</li>
     * </ul>
     *
     * <p>※ {@code idx_refresh_token_token} 인덱스에 의해 빠른 조회가 보장됩니다.</p>
     *
     * @param token JWT RefreshToken 문자열
     * @return RefreshToken 엔티티 (없으면 Optional.empty)
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자의 모든 RefreshToken 삭제 (로그아웃 시)
     *
     * <p>해당 사용자가 모든 디바이스에서 로그아웃되는 효과를 가집니다.
     * 삭제 후 이전에 발급된 RefreshToken으로는 토큰 갱신이 불가능합니다.</p>
     *
     * <h4>주의: @Transactional 필요</h4>
     * <p>DELETE 쿼리이므로 호출하는 Service 메서드에
     * {@code @Transactional} 어노테이션이 반드시 필요합니다.</p>
     *
     * @param user 로그아웃 대상 사용자 엔티티
     */
    void deleteByUser(User user);

    /**
     * 사용자 ID로 모든 RefreshToken 삭제
     *
     * <p>{@link #deleteByUser(User)}와 동일한 기능이지만,
     * User 엔티티 없이 UUID만으로 삭제할 수 있어 편의성을 제공합니다.</p>
     *
     * @param userId 사용자 UUID
     */
    void deleteByUserId(UUID userId);

    /**
     * 특정 시각 이전에 만료된 RefreshToken 일괄 삭제 (정리 작업용)
     *
     * <p>만료된 토큰이 DB에 계속 누적되는 것을 방지합니다.
     * 추후 {@code @Scheduled} 기반 정기 작업에서 호출될 예정입니다.</p>
     *
     * <h4>사용 예시 (추후 구현)</h4>
     * <pre>
     * // 매일 자정에 만료된 토큰 삭제
     * {@literal @}Scheduled(cron = "0 0 0 * * *")
     * public void cleanupExpiredTokens() {
     *     refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
     * }
     * </pre>
     *
     * @param dateTime 이 시각 이전에 만료된 토큰을 삭제
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
