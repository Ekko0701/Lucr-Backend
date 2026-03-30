package com.lucr.dto.response;

import com.lucr.entity.CrawlJob;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 크롤링 작업 응답 DTO
 *
 * CrawlJob Entity → 클라이언트 응답 변환용
 * - Entity의 내부 구조를 외부에 노출하지 않기 위해 DTO로 변환
 * - 상태 조회, 트리거 응답 등에서 공통으로 사용
 *
 * @author Ekko0701
 * @since 2026-02-06
 */
@Schema(description = "크롤링 작업 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlJobResponse {

    @Schema(description = "작업 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    @Schema(
            description = "작업 상태",
            example = "COMPLETED",
            allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED"}
    )
    private String status;
    @Schema(description = "수집된 총 기사 수", example = "150")
    private Integer totalArticles;
    @Schema(
            description = "언론사별 수집 결과 JSON 문자열. 클라이언트에서는 JSON 파싱 후 사용합니다.",
            example = "{\\\"naver\\\":{\\\"success\\\":50,\\\"failed\\\":2},\\\"daum\\\":{\\\"success\\\":45,\\\"failed\\\":1}}"
    )
    private String mediaResults;
    @Schema(description = "에러 메시지 (실패 시)", example = "RabbitMQ publish failed")
    private String errorMessage;
    @Schema(description = "작업 생성 시간", example = "2026-03-30T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "작업 수정 시간", example = "2026-03-30T10:05:00")
    private LocalDateTime updatedAt;
    @Schema(description = "작업 완료 시간", example = "2026-03-30T10:10:00")
    private LocalDateTime completedAt;

    // ========== 변환 메서드 ==========

    /**
     * CrawlJob Entity → CrawlJobResponse DTO 변환
     *
     * @param entity CrawlJob 엔티티
     * @return 변환된 응답 DTO
     */
    public static CrawlJobResponse from(CrawlJob entity) {
        return CrawlJobResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus().name())
                .totalArticles(entity.getTotalArticles())
                .mediaResults(entity.getMediaResults())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
