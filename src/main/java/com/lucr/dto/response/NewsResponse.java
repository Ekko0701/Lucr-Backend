package com.lucr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {
    
    /**
     * 뉴스 ID
     */
    private UUID id;
    
    /**
     * 뉴스 제목
     */
    private String title;
    
    /**
     * 뉴스 본문 (요약본)
     * 
     * 목록 조회 시 전체 본문은 너무 길므로
     * Service에서 앞 100자만 잘라서 전달
     */
    private String contentSummary;
    
    /**
     * 뉴스 출처
     */
    private String source;
    
    /**
     * 뉴스 URL
     */
    private String url;
    
    /**
     * 조회수
     */
    private Integer viewCount;
    
    /**
     * 인기 뉴스 여부
     * 
     * true: 조회수 1000 이상
     */
    private Boolean isHighView;
    
    /**
     * 감정 분석 점수
     * 
     * -1.0 (매우 부정) ~ 1.0 (매우 긍정)
     * null: 아직 분석되지 않음
     */
    private BigDecimal sentimentScore;
    
    /**
     * 뉴스 발행 시간
     */
    private LocalDateTime publishedAt;
    
    /**
     * 생성 시간 (DB 저장 시간)
     */
    private LocalDateTime createdAt;
    
    /**
     * 감정 분석 결과를 한글로 반환
     * 
     * 프론트엔드에서 사용하기 편하도록 추가
     */
    public String getSentimentLabel() {
        if (sentimentScore == null) {
            return "분석 전";
        }
        
        double score = sentimentScore.doubleValue();
        
        if (score >= 0.7) return "매우 긍정적";
        if (score >= 0.3) return "긍정적";
        if (score >= -0.3) return "중립";
        if (score >= -0.7) return "부정적";
        return "매우 부정적";
    }
    
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
