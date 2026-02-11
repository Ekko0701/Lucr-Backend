package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken 엔티티 — JWT RefreshToken을 DB에서 관리
 *
 * <p>JWT는 Stateless 특성상 서버에서 토큰을 무효화할 수 없습니다.
 * RefreshToken을 DB에 저장하면 로그아웃 시 삭제하여 무효화할 수 있습니다.</p>
 *
 * <h3>라이프사이클</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │ 1. 로그인 (AuthService.login)                           │
 * │    → RefreshToken 생성 → DB INSERT                       │
 * ├─────────────────────────────────────────────────────────┤
 * │ 2. 토큰 갱신 (AuthService.refreshToken)                  │
 * │    → DB에서 token으로 조회 → 만료 여부 확인                  │
 * │    → 유효하면 새 AccessToken 발급                          │
 * ├─────────────────────────────────────────────────────────┤
 * │ 3. 로그아웃 (AuthService.logout)                          │
 * │    → 해당 사용자의 모든 RefreshToken DELETE                 │
 * │    → 이후 갱신 요청 시 DB에 없으므로 거부됨                   │
 * ├─────────────────────────────────────────────────────────┤
 * │ 4. 정리 (Scheduled Task, 추후 구현)                       │
 * │    → expiresAt이 지난 레코드 일괄 DELETE                    │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>테이블 구조</h3>
 * <pre>
 * refresh_tokens
 * ├── id          UUID PK (자동 생성)
 * ├── user_id     UUID FK → users.id (ManyToOne)
 * ├── token       VARCHAR(500) UNIQUE (JWT 문자열)
 * ├── expires_at  TIMESTAMP (만료 시각)
 * └── created_at  TIMESTAMP (생성 시각, 자동)
 * </pre>
 *
 * <h3>인덱스</h3>
 * <ul>
 *   <li>{@code idx_refresh_token_user_id} — 사용자별 토큰 조회/삭제 성능 최적화</li>
 *   <li>{@code idx_refresh_token_token} — 토큰 문자열 조회 성능 최적화 (갱신 시 사용)</li>
 * </ul>
 *
 * <h3>왜 DB인가? (vs Redis)</h3>
 * <ul>
 *   <li>현재 단계에서는 별도 Redis 인프라 없이 PostgreSQL만으로 구현</li>
 *   <li>추후 Redis로 마이그레이션하면 TTL 기반 자동 만료가 가능해짐</li>
 *   <li>Repository 인터페이스가 동일하므로 교체 시 비즈니스 로직 변경 최소화</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see User
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_token_token", columnList = "token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /**
     * 고유 식별자 (UUID, 자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * RefreshToken 소유자 (User 엔티티 참조)
     *
     * <h4>관계: ManyToOne (다대일)</h4>
     * <p>사용자 1명이 여러 디바이스/브라우저에서 로그인할 수 있으므로,
     * 한 사용자에게 여러 RefreshToken이 존재할 수 있습니다.</p>
     *
     * <h4>FetchType.LAZY 사용 이유</h4>
     * <p>RefreshToken 조회 시 매번 User를 JOIN하지 않습니다.
     * 필요한 경우에만 User 정보를 지연 로딩합니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * JWT RefreshToken 문자열
     *
     * <p>JwtTokenProvider.generateRefreshToken()으로 생성된 JWT 문자열입니다.
     * Header.Payload.Signature 구조이므로 길이가 수백 자에 달할 수 있어
     * 컬럼 길이를 500으로 설정했습니다.</p>
     *
     * <h4>unique = true</h4>
     * <p>동일한 토큰이 중복 저장되는 것을 DB 레벨에서 방지합니다.</p>
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * 토큰 만료 시각
     *
     * <p>토큰 갱신 요청 시 {@link #isExpired()}로 만료 여부를 확인합니다.
     * 이 값은 토큰 생성 시점 + {@code JwtProperties.refreshTokenExpiration}(기본 7일)입니다.</p>
     *
     * <h4>왜 JWT의 exp 클레임과 별도로 관리하는가?</h4>
     * <ul>
     *   <li>DB 쿼리로 만료된 토큰을 일괄 삭제 가능 ({@code deleteByExpiresAtBefore})</li>
     *   <li>JWT 파싱 없이 만료 여부를 빠르게 확인 가능</li>
     * </ul>
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰 생성 시각 (자동 설정)
     *
     * <p>Hibernate의 {@code @CreationTimestamp}에 의해 INSERT 시 자동으로 현재 시각이 저장됩니다.
     * 감사(audit) 및 디버깅 목적으로 사용됩니다.</p>
     */
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 토큰 만료 여부 확인
     *
     * <p>현재 시각이 {@code expiresAt} 이후인지 비교합니다.</p>
     *
     * <h4>사용처</h4>
     * <p>{@code AuthServiceImpl.refreshToken()}에서 DB 조회 후 호출:</p>
     * <pre>
     * RefreshToken refreshToken = repository.findByToken(token)...;
     * if (refreshToken.isExpired()) {
     *     throw AuthenticationException.expiredToken();
     * }
     * </pre>
     *
     * @return {@code true} — 만료됨 (갱신 거부), {@code false} — 유효 (갱신 가능)
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
