package com.lucr.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 뉴스 생성 요청 DTO
 * 
 * 클라이언트로부터 받는 뉴스 생성 데이터
 * - Entity와 분리하여 클라이언트가 보낼 수 있는 필드만 정의
 * - 자동 생성되는 필드(id, createdAt 등)는 포함하지 않음
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsCreateRequest {
    
    /**
     * 뉴스 제목 (필수)
     * 
     * Validation:
     * - @NotBlank: null, 빈 문자열, 공백만 있는 문자열 불가
     * - @Size: 최소 5자, 최대 500자
     */
    @NotBlank(message = "뉴스 제목은 필수입니다.")
    @Size(min = 5, max = 500, message = "뉴스 제목은 5자 이상 500자 이하여야 합니다.")
    private String title;
    
    /**
     * 뉴스 본문 (필수)
     * 
     * Validation:
     * - @NotBlank: null, 빈 문자열 불가
     * - @Size: 최소 10자
     */
    @NotBlank(message = "뉴스 본문은 필수입니다.")
    @Size(min = 10, message = "뉴스 본문은 10자 이상이어야 합니다.")
    private String content;
    
    /**
     * 뉴스 출처 (필수)
     * 
     * 예시: NAVER_FINANCE, DAUM_FINANCE
     * 
     * Validation:
     * - @NotBlank: null, 빈 문자열 불가
     * - @Size: 최대 100자
     */
    @NotBlank(message = "뉴스 출처는 필수입니다.")
    @Size(max = 100, message = "뉴스 출처는 100자 이하여야 합니다.")
    private String source;
    
    /**
     * 뉴스 URL (필수)
     * 
     * 중복 체크에 사용
     * 
     * Validation:
     * - @NotBlank: null, 빈 문자열 불가
     */
    @NotBlank(message = "뉴스 URL은 필수입니다.")
    private String url;
    
    /**
     * 뉴스 발행 시간 (선택)
     * 
     * 크롤링한 뉴스의 원본 발행 시간
     * null이면 현재 시간으로 설정됨
     */
    private LocalDateTime publishedAt;
}
