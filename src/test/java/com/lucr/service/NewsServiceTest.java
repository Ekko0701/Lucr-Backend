package com.lucr.service;

import com.lucr.dto.request.NewsCreateRequest;
import com.lucr.dto.request.NewsSearchRequest;
import com.lucr.dto.request.NewsUpdateRequest;
import com.lucr.dto.response.NewsDetailResponse;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.entity.News;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.NewsMapper;
import com.lucr.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * NewsService 비즈니스 로직 테스트
 *
 * Mock 기반 단위 테스트:
 * - Repository와 Mapper를 Mock으로 대체
 * - 비즈니스 로직과 예외 처리 집중 테스트
 * - 빠른 실행 속도
 *
 * @author Kim Dongjoo
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NewsService 테스트")
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper newsMapper;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News testNews;
    private NewsCreateRequest createRequest;
    private NewsUpdateRequest updateRequest;
    private NewsDetailResponse detailResponse;
    private NewsResponse newsResponse;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        // 테스트용 Entity
        testNews = News.builder()
                .id(testId)
                .title("삼성전자 주가 상승")
                .content("삼성전자의 주가가 오늘 5% 상승했습니다.")
                .source("NAVER_FINANCE")
                .url("https://example.com/news1")
                .viewCount(1500)
                .isHighView(true)
                .sentimentScore(BigDecimal.valueOf(0.8))
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 테스트용 CreateRequest
        createRequest = NewsCreateRequest.builder()
                .title("애플 신제품 출시")
                .content("애플이 새로운 아이폰을 출시했습니다.")
                .source("YAHOO_FINANCE")
                .url("https://example.com/news2")
                .publishedAt(LocalDateTime.now())
                .build();

        // 테스트용 UpdateRequest
        updateRequest = NewsUpdateRequest.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .source("BLOOMBERG")
                .sentimentScore(BigDecimal.valueOf(0.5))
                .build();

        // 테스트용 DetailResponse
        detailResponse = NewsDetailResponse.builder()
                .id(testId)
                .title("삼성전자 주가 상승")
                .content("삼성전자의 주가가 오늘 5% 상승했습니다.")
                .source("NAVER_FINANCE")
                .url("https://example.com/news1")
                .viewCount(1500)
                .isHighView(true)
                .sentimentScore(BigDecimal.valueOf(0.8))
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentLength(25)
                .sentimentLabel("매우 긍정적")
                .estimatedReadingTime(1)
                .build();

        // 테스트용 NewsResponse
        newsResponse = NewsResponse.builder()
                .id(testId)
                .title("삼성전자 주가 상승")
                .contentSummary("삼성전자의 주가가 오늘 5% 상승했습니다.")
                .source("NAVER_FINANCE")
                .url("https://example.com/news1")
                .viewCount(1500)
                .isHighView(true)
                .sentimentScore(BigDecimal.valueOf(0.8))
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== 1. createNews() 테스트 ==========

    @Nested
    @DisplayName("createNews() - 뉴스 생성")
    class CreateNewsTests {

        @Test
        @DisplayName("정상 생성 - 성공")
        void createNews_Success() {
            // given: URL이 중복되지 않고, 정상적인 생성 요청
            given(newsRepository.existsByUrl(createRequest.getUrl())).willReturn(false);
            given(newsMapper.toEntity(createRequest)).willReturn(testNews);
            given(newsRepository.save(testNews)).willReturn(testNews);
            given(newsMapper.toDetailResponse(testNews)).willReturn(detailResponse);

            // when: 뉴스 생성
            NewsDetailResponse result = newsService.createNews(createRequest);

            // then: 뉴스가 생성되고 DetailResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(detailResponse.getTitle());

            // Mock 호출 검증
            then(newsRepository).should(times(1)).existsByUrl(createRequest.getUrl());
            then(newsMapper).should(times(1)).toEntity(createRequest);
            then(newsRepository).should(times(1)).save(testNews);
            then(newsMapper).should(times(1)).toDetailResponse(testNews);
        }

        @Test
        @DisplayName("URL 중복 - DuplicateResourceException")
        void createNews_DuplicateUrl_ThrowsException() {
            // given: URL이 이미 존재함
            given(newsRepository.existsByUrl(createRequest.getUrl())).willReturn(true);

            // when & then: DuplicateResourceException 발생
            assertThatThrownBy(() -> newsService.createNews(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("이미 존재하는 뉴스 URL입니다");

            // save는 호출되지 않아야 함
            then(newsRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Mapper 호출 검증 - toEntity")
        void createNews_CallsMapper_ToEntity() {
            // given
            given(newsRepository.existsByUrl(anyString())).willReturn(false);
            given(newsMapper.toEntity(createRequest)).willReturn(testNews);
            given(newsRepository.save(any(News.class))).willReturn(testNews);
            given(newsMapper.toDetailResponse(any(News.class))).willReturn(detailResponse);

            // when
            newsService.createNews(createRequest);

            // then: Mapper.toEntity()가 정확히 호출됨
            then(newsMapper).should(times(1)).toEntity(createRequest);
        }

        @Test
        @DisplayName("Repository 저장 검증 - save")
        void createNews_CallsRepository_Save() {
            // given
            given(newsRepository.existsByUrl(anyString())).willReturn(false);
            given(newsMapper.toEntity(any())).willReturn(testNews);
            given(newsRepository.save(testNews)).willReturn(testNews);
            given(newsMapper.toDetailResponse(any())).willReturn(detailResponse);

            // when
            newsService.createNews(createRequest);

            // then: Repository.save()가 정확히 호출됨
            then(newsRepository).should(times(1)).save(testNews);
        }

        @Test
        @DisplayName("Mapper 호출 검증 - toDetailResponse")
        void createNews_CallsMapper_ToDetailResponse() {
            // given
            given(newsRepository.existsByUrl(anyString())).willReturn(false);
            given(newsMapper.toEntity(any())).willReturn(testNews);
            given(newsRepository.save(any())).willReturn(testNews);
            given(newsMapper.toDetailResponse(testNews)).willReturn(detailResponse);

            // when
            newsService.createNews(createRequest);

            // then: Mapper.toDetailResponse()가 정확히 호출됨
            then(newsMapper).should(times(1)).toDetailResponse(testNews);
        }
    }

    // ========== 2. getNewsById() 테스트 ==========

    @Nested
    @DisplayName("getNewsById() - ID로 뉴스 조회")
    class GetNewsByIdTests {

        @Test
        @DisplayName("정상 조회 - 성공")
        void getNewsById_Success() {
            // given: 뉴스가 존재함
            given(newsRepository.findById(testId)).willReturn(Optional.of(testNews));
            given(newsMapper.toDetailResponse(testNews)).willReturn(detailResponse);

            // when: ID로 조회
            NewsDetailResponse result = newsService.getNewsById(testId);

            // then: DetailResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testId);
            assertThat(result.getTitle()).isEqualTo(testNews.getTitle());

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findById(testId);
            then(newsMapper).should(times(1)).toDetailResponse(testNews);
        }

        @Test
        @DisplayName("존재하지 않는 ID - ResourceNotFoundException")
        void getNewsById_NotFound_ThrowsException() {
            // given: 뉴스가 존재하지 않음
            UUID nonExistentId = UUID.randomUUID();
            given(newsRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // when & then: ResourceNotFoundException 발생
            assertThatThrownBy(() -> newsService.getNewsById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("뉴스를 찾을 수 없습니다");

            // Mapper는 호출되지 않아야 함
            then(newsMapper).should(never()).toDetailResponse(any());
        }

        @Test
        @DisplayName("Mapper 호출 검증")
        void getNewsById_CallsMapper() {
            // given
            given(newsRepository.findById(testId)).willReturn(Optional.of(testNews));
            given(newsMapper.toDetailResponse(testNews)).willReturn(detailResponse);

            // when
            newsService.getNewsById(testId);

            // then: Mapper.toDetailResponse()가 정확히 호출됨
            then(newsMapper).should(times(1)).toDetailResponse(testNews);
        }
    }

    // ========== 3. getAllNews() 테스트 ==========

    @Nested
    @DisplayName("getAllNews() - 뉴스 목록 조회")
    class GetAllNewsTests {

        @Test
        @DisplayName("정상 조회 - 페이징 성공")
        void getAllNews_Success() {
            // given: 3개의 뉴스가 있음
            List<News> newsList = List.of(testNews, testNews, testNews);
            Page<News> newsPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 3);

            given(newsRepository.findAll(any(Pageable.class))).willReturn(newsPage);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 목록 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getAllNews(pageable);

            // then: PageResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getCurrentPage()).isEqualTo(0);
            assertThat(result.getPageSize()).isEqualTo(10);

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findAll(pageable);
            then(newsMapper).should(times(3)).toResponse(any(News.class));
        }

        @Test
        @DisplayName("빈 목록 - 빈 PageResponse 반환")
        void getAllNews_EmptyList() {
            // given: 뉴스가 없음
            Page<News> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(newsRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

            // when: 목록 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getAllNews(pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);

            // Mapper는 호출되지 않음
            then(newsMapper).should(never()).toResponse(any());
        }
    }

    // ========== 4. updateNews() 테스트 ==========

    @Nested
    @DisplayName("updateNews() - 뉴스 수정")
    class UpdateNewsTests {

        @Test
        @DisplayName("정상 수정 - 성공")
        void updateNews_Success() {
            // given: 뉴스가 존재함
            given(newsRepository.findById(testId)).willReturn(Optional.of(testNews));
            willDoNothing().given(newsMapper).updateEntity(testNews, updateRequest);
            given(newsMapper.toDetailResponse(testNews)).willReturn(detailResponse);

            // when: 뉴스 수정
            NewsDetailResponse result = newsService.updateNews(testId, updateRequest);

            // then: 수정된 DetailResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testId);

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findById(testId);
            then(newsMapper).should(times(1)).updateEntity(testNews, updateRequest);
            then(newsMapper).should(times(1)).toDetailResponse(testNews);
        }

        @Test
        @DisplayName("존재하지 않는 ID - ResourceNotFoundException")
        void updateNews_NotFound_ThrowsException() {
            // given: 뉴스가 존재하지 않음
            UUID nonExistentId = UUID.randomUUID();
            given(newsRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // when & then: ResourceNotFoundException 발생
            assertThatThrownBy(() -> newsService.updateNews(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("뉴스를 찾을 수 없습니다");

            // Mapper는 호출되지 않아야 함
            then(newsMapper).should(never()).updateEntity(any(), any());
        }

        @Test
        @DisplayName("Mapper 호출 검증 - updateEntity")
        void updateNews_CallsMapper_UpdateEntity() {
            // given
            given(newsRepository.findById(testId)).willReturn(Optional.of(testNews));
            willDoNothing().given(newsMapper).updateEntity(testNews, updateRequest);
            given(newsMapper.toDetailResponse(any())).willReturn(detailResponse);

            // when
            newsService.updateNews(testId, updateRequest);

            // then: Mapper.updateEntity()가 정확히 호출됨
            then(newsMapper).should(times(1)).updateEntity(testNews, updateRequest);
        }

        @Test
        @DisplayName("Repository.save() 호출 안함 - 더티 체킹")
        void updateNews_NeverCallsSave_UseDirtyChecking() {
            // given
            given(newsRepository.findById(testId)).willReturn(Optional.of(testNews));
            willDoNothing().given(newsMapper).updateEntity(any(), any());
            given(newsMapper.toDetailResponse(any())).willReturn(detailResponse);

            // when
            newsService.updateNews(testId, updateRequest);

            // then: Repository.save()는 호출되지 않아야 함 (더티 체킹 사용)
            then(newsRepository).should(never()).save(any());
        }
    }

    // ========== 5. deleteNews() 테스트 ==========

    @Nested
    @DisplayName("deleteNews() - 뉴스 삭제")
    class DeleteNewsTests {

        @Test
        @DisplayName("정상 삭제 - 성공")
        void deleteNews_Success() {
            // given: 뉴스가 존재함
            given(newsRepository.existsById(testId)).willReturn(true);
            willDoNothing().given(newsRepository).deleteById(testId);

            // when: 뉴스 삭제
            newsService.deleteNews(testId);

            // then: 삭제 완료
            then(newsRepository).should(times(1)).existsById(testId);
            then(newsRepository).should(times(1)).deleteById(testId);
        }

        @Test
        @DisplayName("존재하지 않는 ID - ResourceNotFoundException")
        void deleteNews_NotFound_ThrowsException() {
            // given: 뉴스가 존재하지 않음
            UUID nonExistentId = UUID.randomUUID();
            given(newsRepository.existsById(nonExistentId)).willReturn(false);

            // when & then: ResourceNotFoundException 발생
            assertThatThrownBy(() -> newsService.deleteNews(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("뉴스를 찾을 수 없습니다");

            // deleteById는 호출되지 않아야 함
            then(newsRepository).should(never()).deleteById(any());
        }

        @Test
        @DisplayName("Repository.deleteById() 호출 검증")
        void deleteNews_CallsRepository_DeleteById() {
            // given
            given(newsRepository.existsById(testId)).willReturn(true);
            willDoNothing().given(newsRepository).deleteById(testId);

            // when
            newsService.deleteNews(testId);

            // then: Repository.deleteById()가 정확히 호출됨
            then(newsRepository).should(times(1)).deleteById(testId);
        }
    }

    // ========== 6. incrementViewCount() 테스트 ==========

    @Nested
    @DisplayName("incrementViewCount() - 조회수 증가")
    class IncrementViewCountTests {

        @Test
        @DisplayName("정상 증가 - 성공")
        void incrementViewCount_Success() {
            // given: 뉴스가 존재함
            given(newsRepository.findById(testId)).willReturn(Optional.of(testNews));
            given(newsMapper.toDetailResponse(testNews)).willReturn(detailResponse);

            // when: 조회수 증가
            NewsDetailResponse result = newsService.incrementViewCount(testId);

            // then: DetailResponse 반환
            assertThat(result).isNotNull();

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findById(testId);
            then(newsMapper).should(times(1)).toDetailResponse(testNews);
        }

        @Test
        @DisplayName("존재하지 않는 ID - ResourceNotFoundException")
        void incrementViewCount_NotFound_ThrowsException() {
            // given: 뉴스가 존재하지 않음
            UUID nonExistentId = UUID.randomUUID();
            given(newsRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // when & then: ResourceNotFoundException 발생
            assertThatThrownBy(() -> newsService.incrementViewCount(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("뉴스를 찾을 수 없습니다");

            // Mapper는 호출되지 않아야 함
            then(newsMapper).should(never()).toDetailResponse(any());
        }

        @Test
        @DisplayName("Entity.incrementViewCount() 호출 검증")
        void incrementViewCount_CallsEntity_IncrementViewCount() {
            // given
            News spyNews = org.mockito.Mockito.spy(testNews);
            given(newsRepository.findById(testId)).willReturn(Optional.of(spyNews));
            given(newsMapper.toDetailResponse(any())).willReturn(detailResponse);

            // when
            newsService.incrementViewCount(testId);

            // then: Entity의 incrementViewCount()가 호출됨
            then(spyNews).should(times(1)).incrementViewCount();
        }
    }

    // ========== 7. getHighViewNews() 테스트 ==========

    @Nested
    @DisplayName("getHighViewNews() - 인기 뉴스 조회")
    class GetHighViewNewsTests {

        @Test
        @DisplayName("정상 조회 - 조회수 높은 순")
        void getHighViewNews_Success() {
            // given: 인기 뉴스가 있음
            List<News> newsList = List.of(testNews, testNews);
            Page<News> newsPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 2);

            given(newsRepository.findAllByOrderByViewCountDesc(any(Pageable.class))).willReturn(newsPage);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 인기 뉴스 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getHighViewNews(pageable);

            // then: PageResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findAllByOrderByViewCountDesc(pageable);
            then(newsMapper).should(times(2)).toResponse(any(News.class));
        }

        @Test
        @DisplayName("빈 목록 - 빈 PageResponse 반환")
        void getHighViewNews_EmptyList() {
            // given: 인기 뉴스가 없음
            Page<News> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(newsRepository.findAllByOrderByViewCountDesc(any(Pageable.class))).willReturn(emptyPage);

            // when: 인기 뉴스 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getHighViewNews(pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ========== 8. getRecentNews() 테스트 ==========

    @Nested
    @DisplayName("getRecentNews() - 최신 뉴스 조회")
    class GetRecentNewsTests {

        @Test
        @DisplayName("정상 조회 - 생성일 최신순")
        void getRecentNews_Success() {
            // given: 최신 뉴스가 있음
            List<News> newsList = List.of(testNews, testNews);
            Page<News> newsPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 2);

            given(newsRepository.findAll(any(Pageable.class))).willReturn(newsPage);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 최신 뉴스 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getRecentNews(pageable);

            // then: PageResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            // Mock 호출 검증 (createdAt DESC 정렬 Pageable로 호출됨)
            then(newsRepository).should(times(1)).findAll(any(Pageable.class));
            then(newsMapper).should(times(2)).toResponse(any(News.class));
        }

        @Test
        @DisplayName("빈 목록 - 빈 PageResponse 반환")
        void getRecentNews_EmptyList() {
            // given: 최신 뉴스가 없음
            Page<News> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(newsRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

            // when: 최신 뉴스 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getRecentNews(pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ========== 9. existsByUrl() 테스트 ==========

    @Nested
    @DisplayName("existsByUrl() - URL 중복 체크")
    class ExistsByUrlTests {

        @Test
        @DisplayName("URL 존재 - true 반환")
        void existsByUrl_Exists_ReturnsTrue() {
            // given: URL이 존재함
            String url = "https://example.com/news1";
            given(newsRepository.existsByUrl(url)).willReturn(true);

            // when: URL 중복 체크
            boolean result = newsService.existsByUrl(url);

            // then: true 반환
            assertThat(result).isTrue();
            then(newsRepository).should(times(1)).existsByUrl(url);
        }

        @Test
        @DisplayName("URL 존재하지 않음 - false 반환")
        void existsByUrl_NotExists_ReturnsFalse() {
            // given: URL이 존재하지 않음
            String url = "https://example.com/nonexistent";
            given(newsRepository.existsByUrl(url)).willReturn(false);

            // when: URL 중복 체크
            boolean result = newsService.existsByUrl(url);

            // then: false 반환
            assertThat(result).isFalse();
            then(newsRepository).should(times(1)).existsByUrl(url);
        }
    }

    // ========== 10. getNewsBySource() 테스트 ==========

    @Nested
    @DisplayName("getNewsBySource() - 출처별 뉴스 조회")
    class GetNewsBySourceTests {

        @Test
        @DisplayName("정상 조회 - 수동 페이징")
        void getNewsBySource_Success() {
            // given: 출처별 뉴스가 5개 있음
            List<News> newsList = List.of(testNews, testNews, testNews, testNews, testNews);
            given(newsRepository.findBySource("NAVER_FINANCE")).willReturn(newsList);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 첫 페이지 3개 조회
            Pageable pageable = PageRequest.of(0, 3);
            PageResponse<NewsResponse> result = newsService.getNewsBySource("NAVER_FINANCE", pageable);

            // then: 3개만 반환 (수동 페이징)
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getIsFirst()).isTrue();
            assertThat(result.getIsLast()).isFalse();
            assertThat(result.getHasNext()).isTrue();

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findBySource("NAVER_FINANCE");
            then(newsMapper).should(times(3)).toResponse(any(News.class));
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        void getNewsBySource_SecondPage() {
            // given: 출처별 뉴스가 5개 있음
            List<News> newsList = List.of(testNews, testNews, testNews, testNews, testNews);
            given(newsRepository.findBySource("NAVER_FINANCE")).willReturn(newsList);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 두 번째 페이지 3개 조회
            Pageable pageable = PageRequest.of(1, 3);
            PageResponse<NewsResponse> result = newsService.getNewsBySource("NAVER_FINANCE", pageable);

            // then: 2개만 반환 (마지막 페이지)
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getIsFirst()).isFalse();
            assertThat(result.getIsLast()).isTrue();
            assertThat(result.getHasNext()).isFalse();
            assertThat(result.getHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("빈 목록 - 빈 PageResponse 반환")
        void getNewsBySource_EmptyList() {
            // given: 해당 출처의 뉴스가 없음
            given(newsRepository.findBySource("BLOOMBERG")).willReturn(List.of());

            // when: 출처별 조회
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.getNewsBySource("BLOOMBERG", pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("페이징 범위 초과 - 빈 목록 반환")
        void getNewsBySource_OutOfRange() {
            // given: 뉴스가 3개만 있음
            List<News> newsList = List.of(testNews, testNews, testNews);
            given(newsRepository.findBySource("NAVER_FINANCE")).willReturn(newsList);

            // when: 10번째 페이지 조회 (범위 초과)
            Pageable pageable = PageRequest.of(10, 10);
            PageResponse<NewsResponse> result = newsService.getNewsBySource("NAVER_FINANCE", pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }

    // ========== 11. searchByKeyword() 테스트 ==========

    @Nested
    @DisplayName("searchByKeyword() - 키워드 검색")
    class SearchByKeywordTests {

        @Test
        @DisplayName("정상 검색 - 수동 페이징")
        void searchByKeyword_Success() {
            // given: 검색 결과가 4개 있음
            List<News> newsList = List.of(testNews, testNews, testNews, testNews);
            given(newsRepository.searchByKeyword("주가")).willReturn(newsList);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 첫 페이지 3개 조회
            Pageable pageable = PageRequest.of(0, 3);
            PageResponse<NewsResponse> result = newsService.searchByKeyword("주가", pageable);

            // then: 3개만 반환 (수동 페이징)
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(4);
            assertThat(result.getTotalPages()).isEqualTo(2);

            // Mock 호출 검증
            then(newsRepository).should(times(1)).searchByKeyword("주가");
            then(newsMapper).should(times(3)).toResponse(any(News.class));
        }

        @Test
        @DisplayName("검색 결과 없음 - 빈 PageResponse 반환")
        void searchByKeyword_NoResults() {
            // given: 검색 결과가 없음
            given(newsRepository.searchByKeyword("존재하지않는키워드")).willReturn(List.of());

            // when: 검색
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<NewsResponse> result = newsService.searchByKeyword("존재하지않는키워드", pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        void searchByKeyword_SecondPage() {
            // given: 검색 결과가 4개 있음
            List<News> newsList = List.of(testNews, testNews, testNews, testNews);
            given(newsRepository.searchByKeyword("주가")).willReturn(newsList);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 두 번째 페이지 3개 조회
            Pageable pageable = PageRequest.of(1, 3);
            PageResponse<NewsResponse> result = newsService.searchByKeyword("주가", pageable);

            // then: 1개만 반환 (마지막 페이지)
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getIsLast()).isTrue();
        }

        @Test
        @DisplayName("페이징 범위 초과 - 빈 목록 반환")
        void searchByKeyword_OutOfRange() {
            // given: 검색 결과가 2개만 있음
            List<News> newsList = List.of(testNews, testNews);
            given(newsRepository.searchByKeyword("주가")).willReturn(newsList);

            // when: 10번째 페이지 조회 (범위 초과)
            Pageable pageable = PageRequest.of(10, 10);
            PageResponse<NewsResponse> result = newsService.searchByKeyword("주가", pageable);

            // then: 빈 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    // ========== 12. searchNews() 테스트 ==========

    @Nested
    @DisplayName("searchNews() - 복합 조건 검색")
    class SearchNewsTests {

        @Test
        @DisplayName("검색 조건 있음 - 정상 조회")
        void searchNews_WithKeyword_Success() {
            // given: 검색 조건과 결과가 있음
            NewsSearchRequest searchRequest = NewsSearchRequest.builder()
                    .keyword("주가")
                    .source("NAVER_FINANCE")
                    .page(0)
                    .size(10)
                    .build();

            List<News> newsList = List.of(testNews, testNews);
            Page<News> newsPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 2);

            given(newsRepository.findAll(any(Pageable.class))).willReturn(newsPage);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 검색
            PageResponse<NewsResponse> result = newsService.searchNews(searchRequest);

            // then: PageResponse 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            // Mock 호출 검증
            then(newsRepository).should(times(1)).findAll(any(Pageable.class));
            then(newsMapper).should(times(2)).toResponse(any(News.class));
        }

        @Test
        @DisplayName("검색 조건 없음 - 전체 조회")
        void searchNews_WithoutKeyword_ReturnsAll() {
            // given: 검색 조건이 없음 (빈 키워드)
            NewsSearchRequest searchRequest = NewsSearchRequest.builder()
                    .keyword("")
                    .page(0)
                    .size(10)
                    .build();

            List<News> newsList = List.of(testNews, testNews, testNews);
            Page<News> newsPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 3);

            given(newsRepository.findAll(any(Pageable.class))).willReturn(newsPage);
            given(newsMapper.toResponse(any(News.class))).willReturn(newsResponse);

            // when: 검색
            PageResponse<NewsResponse> result = newsService.searchNews(searchRequest);

            // then: 전체 목록 반환
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
        }
    }
}
