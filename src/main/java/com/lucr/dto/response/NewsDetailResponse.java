package com.lucr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.io.Serializable;

/**
 * 뉴스 상세 응답 DTO
 * 
 * 단건 조회 시 사용 (모든 정보 포함)
 * - NewsResponse보다 더 많은 필드 포함
 * - 전체 본문 포함
 * - 메타데이터 포함 (크롤링 시간, 수정 시간 등)
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Schema(description = "뉴스 상세 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "뉴스 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    @Schema(description = "뉴스 제목", example = "삼성전자 주가 급등, 반도체 호황 영향")
    private String title;
    @Schema(
            description = "뉴스 본문 (전체)",
            example = "삼성전자가 반도체 호황에 힘입어 주가가 급등했다. 시장에서는 실적 개선 기대감이 반영된 결과로 보고 있다."
    )
    private String content;
    @Schema(description = "뉴스 출처", example = "NAVER_FINANCE")
    private String source;
    @Schema(description = "뉴스 URL", example = "https://news.example.com/article/123")
    private String url;
    @Schema(description = "조회수", example = "1500")
    private Integer viewCount;
    @Schema(description = "인기 뉴스 여부", example = "true")
    private Boolean isHighView;
    @Schema(description = "감정 분석 점수 (-1.0 ~ 1.0)", example = "0.75")
    private BigDecimal sentimentScore;
    @Schema(description = "감정 분석 레이블", example = "긍정적")
    private String sentimentLabel;
    @Schema(description = "본문 길이 (글자 수)", example = "2500")
    private Integer contentLength;
    @Schema(description = "예상 읽기 시간 (분)", example = "5")
    private Integer estimatedReadingTime;
    @Schema(description = "뉴스 발행 시간", example = "2026-03-30T09:00:00")
    private LocalDateTime publishedAt;
    @Schema(description = "크롤링 시간", example = "2026-03-30T09:03:00")
    private LocalDateTime crawledAt;
    @Schema(description = "DB 저장 시간", example = "2026-03-30T09:05:00")
    private LocalDateTime createdAt;
    @Schema(description = "마지막 수정 시간", example = "2026-03-30T10:00:00")
    private LocalDateTime updatedAt;

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}
