package com.lucr.dto.response;

import com.lucr.entity.CrawlJob;
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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlJobResponse {

    /** 작업 고유 ID */
    private UUID id;

    /** 작업 상태 (PENDING / RUNNING / COMPLETED / FAILED) */
    private String status;

    /** 수집된 총 기사 수 */
    private Integer totalArticles;

    /** 언론사별 수집 결과 JSON */
    private String mediaResults;

    /** 에러 메시지 (실패 시) */
    private String errorMessage;

    /** 작업 생성 시간 */
    private LocalDateTime createdAt;

    /** 작업 수정 시간 */
    private LocalDateTime updatedAt;

    /** 작업 완료 시간 */
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
