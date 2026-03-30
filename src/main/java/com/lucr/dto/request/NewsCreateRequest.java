package com.lucr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 뉴스 생성 요청 DTO
 * 
 * 클라이언트로부터 받는 뉴스 생성 데이터
 * - Entity와 분리하여 클라이언트가 보낼 수 있는 필드만 정의
 * - 자동 생성되는 필드(id, createdAt 등)는 포함하지 않음
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Schema(description = "뉴스 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsCreateRequest {

    @Schema(
            description = "뉴스 제목",
            example = "삼성전자 주가 급등, 반도체 호황 영향",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "뉴스 제목은 필수입니다.")
    @Size(min = 5, max = 500, message = "뉴스 제목은 5자 이상 500자 이하여야 합니다.")
    private String title;

    @Schema(
            description = "뉴스 본문",
            example = "삼성전자가 반도체 호황에 힘입어 주가가 급등했다...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "뉴스 본문은 필수입니다.")
    @Size(min = 10, message = "뉴스 본문은 10자 이상이어야 합니다.")
    private String content;

    @Schema(
            description = "뉴스 출처",
            example = "NAVER_FINANCE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "뉴스 출처는 필수입니다.")
    @Size(max = 100, message = "뉴스 출처는 100자 이하여야 합니다.")
    private String source;

    @Schema(
            description = "뉴스 URL (중복 체크 대상)",
            example = "https://news.example.com/article/123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "뉴스 URL은 필수입니다.")
    private String url;

    @Schema(description = "뉴스 발행 시간 (null이면 현재 시간)", example = "2026-03-30T09:00:00")
    private LocalDateTime publishedAt;
}
