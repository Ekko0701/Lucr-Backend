package com.lucr.mapper;

import com.lucr.dto.request.RegisterRequest;
import com.lucr.dto.response.UserDetailResponse;
import com.lucr.dto.response.UserResponse;
import com.lucr.entity.User;
import org.springframework.stereotype.Component;

/**
 * User Entity ↔ DTO 변환 매퍼
 *
 * 주의: password는 이 매퍼에서 설정하지 않습니다.
 *       BCrypt 해싱은 비즈니스 로직이므로 Service 계층에서 처리합니다.
 *
 * @author Ekko0701
 * @since 2026-02-11
 */
@Component
public class UserMapper {

    /**
     * RegisterRequest → User Entity 변환
     *
     * password는 Service에서 BCrypt 해싱 후 별도로 설정합니다.
     * role은 @Builder.Default로 USER 자동 설정됩니다.
     */
    public User toEntity(RegisterRequest request) {
        return User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .build();
    }

    /**
     * User Entity → UserResponse 변환 (목록용)
     */
    public UserResponse toResponse(User entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .role(entity.getRole().name())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * User Entity → UserDetailResponse 변환 (상세용)
     */
    public UserDetailResponse toDetailResponse(User entity) {
        return UserDetailResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .role(entity.getRole().name())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
