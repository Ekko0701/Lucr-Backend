package com.lucr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "사용자 상세 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "권한", example = "ROLE_USER")
    private String role;
    @Schema(description = "활성 상태", example = "true")
    private Boolean isActive;
    @Schema(description = "가입일", example = "2026-03-01T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "정보 수정일", example = "2026-03-15T14:30:00")
    private LocalDateTime updatedAt;
}
