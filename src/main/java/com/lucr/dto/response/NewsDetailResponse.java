package com.lucr.dto.response;

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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDetailResponse implements Serializable {

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
     * 감정 분석 레이블
     * 
     * sentimentScore를 한글로 변환한 값
     */
    private String sentimentLabel;
    
    /**
     * 본문 길이 (글자 수)
     */
    private Integer contentLength;
    
    /**
     * 예상 읽기 시간 (분)
     */
    private Integer estimatedReadingTime;
    
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

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}
