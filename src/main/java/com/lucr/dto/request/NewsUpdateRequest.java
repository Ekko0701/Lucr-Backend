package com.lucr.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 뉴스 수정 요청 DTO
 * 
 * 뉴스 정보 수정 시 사용
 * - 모든 필드가 선택적 (수정하고 싶은 필드만 전송)
 * - null이 아닌 필드만 업데이트
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsUpdateRequest {
    
    /**
     * 뉴스 제목 (선택)
     * 
     * null이 아닐 경우에만 업데이트
     */
    @Size(min = 5, max = 500, message = "뉴스 제목은 5자 이상 500자 이하여야 합니다.")
    private String title;
    
    /**
     * 뉴스 본문 (선택)
     * 
     * null이 아닐 경우에만 업데이트
     */
    @Size(min = 10, message = "뉴스 본문은 10자 이상이어야 합니다.")
    private String content;
    
    /**
     * 뉴스 출처 (선택)
     * 
     * null이 아닐 경우에만 업데이트
     */
    @Size(max = 100, message = "뉴스 출처는 100자 이하여야 합니다.")
    private String source;
    
    /**
     * 감정 분석 점수 (선택)
     * 
     * AI 분석 결과를 업데이트할 때 사용
     * 
     * Validation:
     * - @DecimalMin: 최소값 -1.0 (매우 부정적)
     * - @DecimalMax: 최대값 1.0 (매우 긍정적)
     */
    @DecimalMin(value = "-1.0", message = "감정 점수는 -1.0 이상이어야 합니다.")
    @DecimalMax(value = "1.0", message = "감정 점수는 1.0 이하여야 합니다.")
    private BigDecimal sentimentScore;
    
    /**
     * 비고
     * 
     * - id는 URL path에서 받으므로 포함하지 않음
     * - url은 unique key이므로 수정 불가
     * - viewCount는 incrementViewCount() 메서드로만 변경
     * - 자동 생성 필드(createdAt, updatedAt)는 수정 불가
     */
}
