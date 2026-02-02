package com.lucr.controller;

import tools.jackson.databind.ObjectMapper;
import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NewsController 단위 테스트
 *
 * @WebMvcTest: Controller 레이어만 로드 (가벼운 테스트)
 * MockMvc: HTTP 요청/응답 시뮬레이션
 * @MockBean: Service를 Mock으로 대체
 */
@WebMvcTest(NewsController.class)
@DisplayName("NewsController 테스트")
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @Autowired
    private ObjectMapper objectMapper;

    // 테스트 데이터
    private UUID testId;
    private NewsResponse newsResponse;
    private NewsDetailResponse newsDetailResponse;
    private NewsCreateRequest createRequest;
    private NewsUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        // NewsResponse 테스트 데이터
        newsResponse = NewsResponse.builder()
                .id(testId)
                .title("삼성전자 반도체 사업 확대")
                .contentSummary("삼성전자가 반도체 생산 시설을 대규모 확대할 계획을 발표했습니다...")
                .source("NAVER_FINANCE")
                .url("https://finance.naver.com/news/1")
                .viewCount(150)
                .isHighView(false)
                .sentimentScore(BigDecimal.valueOf(0.65))
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // NewsDetailResponse 테스트 데이터
        // content 길이: 54자 → contentLength=54, estimatedReadingTime=1 (54/200=0, 최소 1)
        newsDetailResponse = NewsDetailResponse.builder()
                .id(testId)
                .title("삼성전자 반도체 사업 확대")
                .content("삼성전자가 반도체 생산 시설을 대규모 확대할 계획을 발표했습니다. 이번 투자는...")
                .source("NAVER_FINANCE")
                .url("https://finance.naver.com/news/1")
                .viewCount(150)
                .isHighView(false)
                .sentimentScore(BigDecimal.valueOf(0.65))
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // NewsCreateRequest 테스트 데이터
        createRequest = NewsCreateRequest.builder()
                .title("테스트 뉴스 제목")
                .content("테스트 뉴스 본문입니다. 최소 10자 이상이어야 합니다.")
                .source("NAVER_FINANCE")
                .url("https://finance.naver.com/test")
                .publishedAt(LocalDateTime.now())
                .build();

        // NewsUpdateRequest 테스트 데이터
        updateRequest = NewsUpdateRequest.builder()
                .title("수정된 뉴스 제목")
                .content("수정된 뉴스 본문입니다.")
                .build();
    }

    // ========== GET /api/v1/news - 전체 목록 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/news - getAllNews()")
    class GetAllNewsTests {

        @Test
        @DisplayName("성공 - 뉴스 목록 조회")
        void getAllNews_Success() throws Exception {
            // given
            List<NewsResponse> newsList = List.of(newsResponse);
            PageResponse<NewsResponse> pageResponse = PageResponse.<NewsResponse>builder()
                    .content(newsList)
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.getAllNews(any(Pageable.class))).willReturn(pageResponse);

            // when & then
            mockMvc.perform(
                            get("/api/v1/news")
                                    .param("page", "0")
                                    .param("size", "20")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("삼성전자 반도체 사업 확대"))
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.timestamp").exists());

            then(newsService).should(times(1)).getAllNews(any(Pageable.class));
        }

        @Test
        @DisplayName("빈 목록 - 200 OK")
        void getAllNews_EmptyList() throws Exception {
            // given
            PageResponse<NewsResponse> emptyPage = PageResponse.<NewsResponse>builder()
                    .content(List.of())
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(0L)
                    .totalPages(0)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.getAllNews(any(Pageable.class))).willReturn(emptyPage);

            // when & then
            mockMvc.perform(get("/api/v1/news"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));

            then(newsService).should(times(1)).getAllNews(any(Pageable.class));
        }
    }

    // ========== GET /api/v1/news/{id} - 단건 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/news/{id} - getNews()")
    class GetNewsByIdTests {

        @Test
        @DisplayName("성공 - 뉴스 상세 조회")
        void getNews_Success() throws Exception {
            // given
            given(newsService.getNewsById(testId)).willReturn(newsDetailResponse);

            // when & then
            mockMvc.perform(
                            get("/api/v1/news/{id}", testId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testId.toString()))
                    .andExpect(jsonPath("$.data.title").value("삼성전자 반도체 사업 확대"))
                    .andExpect(jsonPath("$.data.content").exists())
                    .andExpect(jsonPath("$.data.contentLength").value(46))
                    .andExpect(jsonPath("$.data.estimatedReadingTime").value(1));  // 46/200=0 → 최소 1

            then(newsService).should(times(1)).getNewsById(testId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID (404)")
        void getNews_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();
            given(newsService.getNewsById(nonExistentId))
                    .willThrow(ResourceNotFoundException.newsNotFound(nonExistentId.toString()));

            // when & then
            mockMvc.perform(get("/api/v1/news/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());

            then(newsService).should(times(1)).getNewsById(nonExistentId);
        }
    }

    // ========== POST /api/v1/news - 뉴스 생성 ==========

    @Nested
    @DisplayName("POST /api/v1/news - createNews()")
    class CreateNewsTests {

        @Test
        @DisplayName("성공 - 뉴스 생성")
        void createNews_Success() throws Exception {
            // given
            given(newsService.createNews(any(NewsCreateRequest.class)))
                    .willReturn(newsDetailResponse);

            // when & then
            mockMvc.perform(
                            post("/api/v1/news")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(createRequest))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("뉴스가 성공적으로 생성되었습니다."))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.title").exists());

            then(newsService).should(times(1)).createNews(any(NewsCreateRequest.class));
        }

        @Test
        @DisplayName("실패 - 제목이 너무 짧음 (Validation)")
        void createNews_TitleTooShort() throws Exception {
            // given
            NewsCreateRequest invalidRequest = NewsCreateRequest.builder()
                    .title("짧음")  // 5자 미만
                    .content("테스트 뉴스 본문입니다.")
                    .source("NAVER_FINANCE")
                    .url("https://finance.naver.com/test")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/news")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.errors").isArray());

            then(newsService).should(times(0)).createNews(any());
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락 (Validation)")
        void createNews_MissingRequiredFields() throws Exception {
            // given
            NewsCreateRequest invalidRequest = NewsCreateRequest.builder()
                    .title("테스트 뉴스 제목")
                    // content, source, url 누락
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/news")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.errors").isArray());

            then(newsService).should(times(0)).createNews(any());
        }
    }

    // ========== PUT /api/v1/news/{id} - 뉴스 수정 ==========

    @Nested
    @DisplayName("PUT /api/v1/news/{id} - updateNews()")
    class UpdateNewsTests {

        @Test
        @DisplayName("성공 - 뉴스 수정")
        void updateNews_Success() throws Exception {
            // given
            given(newsService.updateNews(eq(testId), any(NewsUpdateRequest.class)))
                    .willReturn(newsDetailResponse);

            // when & then
            mockMvc.perform(
                            put("/api/v1/news/{id}", testId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateRequest))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("뉴스가 성공적으로 수정되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(testId.toString()));

            then(newsService).should(times(1)).updateNews(eq(testId), any(NewsUpdateRequest.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID (404)")
        void updateNews_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();
            given(newsService.updateNews(eq(nonExistentId), any(NewsUpdateRequest.class)))
                    .willThrow(ResourceNotFoundException.newsNotFound(nonExistentId.toString()));

            // when & then
            mockMvc.perform(
                            put("/api/v1/news/{id}", nonExistentId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateRequest))
                    )
                    .andExpect(status().isNotFound());

            then(newsService).should(times(1)).updateNews(eq(nonExistentId), any(NewsUpdateRequest.class));
        }
    }

    // ========== DELETE /api/v1/news/{id} - 뉴스 삭제 ==========

    @Nested
    @DisplayName("DELETE /api/v1/news/{id} - deleteNews()")
    class DeleteNewsTests {

        @Test
        @DisplayName("성공 - 뉴스 삭제")
        void deleteNews_Success() throws Exception {
            // given
            willDoNothing().given(newsService).deleteNews(testId);

            // when & then
            mockMvc.perform(delete("/api/v1/news/{id}", testId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("뉴스가 성공적으로 삭제되었습니다."));

            then(newsService).should(times(1)).deleteNews(testId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID (404)")
        void deleteNews_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();
            willThrow(ResourceNotFoundException.newsNotFound(nonExistentId.toString()))
                    .given(newsService).deleteNews(nonExistentId);

            // when & then
            mockMvc.perform(delete("/api/v1/news/{id}", nonExistentId))
                    .andExpect(status().isNotFound());

            then(newsService).should(times(1)).deleteNews(nonExistentId);
        }
    }

    // ========== GET /api/v1/news/popular - 인기 뉴스 ==========

    @Nested
    @DisplayName("GET /api/v1/news/popular - getPopularNews()")
    class GetPopularNewsTests {

        @Test
        @DisplayName("성공 - 인기 뉴스 조회")
        void getPopularNews_Success() throws Exception {
            // given
            List<NewsResponse> popularNews = List.of(newsResponse);
            PageResponse<NewsResponse> pageResponse = PageResponse.<NewsResponse>builder()
                    .content(popularNews)
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.getHighViewNews(any(Pageable.class))).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/news/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());

            then(newsService).should(times(1)).getHighViewNews(any(Pageable.class));
        }
    }

    // ========== GET /api/v1/news/recent - 최신 뉴스 ==========

    @Nested
    @DisplayName("GET /api/v1/news/recent - getRecentNews()")
    class GetRecentNewsTests {

        @Test
        @DisplayName("성공 - 최신 뉴스 조회")
        void getRecentNews_Success() throws Exception {
            // given
            List<NewsResponse> recentNews = List.of(newsResponse);
            PageResponse<NewsResponse> pageResponse = PageResponse.<NewsResponse>builder()
                    .content(recentNews)
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.getRecentNews(any(Pageable.class))).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/news/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());

            then(newsService).should(times(1)).getRecentNews(any(Pageable.class));
        }
    }

    // ========== GET /api/v1/news/search - 키워드 검색 ==========

    @Nested
    @DisplayName("GET /api/v1/news/search - searchByKeyword()")
    class SearchByKeywordTests {

        @Test
        @DisplayName("성공 - 키워드 검색")
        void searchByKeyword_Success() throws Exception {
            // given
            String keyword = "삼성";
            List<NewsResponse> searchResults = List.of(newsResponse);
            PageResponse<NewsResponse> pageResponse = PageResponse.<NewsResponse>builder()
                    .content(searchResults)
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.searchByKeyword(eq(keyword), any(Pageable.class)))
                    .willReturn(pageResponse);

            // when & then
            mockMvc.perform(
                            get("/api/v1/news/search")
                                    .param("keyword", keyword)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].title").value("삼성전자 반도체 사업 확대"));

            then(newsService).should(times(1)).searchByKeyword(eq(keyword), any(Pageable.class));
        }

        @Test
        @DisplayName("실패 - 키워드 누락 (400)")
        void searchByKeyword_MissingKeyword() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/news/search"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /api/v1/news/search/advanced - 고급 검색 ==========

    @Nested
    @DisplayName("POST /api/v1/news/search/advanced - advancedSearch()")
    class AdvancedSearchTests {

        @Test
        @DisplayName("성공 - 고급 검색")
        void advancedSearch_Success() throws Exception {
            // given
            NewsSearchRequest searchRequest = NewsSearchRequest.builder()
                    .keyword("삼성")
                    .source("NAVER_FINANCE")
                    .minViewCount(100)
                    .page(0)
                    .size(20)
                    .build();

            List<NewsResponse> searchResults = List.of(newsResponse);
            PageResponse<NewsResponse> pageResponse = PageResponse.<NewsResponse>builder()
                    .content(searchResults)
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.searchNews(any(NewsSearchRequest.class))).willReturn(pageResponse);

            // when & then
            mockMvc.perform(
                            post("/api/v1/news/search/advanced")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(searchRequest))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());

            then(newsService).should(times(1)).searchNews(any(NewsSearchRequest.class));
        }
    }

    // ========== GET /api/v1/news/source/{source} - 출처별 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/news/source/{source} - getNewsBySource()")
    class GetNewsBySourceTests {

        @Test
        @DisplayName("성공 - 출처별 뉴스 조회")
        void getNewsBySource_Success() throws Exception {
            // given
            String source = "NAVER_FINANCE";
            List<NewsResponse> sourceNews = List.of(newsResponse);
            PageResponse<NewsResponse> pageResponse = PageResponse.<NewsResponse>builder()
                    .content(sourceNews)
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(newsService.getNewsBySource(eq(source), any(Pageable.class)))
                    .willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/news/source/{source}", source))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());

            then(newsService).should(times(1)).getNewsBySource(eq(source), any(Pageable.class));
        }
    }

    // ========== POST /api/v1/news/{id}/view - 조회수 증가 ==========

    @Nested
    @DisplayName("POST /api/v1/news/{id}/view - incrementViewCount()")
    class IncrementViewCountTests {

        @Test
        @DisplayName("성공 - 조회수 증가")
        void incrementViewCount_Success() throws Exception {
            // given
            NewsDetailResponse updatedNews = NewsDetailResponse.builder()
                    .id(testId)
                    .title("삼성전자 반도체 사업 확대")
                    .content("...")
                    .source("NAVER_FINANCE")
                    .url("https://finance.naver.com/news/1")
                    .viewCount(151)  // 증가됨
                    .isHighView(false)
                    .build();

            given(newsService.incrementViewCount(testId)).willReturn(updatedNews);

            // when & then
            mockMvc.perform(post("/api/v1/news/{id}/view", testId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.viewCount").value(151));

            then(newsService).should(times(1)).incrementViewCount(testId);
        }
    }

    // ========== GET /api/v1/news/exists - URL 중복 확인 ==========

    @Nested
    @DisplayName("GET /api/v1/news/exists - checkUrlExists()")
    class CheckUrlExistsTests {

        @Test
        @DisplayName("성공 - URL 존재함")
        void checkUrlExists_True() throws Exception {
            // given
            String url = "https://finance.naver.com/news/1";
            given(newsService.existsByUrl(url)).willReturn(true);

            // when & then
            mockMvc.perform(
                            get("/api/v1/news/exists")
                                    .param("url", url)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("이미 존재하는 URL입니다."))
                    .andExpect(jsonPath("$.data").value(true));

            then(newsService).should(times(1)).existsByUrl(url);
        }

        @Test
        @DisplayName("성공 - URL 존재하지 않음")
        void checkUrlExists_False() throws Exception {
            // given
            String url = "https://finance.naver.com/news/999";
            given(newsService.existsByUrl(url)).willReturn(false);

            // when & then
            mockMvc.perform(
                            get("/api/v1/news/exists")
                                    .param("url", url)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("사용 가능한 URL입니다."))
                    .andExpect(jsonPath("$.data").value(false));

            then(newsService).should(times(1)).existsByUrl(url);
        }
    }
}
