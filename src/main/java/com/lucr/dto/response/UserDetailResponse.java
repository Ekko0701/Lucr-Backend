package com.lucr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 상세 응답 DTO
 *
 * - password는 절대 응답에 포함하지 않음
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    private UUID id;
    private String email;
    private String name;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
