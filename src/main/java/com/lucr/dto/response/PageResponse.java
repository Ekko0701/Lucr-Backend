package com.lucr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 DTO
 * 
 * 페이징된 데이터를 클라이언트에 전달할 때 사용
 * - 실제 데이터 (content)
 * - 페이징 메타데이터 (총 개수, 페이지 정보 등)
 * 
 * 제네릭을 사용하여 모든 타입의 데이터에 사용 가능
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    
    /**
     * 실제 데이터 목록
     * 
     * 예: List<NewsResponse>
     */
    private List<T> content;
    
    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private Integer currentPage;
    
    /**
     * 페이지 크기 (한 페이지당 항목 수)
     */
    private Integer pageSize;
    
    /**
     * 전체 항목 수
     */
    private Long totalElements;
    
    /**
     * 전체 페이지 수
     */
    private Integer totalPages;
    
    /**
     * 첫 페이지 여부
     */
    private Boolean isFirst;
    
    /**
     * 마지막 페이지 여부
     */
    private Boolean isLast;
    
    /**
     * 다음 페이지 존재 여부
     */
    private Boolean hasNext;
    
    /**
     * 이전 페이지 존재 여부
     */
    private Boolean hasPrevious;
    
    /**
     * Spring Data JPA의 Page 객체로부터 PageResponse 생성
     * 
     * 편의 메서드: Page<Entity> -> PageResponse<DTO>
     * 
     * @param page Spring Data JPA Page 객체
     * @param content 변환된 DTO 리스트
     * @param <T> DTO 타입
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
    
    /**
     * 빈 페이지 응답 생성
     * 
     * 검색 결과가 없을 때 사용
     * 
     * @param <T> DTO 타입
     * @return 빈 PageResponse 객체
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(List.of())
                .currentPage(0)
                .pageSize(0)
                .totalElements(0L)
                .totalPages(0)
                .isFirst(true)
                .isLast(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
    
    /**
     * 사용 예시 1: Service에서 Page<Entity> -> PageResponse<DTO> 변환
     * 
     * // Repository에서 페이징 조회
     * Page<News> newsPage = newsRepository.findAll(pageable);
     * 
     * // Entity -> DTO 변환
     * List<NewsResponse> newsResponses = newsPage.getContent()
     *     .stream()
     *     .map(NewsMapper::toResponse)
     *     .toList();
     * 
     * // PageResponse 생성
     * PageResponse<NewsResponse> response = PageResponse.of(newsPage, newsResponses);
     */
    
    /**
     * 사용 예시 2: 클라이언트가 받는 JSON 구조
     * 
     * {
     *   "content": [
     *     {
     *       "id": "550e8400-e29b-41d4-a716-446655440000",
     *       "title": "삼성전자 주가 급등",
     *       "contentSummary": "삼성전자가...",
     *       "viewCount": 1500,
     *       ...
     *     },
     *     ...
     *   ],
     *   "currentPage": 0,
     *   "pageSize": 20,
     *   "totalElements": 125,
     *   "totalPages": 7,
     *   "isFirst": true,
     *   "isLast": false,
     *   "hasNext": true,
     *   "hasPrevious": false
     * }
     */
    
    /**
     * 사용 예시 3: 프론트엔드에서 페이지네이션 UI 구성
     * 
     * // React 예시
     * const { content, currentPage, totalPages, hasNext, hasPrevious } = response;
     * 
     * // 뉴스 목록 렌더링
     * content.map(news => <NewsCard key={news.id} data={news} />)
     * 
     * // 페이지네이션 버튼
     * <Button disabled={!hasPrevious} onClick={() => goToPage(currentPage - 1)}>
     *   이전
     * </Button>
     * <span>{currentPage + 1} / {totalPages}</span>
     * <Button disabled={!hasNext} onClick={() => goToPage(currentPage + 1)}>
     *   다음
     * </Button>
     */
}
