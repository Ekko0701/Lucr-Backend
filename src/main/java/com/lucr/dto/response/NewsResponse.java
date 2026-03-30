package com.lucr.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * 뉴스 응답 DTO
 * 
 * 클라이언트에게 전달하는 뉴스 데이터
 * - 목록 조회 시 사용 (간단한 정보만)
 * - Entity의 모든 필드를 노출하지 않음 (보안)
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Schema(description = "뉴스 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "뉴스 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    @Schema(description = "뉴스 제목", example = "삼성전자 주가 급등, 반도체 호황 영향")
    private String title;
    @Schema(description = "본문 요약 (최대 100자)", example = "삼성전자가 반도체 호황에 힘입어...")
    private String contentSummary;
    @Schema(description = "뉴스 출처", example = "NAVER_FINANCE")
    private String source;
    @Schema(description = "뉴스 URL", example = "https://news.example.com/article/123")
    private String url;
    @Schema(description = "조회수", example = "1500")
    private Integer viewCount;
    @Schema(description = "인기 뉴스 여부 (조회수 1000+)", example = "true")
    private Boolean isHighView;
    @Schema(description = "감정 분석 점수 (-1.0 ~ 1.0, null: 미분석)", example = "0.75")
    private BigDecimal sentimentScore;
    @Schema(description = "감정 분석 레이블", example = "긍정적")
    private String sentimentLabel;
    @Schema(description = "뉴스 발행 시간", example = "2026-03-30T09:00:00")
    private LocalDateTime publishedAt;
    @Schema(description = "DB 저장 시간", example = "2026-03-30T09:05:00")
    private LocalDateTime createdAt;
    
    /**
     * 사용 예시
     * 
     * // Entity -> Response 변환
     * NewsResponse response = NewsResponse.builder()
     *     .id(news.getId())
     *     .title(news.getTitle())
     *     .contentSummary(news.getContent().substring(0, 100) + "...")
     *     .source(news.getSource())
     *     .url(news.getUrl())
     *     .viewCount(news.getViewCount())
     *     .isHighView(news.getIsHighView())
     *     .sentimentScore(news.getSentimentScore())
     *     .publishedAt(news.getPublishedAt())
     *     .createdAt(news.getCreatedAt())
     *     .build();
     */
}
