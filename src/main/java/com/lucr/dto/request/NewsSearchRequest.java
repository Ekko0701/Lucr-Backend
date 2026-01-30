package com.lucr.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 뉴스 검색 요청 DTO
 * 
 * 복잡한 검색 조건을 처리하기 위한 DTO
 * - 모든 필드가 선택적 (원하는 조건만 조합 가능)
 * - 페이징 파라미터 포함
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSearchRequest {
    
    /**
     * 검색 키워드 (선택)
     * 
     * 제목 또는 본문에 포함된 키워드 검색
     * 예시: "삼성전자", "반도체", "주가"
     */
    private String keyword;
    
    /**
     * 뉴스 출처 (선택)
     * 
     * 특정 출처의 뉴스만 검색
     * 예시: "NAVER_FINANCE"
     */
    private String source;
    
    /**
     * 최소 조회수 (선택)
     * 
     * 조회수가 이 값 이상인 뉴스만 검색
     * 예시: 1000 (인기 뉴스만)
     */
    @Min(value = 0, message = "최소 조회수는 0 이상이어야 합니다.")
    private Integer minViewCount;
    
    /**
     * 최소 감정 점수 (선택)
     * 
     * 감정 점수가 이 값 이상인 뉴스만 검색
     * 예시: 0.5 (긍정적인 뉴스만)
     */
    private BigDecimal minSentimentScore;
    
    /**
     * 최대 감정 점수 (선택)
     * 
     * 감정 점수가 이 값 이하인 뉴스만 검색
     * 예시: -0.5 (부정적인 뉴스만)
     */
    private BigDecimal maxSentimentScore;
    
    /**
     * 시작 날짜 (선택)
     * 
     * 이 날짜 이후 발행된 뉴스만 검색
     * 예시: LocalDateTime.now().minusDays(7) (최근 7일)
     */
    private LocalDateTime startDate;
    
    /**
     * 종료 날짜 (선택)
     * 
     * 이 날짜 이전 발행된 뉴스만 검색
     */
    private LocalDateTime endDate;
    
    /**
     * 인기 뉴스 여부 (선택)
     * 
     * true: 조회수 1000 이상인 뉴스만
     * false: 조회수 1000 미만인 뉴스만
     * null: 모든 뉴스
     */
    private Boolean isHighView;
    
    // ========== 페이징 파라미터 ==========
    
    /**
     * 페이지 번호 (0부터 시작)
     * 
     * 기본값: 0 (첫 페이지)
     */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    @Builder.Default
    private Integer page = 0;
    
    /**
     * 페이지 크기 (한 페이지당 항목 수)
     * 
     * 기본값: 20
     */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Builder.Default
    private Integer size = 20;
    
    /**
     * 정렬 기준 (선택)
     * 
     * 예시:
     * - "viewCount,desc" (조회수 내림차순)
     * - "publishedAt,desc" (최신순)
     * - "createdAt,asc" (오래된 순)
     * 
     * 기본값: "createdAt,desc" (최신 생성순)
     */
    @Builder.Default
    private String sort = "createdAt,desc";
    
    /**
     * 사용 예시
     * 
     * // 1. 키워드 검색
     * NewsSearchRequest.builder()
     *     .keyword("삼성전자")
     *     .build();
     * 
     * // 2. 인기 뉴스 (조회수 1000 이상)
     * NewsSearchRequest.builder()
     *     .minViewCount(1000)
     *     .sort("viewCount,desc")
     *     .build();
     * 
     * // 3. 긍정적인 뉴스 (최근 7일)
     * NewsSearchRequest.builder()
     *     .minSentimentScore(BigDecimal.valueOf(0.5))
     *     .startDate(LocalDateTime.now().minusDays(7))
     *     .build();
     * 
     * // 4. 네이버 금융 뉴스 (최신 10개)
     * NewsSearchRequest.builder()
     *     .source("NAVER_FINANCE")
     *     .size(10)
     *     .sort("publishedAt,desc")
     *     .build();
     */
}
