package com.lucr.security;

import com.lucr.entity.User;
import com.lucr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Spring Security UserDetailsService 구현 — DB 기반 사용자 인증 정보 제공
 *
 * <p>Spring Security가 사용자 인증을 수행할 때 필요한 사용자 정보를
 * DB에서 조회하여 {@link UserDetails} 객체로 변환하는 역할을 합니다.</p>
 *
 * <h3>왜 이 클래스가 필요한가?</h3>
 * <p>Spring Security는 사용자 정보를 {@link UserDetails} 인터페이스로 추상화합니다.
 * 우리 프로젝트는 사용자 정보를 {@link User} 엔티티로 관리하므로,
 * 이 둘을 연결하는 어댑터(Adapter) 역할이 필요합니다.</p>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                Spring Security 인증 흐름                         │
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │  AuthenticationManager.authenticate(email, password)            │
 * │          ↓                                                      │
 * │  CustomUserDetailsService.loadUserByUsername(email)              │
 * │          ↓                                                      │
 * │  UserRepository.findByEmailAndIsActive(email, true)             │
 * │          ↓                                                      │
 * │  User 엔티티 → Spring Security UserDetails 변환                   │
 * │          ↓                                                      │
 * │  AuthenticationManager가 password 비교 (PasswordEncoder 사용)     │
 * │          ↓                                                      │
 * │  인증 성공 → SecurityContext에 Authentication 저장                 │
 * │                                                                 │
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>사용처</h3>
 * <ul>
 *   <li>{@code AuthenticationManager} — 로그인 시 이메일로 사용자 정보를 조회하고,
 *       반환된 {@code UserDetails}의 비밀번호와 입력된 비밀번호를 {@code PasswordEncoder}로 비교</li>
 *   <li>{@code JwtAuthenticationFilter} — (선택적) 토큰 기반 사용자 로드
 *       (현재 프로젝트에서는 JWT Claims에서 직접 정보를 추출하므로 필터에서는 미사용)</li>
 * </ul>
 *
 * <h3>왜 {@code @Transactional(readOnly = true)}인가?</h3>
 * <p>이 클래스는 오직 조회만 수행합니다. {@code readOnly = true}를 설정하면:</p>
 * <ul>
 *   <li>Hibernate의 Dirty Checking을 건너뜀 → 성능 최적화</li>
 *   <li>DB에 READ 전용 트랜잭션 힌트 전달 → DB 레벨 최적화 가능</li>
 *   <li>실수로 데이터를 변경하는 것을 방지</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-11
 * @see UserDetails
 * @see UserDetailsService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 이메일(username)로 사용자 정보를 로드하여 {@link UserDetails}로 변환
     *
     * <p>Spring Security의 {@code UserDetailsService} 인터페이스를 구현합니다.
     * 메서드 이름이 {@code loadUserByUsername}이지만, 우리 프로젝트에서는
     * 이메일을 로그인 식별자로 사용하므로 {@code username = email}입니다.</p>
     *
     * <h4>처리 흐름</h4>
     * <ol>
     *   <li>이메일 + 활성 상태(isActive=true)로 사용자 조회</li>
     *   <li>사용자가 없거나 비활성이면 {@link UsernameNotFoundException} 발생</li>
     *   <li>User 엔티티를 Spring Security {@link UserDetails} 객체로 변환</li>
     * </ol>
     *
     * <h4>UserDetails 매핑</h4>
     * <pre>
     * User 엔티티                     → UserDetails
     * ─────────────────────────────────────────────────
     * user.getEmail()                → username
     * user.getPassword()             → password (BCrypt 해시)
     * "ROLE_" + user.getRole().name() → authorities (예: ROLE_USER, ROLE_ADMIN)
     * !user.getIsActive()            → disabled
     * </pre>
     *
     * <h4>왜 {@code findByEmailAndIsActive(email, true)}를 사용하는가?</h4>
     * <p>비활성화된 계정(isActive=false)은 아예 조회되지 않습니다.
     * Spring Security의 {@code disabled} 플래그와 이중으로 검증하여 안전성을 확보합니다.</p>
     *
     * <h4>권한(Authorities) 접두사 "ROLE_" 규약</h4>
     * <p>Spring Security의 {@code hasRole("ADMIN")}은 내부적으로
     * {@code "ROLE_ADMIN"} 권한을 확인합니다. 따라서 반드시 {@code "ROLE_"} 접두사를 붙여야 합니다.</p>
     * <ul>
     *   <li>{@code hasRole("ADMIN")} → {@code "ROLE_ADMIN"} 검사</li>
     *   <li>{@code hasAuthority("ROLE_ADMIN")} → 동일하게 {@code "ROLE_ADMIN"} 검사</li>
     * </ul>
     *
     * @param email 사용자 이메일 (Spring Security 프레임워크에서는 "username"으로 전달)
     * @return 인증에 필요한 사용자 정보를 담은 {@link UserDetails} 객체
     * @throws UsernameNotFoundException 이메일에 해당하는 활성 사용자가 없을 때
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 정보 로드 요청: email={}", email);

        // 이메일 + 활성 상태로 사용자 조회 (비활성 계정은 조회되지 않음)
        User user = userRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음 또는 비활성 계정: email={}", email);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
                });

        // User 엔티티 → Spring Security UserDetails 변환
        // Spring Security가 제공하는 User.builder()를 사용하여 UserDetails 생성
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())                // 로그인 식별자 = 이메일
                .password(user.getPassword())             // BCrypt 해싱된 비밀번호
                .authorities(Collections.singletonList(   // 권한 목록 (단일 역할)
                        // "ROLE_" + "USER" = "ROLE_USER" 또는 "ROLE_" + "ADMIN" = "ROLE_ADMIN"
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .accountExpired(false)                    // 계정 만료 여부 (미사용, 항상 false)
                .accountLocked(false)                     // 계정 잠금 여부 (미사용, 항상 false)
                .credentialsExpired(false)                // 비밀번호 만료 여부 (미사용, 항상 false)
                .disabled(!user.getIsActive())            // 비활성 계정이면 disabled=true
                .build();
    }
}
