package com.lucr.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 감정 분석 레이블
     * 
     * sentimentScore를 한글로 변환한 값
     * "분석 전", "매우 긍정적", "긍정적", "중립", "부정적", "매우 부정적"
     */
    private String sentimentLabel;
    
    /**
     * 뉴스 발행 시간
     */
    private LocalDateTime publishedAt;
    
    /**
     * 생성 시간 (DB 저장 시간)
     */
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
