package com.lucr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDetailResponse {
    
    /**
     * 뉴스 ID
     */
    private UUID id;
    
    /**
     * 뉴스 제목
     */
    private String title;
    
    /**
     * 뉴스 본문 (전체)
     * 
     * 상세 조회 시에는 전체 본문 제공
     */
    private String content;
    
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
     */
    private Boolean isHighView;
    
    /**
     * 감정 분석 점수
     * 
     * -1.0 (매우 부정) ~ 1.0 (매우 긍정)
     */
    private BigDecimal sentimentScore;
    
    /**
     * 뉴스 발행 시간
     */
    private LocalDateTime publishedAt;
    
    /**
     * 크롤링 시간
     * 
     * 실제로 뉴스를 수집한 시간
     */
    private LocalDateTime crawledAt;
    
    /**
     * 생성 시간
     * 
     * DB에 저장된 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     * 
     * 마지막 업데이트 시간
     */
    private LocalDateTime updatedAt;
    
    // ========== 추가 메타데이터 ==========
    
    /**
     * 본문 길이 (글자 수)
     * 
     * 프론트엔드에서 "예상 읽기 시간" 계산에 사용 가능
     */
    private Integer contentLength;
    
    /**
     * 감정 분석 결과 (한글)
     */
    private String sentimentLabel;
    
    /**
     * 예상 읽기 시간 (분)
     * 
     * 일반적으로 사람은 분당 200-250자 읽음
     * contentLength / 200 으로 계산
     */
    private Integer estimatedReadingTime;
    
    /**
     * 감정 분석 결과를 한글로 반환
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
     * 예상 읽기 시간 계산
     */
    public Integer getEstimatedReadingTime() {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // 분당 200자 기준
        int minutes = content.length() / 200;
        return minutes < 1 ? 1 : minutes;
    }
    
    /**
     * Builder 패턴 사용 예시
     * 
     * NewsDetailResponse response = NewsDetailResponse.builder()
     *     .id(news.getId())
     *     .title(news.getTitle())
     *     .content(news.getContent())
     *     .source(news.getSource())
     *     .url(news.getUrl())
     *     .viewCount(news.getViewCount())
     *     .isHighView(news.getIsHighView())
     *     .sentimentScore(news.getSentimentScore())
     *     .publishedAt(news.getPublishedAt())
     *     .crawledAt(news.getCrawledAt())
     *     .createdAt(news.getCreatedAt())
     *     .updatedAt(news.getUpdatedAt())
     *     .contentLength(news.getContent().length())
     *     .sentimentLabel(getSentimentLabel())
     *     .estimatedReadingTime(getEstimatedReadingTime())
     *     .build();
     */
}
