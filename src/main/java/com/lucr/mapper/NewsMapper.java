package com.lucr.mapper;

import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.entity.News;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * News Entity ↔ DTO 변환 Mapper
 * 
 * Entity와 DTO 간의 변환을 담당
 * - 계층 간 데이터 전달 시 사용
 * - Entity를 직접 노출하지 않고 DTO로 변환
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Component
public class NewsMapper {
    
    // ========== Request DTO → Entity 변환 ==========
    
    /**
     * NewsCreateRequest → News Entity 변환
     * 
     * 새 뉴스 생성 시 사용
     * - 자동 생성 필드(id, createdAt 등)는 JPA가 처리
     * - viewCount, isHighView는 기본값 사용
     * 
     * @param request 뉴스 생성 요청 DTO
     * @return News Entity
     */
    public News toEntity(NewsCreateRequest request) {
        return News.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .source(request.getSource())
                .url(request.getUrl())
                .publishedAt(request.getPublishedAt() != null 
                        ? request.getPublishedAt() 
                        : LocalDateTime.now())
                // viewCount, isHighView는 Entity의 @Builder.Default 사용
                // id, createdAt, updatedAt는 JPA가 자동 생성
                .build();
    }
    
    /**
     * NewsUpdateRequest로 기존 Entity 업데이트
     * 
     * 부분 업데이트 지원
     * - null이 아닌 필드만 업데이트
     * - null인 필드는 기존 값 유지
     * 
     * @param entity 업데이트할 Entity
     * @param request 업데이트 요청 DTO
     */
    public void updateEntity(News entity, NewsUpdateRequest request) {
        // null이 아닌 경우에만 업데이트
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        
        if (request.getContent() != null) {
            entity.setContent(request.getContent());
        }
        
        if (request.getSource() != null) {
            entity.setSource(request.getSource());
        }
        
        if (request.getSentimentScore() != null) {
            entity.setSentimentScore(request.getSentimentScore());
        }
        
        // updatedAt는 @UpdateTimestamp가 자동 처리
    }
    
    // ========== Entity → Response DTO 변환 ==========
    
    /**
     * News Entity → NewsResponse 변환
     * 
     * 목록 조회 시 사용
     * - 간단한 정보만 포함
     * - 본문은 100자로 요약
     * 
     * @param entity News Entity
     * @return NewsResponse DTO
     */
    public NewsResponse toResponse(News entity) {
        // 본문 요약 (100자 + "...")
        String contentSummary = createContentSummary(entity.getContent());
        
        return NewsResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .contentSummary(contentSummary)
                .source(entity.getSource())
                .url(entity.getUrl())
                .viewCount(entity.getViewCount())
                .isHighView(entity.getIsHighView())
                .sentimentScore(entity.getSentimentScore())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    /**
     * News Entity → NewsDetailResponse 변환
     * 
     * 상세 조회 시 사용
     * - 모든 정보 포함
     * - 전체 본문 포함
     * - 메타데이터 추가
     * 
     * @param entity News Entity
     * @return NewsDetailResponse DTO
     */
    public NewsDetailResponse toDetailResponse(News entity) {
        // contentLength, sentimentLabel, estimatedReadingTime은 DTO에서 자동 계산
        return NewsDetailResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .source(entity.getSource())
                .url(entity.getUrl())
                .viewCount(entity.getViewCount())
                .isHighView(entity.getIsHighView())
                .sentimentScore(entity.getSentimentScore())
                .publishedAt(entity.getPublishedAt())
                .crawledAt(entity.getCrawledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    // ========== Helper 메서드 ==========
    
    /**
     * 본문 요약 생성
     * 
     * 본문이 100자보다 길면 100자까지만 자르고 "..." 추가
     * 
     * @param content 전체 본문
     * @return 요약된 본문
     */
    private String createContentSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        if (content.length() <= 100) {
            return content;
        }
        
        return content.substring(0, 100) + "...";
    }
    
    /**
     * 사용 예시
     * 
     * // 1. Request → Entity (생성)
     * NewsCreateRequest createRequest = ...;
     * News news = newsMapper.toEntity(createRequest);
     * newsRepository.save(news);
     * 
     * // 2. Request → Entity (수정)
     * News existingNews = newsRepository.findById(id).orElseThrow();
     * NewsUpdateRequest updateRequest = ...;
     * newsMapper.updateEntity(existingNews, updateRequest);
     * newsRepository.save(existingNews);  // Dirty Checking으로 자동 UPDATE
     * 
     * // 3. Entity → Response (목록)
     * List<News> newsList = newsRepository.findAll();
     * List<NewsResponse> responses = newsList.stream()
     *     .map(newsMapper::toResponse)
     *     .toList();
     * 
     * // 4. Entity → DetailResponse (상세)
     * News news = newsRepository.findById(id).orElseThrow();
     * NewsDetailResponse response = newsMapper.toDetailResponse(news);
     */
}
