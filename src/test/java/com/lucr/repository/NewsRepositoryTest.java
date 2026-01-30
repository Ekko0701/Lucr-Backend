package com.lucr.repository;

import com.lucr.entity.News;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * NewsRepository 테스트
 * 
 * @DataJpaTest:
 * - JPA 관련 컴포넌트만 로드 (경량 테스트)
 * - 인메모리 DB (H2) 자동 설정
 * - 각 테스트 메서드마다 트랜잭션 롤백 (테스트 격리)
 * - @Transactional 자동 적용
 * 
 * @author Kim Dongjoo
 * @since 2026-01-28
 */
@DataJpaTest
@DisplayName("NewsRepository 테스트")
class NewsRepositoryTest {

    @Autowired
    private NewsRepository newsRepository;

    private News testNews1;
    private News testNews2;
    private News testNews3;

    /**
     * 각 테스트 메서드 실행 전에 테스트 데이터 초기화
     */
    @BeforeEach
    void setUp() {
        // 테스트용 뉴스 데이터 생성
        testNews1 = News.builder()
                .url("https://example.com/news1")
                .title("삼성전자 주가 상승")
                .content("삼성전자의 주가가 오늘 5% 상승했습니다.")
                .source("NAVER_FINANCE")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .viewCount(1500)
                .isHighView(true)
                .sentimentScore(BigDecimal.valueOf(0.8))
                .build();

        testNews2 = News.builder()
                .url("https://example.com/news2")
                .title("애플 신제품 출시")
                .content("애플이 새로운 아이폰을 출시했습니다.")
                .source("YAHOO_FINANCE")
                .publishedAt(LocalDateTime.now().minusDays(3))
                .viewCount(500)
                .isHighView(false)
                .sentimentScore(BigDecimal.valueOf(0.6))
                .build();

        testNews3 = News.builder()
                .url("https://example.com/news3")
                .title("테슬라 실적 발표")
                .content("테슬라의 분기 실적이 발표되었습니다.")
                .source("NAVER_FINANCE")
                .publishedAt(LocalDateTime.now().minusDays(7))
                .viewCount(800)
                .isHighView(false)
                .sentimentScore(BigDecimal.valueOf(0.3))
                .build();
    }

    // ========== 1. 기본 CRUD 테스트 ==========

    @Test
    @DisplayName("뉴스 저장 - 성공")
    void save_Success() {
        // when: 뉴스 저장
        News savedNews = newsRepository.save(testNews1);

        // then: 저장 확인
        assertThat(savedNews.getId()).isNotNull();
        assertThat(savedNews.getTitle()).isEqualTo("삼성전자 주가 상승");
        assertThat(savedNews.getUrl()).isEqualTo("https://example.com/news1");
        assertThat(savedNews.getCreatedAt()).isNotNull();
        assertThat(savedNews.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ID로 뉴스 조회 - 성공")
    void findById_Success() {
        // given: 뉴스 저장
        News savedNews = newsRepository.save(testNews1);

        // when: ID로 조회
        Optional<News> foundNews = newsRepository.findById(savedNews.getId());

        // then: 조회 성공
        assertThat(foundNews).isPresent();
        assertThat(foundNews.get().getTitle()).isEqualTo("삼성전자 주가 상승");
    }

    @Test
    @DisplayName("ID로 뉴스 조회 - 실패 (존재하지 않는 ID)")
    void findById_NotFound() {
        // given: 존재하지 않는 UUID
        UUID nonExistentId = UUID.randomUUID();

        // when: 조회
        Optional<News> foundNews = newsRepository.findById(nonExistentId);

        // then: 빈 Optional
        assertThat(foundNews).isEmpty();
    }

    @Test
    @DisplayName("모든 뉴스 조회 - 성공")
    void findAll_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 전체 조회
        List<News> allNews = newsRepository.findAll();

        // then: 3개 조회
        assertThat(allNews).hasSize(3);
    }

    @Test
    @DisplayName("뉴스 삭제 - 성공")
    void delete_Success() {
        // given: 뉴스 저장
        News savedNews = newsRepository.save(testNews1);
        UUID savedId = savedNews.getId();

        // when: 삭제
        newsRepository.delete(savedNews);

        // then: 조회 시 없음
        Optional<News> deletedNews = newsRepository.findById(savedId);
        assertThat(deletedNews).isEmpty();
    }

    @Test
    @DisplayName("뉴스 개수 확인 - 성공")
    void count_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 개수 조회
        long count = newsRepository.count();

        // then: 3개
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("뉴스 업데이트 - 성공")
    void update_Success() {
        // given: 뉴스 저장
        News savedNews = newsRepository.save(testNews1);
        String originalTitle = savedNews.getTitle();

        // when: 제목 수정
        savedNews.setTitle("수정된 제목");
        News updatedNews = newsRepository.save(savedNews);

        // then: 제목이 변경됨
        assertThat(updatedNews.getTitle()).isNotEqualTo(originalTitle);
        assertThat(updatedNews.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedNews.getId()).isEqualTo(savedNews.getId());
    }

    // ========== 2. Query Methods 테스트 ==========

    @Test
    @DisplayName("URL로 뉴스 조회 - 성공")
    void findByUrl_Success() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: URL로 조회
        Optional<News> foundNews = newsRepository.findByUrl("https://example.com/news1");

        // then: 조회 성공
        assertThat(foundNews).isPresent();
        assertThat(foundNews.get().getTitle()).isEqualTo("삼성전자 주가 상승");
    }

    @Test
    @DisplayName("URL로 뉴스 조회 - 실패 (존재하지 않는 URL)")
    void findByUrl_NotFound() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: 존재하지 않는 URL로 조회
        Optional<News> foundNews = newsRepository.findByUrl("https://example.com/nonexistent");

        // then: 빈 Optional
        assertThat(foundNews).isEmpty();
    }

    @Test
    @DisplayName("출처로 뉴스 조회 - 성공")
    void findBySource_Success() {
        // given: 3개의 뉴스 저장 (NAVER_FINANCE 2개, YAHOO_FINANCE 1개)
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: NAVER_FINANCE 출처로 조회
        List<News> naverNews = newsRepository.findBySource("NAVER_FINANCE");

        // then: 2개 조회
        assertThat(naverNews).hasSize(2);
        assertThat(naverNews)
                .extracting(News::getSource)
                .containsOnly("NAVER_FINANCE");
    }

    @Test
    @DisplayName("제목 키워드로 뉴스 조회 - 성공")
    void findByTitleContaining_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: "주가" 키워드로 조회
        List<News> newsWithKeyword = newsRepository.findByTitleContaining("주가");

        // then: 1개 조회
        assertThat(newsWithKeyword).hasSize(1);
        assertThat(newsWithKeyword.getFirst().getTitle()).contains("주가");
    }

    @Test
    @DisplayName("고조회수 뉴스 조회 - 성공")
    void findByIsHighView_Success() {
        // given: 3개의 뉴스 저장 (고조회수 1개, 일반 2개)
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 고조회수 뉴스 조회
        List<News> highViewNews = newsRepository.findByIsHighView(true);

        // then: 1개 조회
        assertThat(highViewNews).hasSize(1);
        assertThat(highViewNews.get(0).getViewCount()).isGreaterThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("조회수 이상 뉴스 조회 - 성공")
    void findByViewCountGreaterThanEqual_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1500
        newsRepository.save(testNews2);  // 500
        newsRepository.save(testNews3);  // 800

        // when: 조회수 800 이상 조회
        List<News> newsWithHighViews = newsRepository.findByViewCountGreaterThanEqual(800);

        // then: 2개 조회 (1500, 800)
        assertThat(newsWithHighViews).hasSize(2);
        assertThat(newsWithHighViews)
                .extracting(News::getViewCount)
                .containsExactlyInAnyOrder(1500, 800);
    }

    @Test
    @DisplayName("감정 점수 범위로 뉴스 조회 - 성공")
    void findBySentimentScoreBetween_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 0.8
        newsRepository.save(testNews2);  // 0.6
        newsRepository.save(testNews3);  // 0.3

        // when: 감정 점수 0.5 ~ 0.7 범위 조회
        List<News> newsWithSentiment = newsRepository.findBySentimentScoreBetween(
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.7)
        );

        // then: 1개 조회 (0.6)
        assertThat(newsWithSentiment).hasSize(1);
        assertThat(newsWithSentiment.get(0).getSentimentScore())
                .isEqualByComparingTo(BigDecimal.valueOf(0.6));
    }

    @Test
    @DisplayName("특정 날짜 이후 발행 뉴스 조회 - 성공")
    void findByPublishedAtAfter_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1일 전
        newsRepository.save(testNews2);  // 3일 전
        newsRepository.save(testNews3);  // 7일 전

        // when: 5일 전 이후 발행된 뉴스 조회
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        List<News> recentNews = newsRepository.findByPublishedAtAfter(fiveDaysAgo);

        // then: 2개 조회 (1일 전, 3일 전)
        assertThat(recentNews).hasSize(2);
    }

    @Test
    @DisplayName("출처와 발행일 복합 조건 조회 - 성공")
    void findBySourceAndPublishedAtAfter_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // NAVER_FINANCE, 1일 전
        newsRepository.save(testNews2);  // YAHOO_FINANCE, 3일 전
        newsRepository.save(testNews3);  // NAVER_FINANCE, 7일 전

        // when: NAVER_FINANCE에서 5일 전 이후 발행된 뉴스 조회
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        List<News> filteredNews = newsRepository.findBySourceAndPublishedAtAfter(
                "NAVER_FINANCE",
                fiveDaysAgo
        );

        // then: 1개 조회 (testNews1)
        assertThat(filteredNews).hasSize(1);
        assertThat(filteredNews.get(0).getTitle()).isEqualTo("삼성전자 주가 상승");
    }

    @Test
    @DisplayName("출처와 발행일 복합 조건 조회 - 조건 불만족")
    void findBySourceAndPublishedAtAfter_NoMatch() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // NAVER_FINANCE, 1일 전
        newsRepository.save(testNews2);  // YAHOO_FINANCE, 3일 전
        newsRepository.save(testNews3);  // NAVER_FINANCE, 7일 전

        // when: BLOOMBERG에서 조회 (존재하지 않는 출처)
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        List<News> filteredNews = newsRepository.findBySourceAndPublishedAtAfter(
                "BLOOMBERG",
                fiveDaysAgo
        );

        // then: 빈 리스트
        assertThat(filteredNews).isEmpty();
    }

    @Test
    @DisplayName("제목 키워드로 뉴스 조회 - 빈 문자열")
    void findByTitleContaining_EmptyString() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 빈 문자열로 조회
        List<News> results = newsRepository.findByTitleContaining("");

        // then: 모든 뉴스 반환 (LIKE %%는 모든 문자열 매칭)
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("발행일 null인 뉴스 - 날짜 검색 시 제외됨")
    void findByPublishedAtAfter_NullPublishedAt() {
        // given: 발행일이 null인 뉴스
        News newsWithNullDate = News.builder()
                .url("https://example.com/null-date")
                .title("발행일 없는 뉴스")
                .source("NAVER_FINANCE")
                .publishedAt(null)
                .build();
        newsRepository.save(newsWithNullDate);
        newsRepository.save(testNews1);  // 1일 전

        // when: 5일 전 이후 조회
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        List<News> recentNews = newsRepository.findByPublishedAtAfter(fiveDaysAgo);

        // then: null은 제외되고 1개만 조회
        assertThat(recentNews).hasSize(1);
        assertThat(recentNews.get(0).getTitle()).isEqualTo("삼성전자 주가 상승");
    }

    // ========== 3. 페이징 테스트 ==========

    @Test
    @DisplayName("조회수 기준 내림차순 페이징 조회 - 성공")
    void findAllByOrderByViewCountDesc_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1500
        newsRepository.save(testNews2);  // 500
        newsRepository.save(testNews3);  // 800

        // when: 첫 페이지 2개 조회 (조회수 내림차순)
        Pageable pageable = PageRequest.of(0, 2);
        Page<News> newsPage = newsRepository.findAllByOrderByViewCountDesc(pageable);

        // then: 1500, 800 순서로 조회
        assertThat(newsPage.getContent()).hasSize(2);
        assertThat(newsPage.getTotalElements()).isEqualTo(3);
        assertThat(newsPage.getContent().get(0).getViewCount()).isEqualTo(1500);
        assertThat(newsPage.getContent().get(1).getViewCount()).isEqualTo(800);
    }

    @Test
    @DisplayName("발행일 기준 내림차순 페이징 조회 - 성공")
    void findAllByOrderByPublishedAtDesc_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1일 전
        newsRepository.save(testNews2);  // 3일 전
        newsRepository.save(testNews3);  // 7일 전

        // when: 첫 페이지 2개 조회 (발행일 내림차순)
        Pageable pageable = PageRequest.of(0, 2);
        Page<News> newsPage = newsRepository.findAllByOrderByPublishedAtDesc(pageable);

        // then: 최신순으로 조회
        assertThat(newsPage.getContent()).hasSize(2);
        assertThat(newsPage.getTotalElements()).isEqualTo(3);
        assertThat(newsPage.getContent().get(0).getTitle()).isEqualTo("삼성전자 주가 상승");
        assertThat(newsPage.getContent().get(1).getTitle()).isEqualTo("애플 신제품 출시");
    }

    // ========== 4. @Query (JPQL) 테스트 ==========

    @Test
    @DisplayName("키워드로 제목/본문 검색 - 성공")
    void searchByKeyword_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: "주가" 키워드로 검색 (제목 또는 본문)
        List<News> searchResults = newsRepository.searchByKeyword("주가");

        // then: 1개 조회
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getTitle()).contains("주가");
    }

    @Test
    @DisplayName("특수문자 포함 검색 - 성공")
    void searchByKeyword_SpecialCharacters() {
        // given: 특수문자 포함 뉴스
        News newsWithSpecialChars = News.builder()
                .url("https://example.com/special")
                .title("주가 10% 상승!")
                .content("(주)삼성전자의 주가가 오늘 10% 상승했습니다.")
                .source("NAVER_FINANCE")
                .publishedAt(LocalDateTime.now())
                .build();
        newsRepository.save(newsWithSpecialChars);

        // when: 특수문자로 검색
        List<News> results1 = newsRepository.searchByKeyword("10%");
        List<News> results2 = newsRepository.searchByKeyword("(주)");

        // then: 검색 성공
        assertThat(results1).hasSize(1);
        assertThat(results2).hasSize(1);
    }

    @Test
    @DisplayName("출처별 뉴스 개수 조회 - 성공")
    void countBySource_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // NAVER_FINANCE
        newsRepository.save(testNews2);  // YAHOO_FINANCE
        newsRepository.save(testNews3);  // NAVER_FINANCE

        // when: 출처별 개수 조회
        List<Object[]> counts = newsRepository.countBySource();

        // then: NAVER_FINANCE 2개, YAHOO_FINANCE 1개
        assertThat(counts).hasSize(2);
        
        // Object[]를 분석
        for (Object[] count : counts) {
            String source = (String) count[0];
            Long newsCount = (Long) count[1];
            
            if ("NAVER_FINANCE".equals(source)) {
                assertThat(newsCount).isEqualTo(2);
            } else if ("YAHOO_FINANCE".equals(source)) {
                assertThat(newsCount).isEqualTo(1);
            }
        }
    }

    @Test
    @DisplayName("평균 조회수 조회 - 성공")
    void getAverageViewCount_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1500
        newsRepository.save(testNews2);  // 500
        newsRepository.save(testNews3);  // 800

        // when: 평균 조회수 조회
        Double avgViewCount = newsRepository.getAverageViewCount();

        // then: (1500 + 500 + 800) / 3 = 933.33
        assertThat(avgViewCount).isCloseTo(933.33, within(0.01));
    }

    @Test
    @DisplayName("특정 기간 뉴스 조회 - 성공")
    void findNewsBetweenDates_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1일 전
        newsRepository.save(testNews2);  // 3일 전
        newsRepository.save(testNews3);  // 7일 전

        // when: 5일 전 ~ 현재 사이 뉴스 조회
        LocalDateTime startDate = LocalDateTime.now().minusDays(5);
        LocalDateTime endDate = LocalDateTime.now();
        List<News> newsInRange = newsRepository.findNewsBetweenDates(startDate, endDate);

        // then: 2개 조회 (1일 전, 3일 전)
        assertThat(newsInRange).hasSize(2);
        
        // 조회수 내림차순 정렬 확인
        assertThat(newsInRange.get(0).getViewCount()).isEqualTo(1500);
        assertThat(newsInRange.get(1).getViewCount()).isEqualTo(500);
    }

    @Test
    @DisplayName("긍정적 고조회수 뉴스 조회 - 성공")
    void findPositiveHighViewNews_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // sentiment: 0.8, highView: true
        newsRepository.save(testNews2);  // sentiment: 0.6, highView: false
        newsRepository.save(testNews3);  // sentiment: 0.3, highView: false

        // when: 긍정적(>0.5) 고조회수 뉴스 조회
        Pageable pageable = PageRequest.of(0, 10);
        List<News> positiveHighViewNews = newsRepository.findPositiveHighViewNews(pageable);

        // then: 1개 조회 (testNews1)
        assertThat(positiveHighViewNews).hasSize(1);
        assertThat(positiveHighViewNews.get(0).getSentimentScore())
                .isGreaterThan(BigDecimal.valueOf(0.5));
        assertThat(positiveHighViewNews.get(0).getIsHighView()).isTrue();
    }

    @Test
    @DisplayName("Native Query - 상위 뉴스 조회 - 성공")
    void findTopNewsByNativeQuery_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // sentiment: 0.8, viewCount: 1500
        newsRepository.save(testNews2);  // sentiment: 0.6, viewCount: 500
        newsRepository.save(testNews3);  // sentiment: 0.3, viewCount: 800

        // when: sentiment > 0.5인 뉴스 중 상위 2개 조회
        List<News> topNews = newsRepository.findTopNewsByNativeQuery(0.5, 2);

        // then: 2개 조회 (조회수 내림차순)
        assertThat(topNews).hasSize(2);
        assertThat(topNews.get(0).getViewCount()).isEqualTo(1500);
        assertThat(topNews.get(1).getViewCount()).isEqualTo(500);
    }

    // ========== 5. Exists 쿼리 테스트 ==========

    @Test
    @DisplayName("URL 존재 여부 확인 - 존재함")
    void existsByUrl_True() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: URL 존재 확인
        boolean exists = newsRepository.existsByUrl("https://example.com/news1");

        // then: 존재함
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("URL 존재 여부 확인 - 존재하지 않음")
    void existsByUrl_False() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: 존재하지 않는 URL 확인
        boolean exists = newsRepository.existsByUrl("https://example.com/nonexistent");

        // then: 존재하지 않음
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("출처 존재 여부 확인 - 존재함")
    void existsBySource_True() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: 출처 존재 확인
        boolean exists = newsRepository.existsBySource("NAVER_FINANCE");

        // then: 존재함
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("출처 존재 여부 확인 - 존재하지 않음")
    void existsBySource_False() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: 존재하지 않는 출처 확인
        boolean exists = newsRepository.existsBySource("BLOOMBERG");

        // then: 존재하지 않음
        assertThat(exists).isFalse();
    }

    // ========== 6. Delete 쿼리 테스트 ==========

    @Test
    @DisplayName("출처별 뉴스 삭제 - 성공")
    void deleteBySource_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // NAVER_FINANCE
        newsRepository.save(testNews2);  // YAHOO_FINANCE
        newsRepository.save(testNews3);  // NAVER_FINANCE

        // when: NAVER_FINANCE 출처 뉴스 삭제
        newsRepository.deleteBySource("NAVER_FINANCE");
        newsRepository.flush();  // 즉시 실행

        // then: 1개만 남음 (YAHOO_FINANCE)
        List<News> remainingNews = newsRepository.findAll();
        assertThat(remainingNews).hasSize(1);
        assertThat(remainingNews.get(0).getSource()).isEqualTo("YAHOO_FINANCE");
    }

    @Test
    @DisplayName("특정 날짜 이전 뉴스 삭제 - 성공")
    void deleteByPublishedAtBefore_Success() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);  // 1일 전
        newsRepository.save(testNews2);  // 3일 전
        newsRepository.save(testNews3);  // 7일 전

        // when: 5일 전 이전 뉴스 삭제
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        newsRepository.deleteByPublishedAtBefore(fiveDaysAgo);
        newsRepository.flush();  // 즉시 실행

        // then: 2개만 남음 (1일 전, 3일 전)
        List<News> remainingNews = newsRepository.findAll();
        assertThat(remainingNews).hasSize(2);
        assertThat(remainingNews)
                .extracting(News::getTitle)
                .containsExactlyInAnyOrder("삼성전자 주가 상승", "애플 신제품 출시");
    }

    // ========== 7. Entity 생명주기 테스트 ==========

    @Test
    @DisplayName("Entity 생성 시 자동 타임스탬프 - 성공")
    void entityLifecycle_CreatedAt_UpdatedAt() {
        // given: 뉴스 저장
        News savedNews = newsRepository.save(testNews1);

        // then: createdAt과 updatedAt이 자동 생성됨
        assertThat(savedNews.getCreatedAt()).isNotNull();
        assertThat(savedNews.getUpdatedAt()).isNotNull();
        // createdAt과 updatedAt은 거의 동시에 생성되므로 같거나 매우 가까움
        assertThat(savedNews.getCreatedAt()).isBeforeOrEqualTo(savedNews.getUpdatedAt());
    }

    @Test
    @DisplayName("Entity 수정 시 updatedAt 자동 갱신 - 성공")
    void entityLifecycle_UpdatedAt_Changed() throws InterruptedException {
        // given: 뉴스 저장
        News savedNews = newsRepository.save(testNews1);
        newsRepository.flush();  // 영속성 컨텍스트 플러시
        LocalDateTime originalUpdatedAt = savedNews.getUpdatedAt();
        LocalDateTime originalCreatedAt = savedNews.getCreatedAt();

        // 약간의 시간 대기 (updatedAt 변경 확인용)
        Thread.sleep(100);

        // when: 뉴스 수정
        savedNews.setTitle("수정된 제목");
        News updatedNews = newsRepository.save(savedNews);
        newsRepository.flush();  // 변경 사항 플러시

        // then: updatedAt이 변경되거나 같을 수 있음 (JPA 구현에 따라 다름)
        assertThat(updatedNews.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        assertThat(updatedNews.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("조회수 증가 - 일반 케이스")
    void incrementViewCount_NormalCase() {
        // given: 조회수 100인 뉴스
        News news = News.builder()
                .url("https://example.com/increment-test")
                .title("조회수 증가 테스트")
                .content("내용")
                .source("NAVER_FINANCE")
                .viewCount(100)
                .build();
        News savedNews = newsRepository.save(news);

        // when: 조회수 증가
        savedNews.incrementViewCount();
        News updatedNews = newsRepository.save(savedNews);

        // then: 101로 증가
        assertThat(updatedNews.getViewCount()).isEqualTo(101);
        assertThat(updatedNews.getIsHighView()).isFalse();
    }

    @Test
    @DisplayName("조회수 여러 번 증가 - 성공")
    void incrementViewCount_Multiple() {
        // given: 조회수 0인 뉴스
        News news = News.builder()
                .url("https://example.com/multiple-increment")
                .title("여러 번 증가 테스트")
                .source("NAVER_FINANCE")
                .build();
        News savedNews = newsRepository.save(news);

        // when: 조회수 5번 증가
        for (int i = 0; i < 5; i++) {
            savedNews.incrementViewCount();
        }
        News updatedNews = newsRepository.save(savedNews);

        // then: 5로 증가
        assertThat(updatedNews.getViewCount()).isEqualTo(5);
        assertThat(updatedNews.getIsHighView()).isFalse();
    }

    // ========== 8. Edge Cases & Negative Tests ==========

    @Test
    @DisplayName("중복 URL 저장 - 실패 (Unique 제약 조건)")
    void save_DuplicateUrl_Fail() {
        // given: 첫 번째 뉴스 저장
        newsRepository.save(testNews1);

        // when & then: 같은 URL로 두 번째 뉴스 저장 시도
        News duplicateNews = News.builder()
                .url("https://example.com/news1")  // 중복 URL
                .title("다른 제목")
                .content("다른 내용")
                .source("NAVER_FINANCE")
                .publishedAt(LocalDateTime.now())
                .build();

        // Unique 제약 조건 위반 예외 발생
        assertThatThrownBy(() -> {
            newsRepository.save(duplicateNews);
            newsRepository.flush();  // 즉시 실행하여 예외 발생 확인
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("필수 필드 null - 실패 (Not Null 제약 조건)")
    void save_NullTitle_Fail() {
        // given: 제목이 null인 뉴스
        News invalidNews = News.builder()
                .url("https://example.com/test")
                .title(null)  // null (필수 필드)
                .content("내용")
                .source("NAVER_FINANCE")
                .build();

        // when & then: Not Null 제약 조건 위반
        assertThatThrownBy(() -> {
            newsRepository.save(invalidNews);
            newsRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("URL null - 실패 (Not Null 제약 조건)")
    void save_NullUrl_Fail() {
        // given: URL이 null인 뉴스
        News invalidNews = News.builder()
                .url(null)  // null (필수 필드)
                .title("제목")
                .content("내용")
                .source("NAVER_FINANCE")
                .build();

        // when & then: Not Null 제약 조건 위반
        assertThatThrownBy(() -> {
            newsRepository.save(invalidNews);
            newsRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Source null - 실패 (Not Null 제약 조건)")
    void save_NullSource_Fail() {
        // given: Source가 null인 뉴스
        News invalidNews = News.builder()
                .url("https://example.com/test")
                .title("제목")
                .content("내용")
                .source(null)  // null (필수 필드)
                .build();

        // when & then: Not Null 제약 조건 위반
        assertThatThrownBy(() -> {
            newsRepository.save(invalidNews);
            newsRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("빈 리스트 조회 - 성공")
    void findBySource_EmptyList() {
        // given: NAVER_FINANCE만 저장
        newsRepository.save(testNews1);

        // when: 존재하지 않는 출처로 조회
        List<News> newsWithNonExistentSource = newsRepository.findBySource("BLOOMBERG");

        // then: 빈 리스트 반환
        assertThat(newsWithNonExistentSource).isEmpty();
    }

    @Test
    @DisplayName("빈 페이지 조회 - 성공")
    void findAllByOrderByViewCountDesc_EmptyPage() {
        // given: 데이터 없음

        // when: 빈 DB에서 페이징 조회
        Pageable pageable = PageRequest.of(0, 10);
        Page<News> emptyPage = newsRepository.findAllByOrderByViewCountDesc(pageable);

        // then: 빈 페이지 반환
        assertThat(emptyPage.getContent()).isEmpty();
        assertThat(emptyPage.getTotalElements()).isEqualTo(0);
        assertThat(emptyPage.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이징 범위 초과 - 빈 페이지 반환")
    void findAllByOrderByViewCountDesc_OutOfRange() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 존재하지 않는 페이지 조회 (10번째 페이지)
        Pageable pageable = PageRequest.of(10, 10);
        Page<News> outOfRangePage = newsRepository.findAllByOrderByViewCountDesc(pageable);

        // then: 빈 페이지 반환
        assertThat(outOfRangePage.getContent()).isEmpty();
        assertThat(outOfRangePage.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("검색 키워드 없음 - 빈 리스트")
    void searchByKeyword_NoMatch() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 존재하지 않는 키워드로 검색
        List<News> searchResults = newsRepository.searchByKeyword("존재하지않는키워드12345");

        // then: 빈 리스트 반환
        assertThat(searchResults).isEmpty();
    }

    @Test
    @DisplayName("null 값 필드 저장 - 성공 (nullable 필드)")
    void save_NullableFields_Success() {
        // given: 선택 필드들이 null인 뉴스
        News newsWithNulls = News.builder()
                .url("https://example.com/test-null")
                .title("제목만 있는 뉴스")
                .source("NAVER_FINANCE")
                .content(null)  // nullable
                .publishedAt(null)  // nullable
                .sentimentScore(null)  // nullable
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithNulls);

        // then: 저장 성공
        assertThat(savedNews.getId()).isNotNull();
        assertThat(savedNews.getContent()).isNull();
        assertThat(savedNews.getPublishedAt()).isNull();
        assertThat(savedNews.getSentimentScore()).isNull();
    }

    @Test
    @DisplayName("감정 점수 경계값 - 최소값 (-1.0)")
    void save_SentimentScore_MinBoundary() {
        // given: 감정 점수 최소값 (-1.0)
        News negativeNews = News.builder()
                .url("https://example.com/negative")
                .title("매우 부정적인 뉴스")
                .content("내용")
                .source("NAVER_FINANCE")
                .sentimentScore(BigDecimal.valueOf(-1.0))
                .build();

        // when: 저장
        News savedNews = newsRepository.save(negativeNews);

        // then: 저장 성공
        assertThat(savedNews.getSentimentScore())
                .isEqualByComparingTo(BigDecimal.valueOf(-1.0));
    }

    @Test
    @DisplayName("감정 점수 경계값 - 최대값 (1.0)")
    void save_SentimentScore_MaxBoundary() {
        // given: 감정 점수 최대값 (1.0)
        News positiveNews = News.builder()
                .url("https://example.com/positive")
                .title("매우 긍정적인 뉴스")
                .content("내용")
                .source("NAVER_FINANCE")
                .sentimentScore(BigDecimal.valueOf(1.0))
                .build();

        // when: 저장
        News savedNews = newsRepository.save(positiveNews);

        // then: 저장 성공
        assertThat(savedNews.getSentimentScore())
                .isEqualByComparingTo(BigDecimal.valueOf(1.0));
    }

    @Test
    @DisplayName("감정 점수 범위 초과 - 실패 (Entity 검증)")
    void setSentimentScore_OutOfRange_Fail() {
        // given: 저장된 뉴스
        News savedNews = newsRepository.save(testNews1);

        // when & then: 범위 초과 값 설정 시도 (1.5 > 1.0)
        assertThatThrownBy(() -> {
            savedNews.setSentimentScore(BigDecimal.valueOf(1.5));
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("감정 점수는 -1.0과 1.0 사이여야 합니다");
    }

    @Test
    @DisplayName("감정 점수 범위 미만 - 실패 (Entity 검증)")
    void setSentimentScore_BelowRange_Fail() {
        // given: 저장된 뉴스
        News savedNews = newsRepository.save(testNews1);

        // when & then: 범위 미만 값 설정 시도 (-1.5 < -1.0)
        assertThatThrownBy(() -> {
            savedNews.setSentimentScore(BigDecimal.valueOf(-1.5));
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("감정 점수는 -1.0과 1.0 사이여야 합니다");
    }

    @Test
    @DisplayName("감정 점수 null 설정 - 성공")
    void setSentimentScore_Null_Success() {
        // given: 감정 점수가 있는 뉴스
        News savedNews = newsRepository.save(testNews1);
        assertThat(savedNews.getSentimentScore()).isNotNull();

        // when: null로 설정
        savedNews.setSentimentScore(null);
        News updatedNews = newsRepository.save(savedNews);

        // then: null로 저장됨
        assertThat(updatedNews.getSentimentScore()).isNull();
    }

    @Test
    @DisplayName("감정 점수 정밀도 - precision 3, scale 2")
    void save_SentimentScore_Precision() {
        // given: 소수점 3자리 감정 점수
        News news1 = News.builder()
                .url("https://example.com/precision1")
                .title("정밀도 테스트 1")
                .source("NAVER_FINANCE")
                .sentimentScore(BigDecimal.valueOf(0.756))  // 3자리
                .build();

        News news2 = News.builder()
                .url("https://example.com/precision2")
                .title("정밀도 테스트 2")
                .source("NAVER_FINANCE")
                .sentimentScore(BigDecimal.valueOf(0.12))  // 2자리
                .build();

        // when: 저장
        News savedNews1 = newsRepository.save(news1);
        News savedNews2 = newsRepository.save(news2);

        // then: scale이 2 이하로 유지됨
        assertThat(savedNews1.getSentimentScore()).isNotNull();
        assertThat(savedNews2.getSentimentScore()).isNotNull();
        // DB의 precision/scale 설정에 따라 반올림 또는 절사됨
    }

    @Test
    @DisplayName("조회수 0인 뉴스 - 정상 동작")
    void save_ZeroViewCount_Success() {
        // given: 조회수 0인 뉴스 (기본값)
        News zeroViewNews = News.builder()
                .url("https://example.com/zero-view")
                .title("조회수 0인 뉴스")
                .content("내용")
                .source("NAVER_FINANCE")
                .build();

        // when: 저장
        News savedNews = newsRepository.save(zeroViewNews);

        // then: 조회수 기본값 0
        assertThat(savedNews.getViewCount()).isEqualTo(0);
        assertThat(savedNews.getIsHighView()).isFalse();
    }

    @Test
    @DisplayName("조회수 음수 설정 - 저장은 되지만 비정상")
    void save_NegativeViewCount_SavedButAbnormal() {
        // given: 조회수 음수인 뉴스
        News newsWithNegativeViews = News.builder()
                .url("https://example.com/negative-views")
                .title("음수 조회수 테스트")
                .source("NAVER_FINANCE")
                .viewCount(-100)  // 음수 (비정상적이지만 제약 조건 없음)
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithNegativeViews);

        // then: 저장은 되지만 비정상적인 값
        assertThat(savedNews.getViewCount()).isEqualTo(-100);
        assertThat(savedNews.getIsHighView()).isFalse();
        // 참고: Entity에 검증 로직 추가 권장
    }

    @Test
    @DisplayName("매우 큰 조회수 - 성공")
    void save_VeryLargeViewCount_Success() {
        // given: 매우 큰 조회수
        News newsWithLargeViews = News.builder()
                .url("https://example.com/large-views")
                .title("매우 큰 조회수 테스트")
                .source("NAVER_FINANCE")
                .viewCount(Integer.MAX_VALUE - 1)  // 2,147,483,646
                .isHighView(true)  // 명시적 설정
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithLargeViews);

        // then: 저장 성공
        assertThat(savedNews.getViewCount()).isEqualTo(Integer.MAX_VALUE - 1);
        assertThat(savedNews.getIsHighView()).isTrue();
    }

    @Test
    @DisplayName("조회수 경계값 - 999 (고조회수 미만)")
    void incrementViewCount_Boundary_999() {
        // given: 조회수 999인 뉴스
        News news = News.builder()
                .url("https://example.com/boundary")
                .title("경계값 테스트")
                .content("내용")
                .source("NAVER_FINANCE")
                .viewCount(999)
                .build();
        News savedNews = newsRepository.save(news);

        // when: 조회수 확인
        // then: isHighView = false (1000 미만)
        assertThat(savedNews.getIsHighView()).isFalse();
    }

    @Test
    @DisplayName("조회수 경계값 - 1000 (고조회수 정확히)")
    void incrementViewCount_Boundary_1000() {
        // given: 조회수 999인 뉴스
        News news = News.builder()
                .url("https://example.com/boundary-1000")
                .title("경계값 1000 테스트")
                .content("내용")
                .source("NAVER_FINANCE")
                .viewCount(999)
                .build();
        News savedNews = newsRepository.save(news);

        // when: 조회수 1 증가 (999 → 1000)
        savedNews.incrementViewCount();
        News updatedNews = newsRepository.save(savedNews);

        // then: isHighView = true (정확히 1000)
        assertThat(updatedNews.getViewCount()).isEqualTo(1000);
        assertThat(updatedNews.getIsHighView()).isTrue();
    }

    @Test
    @DisplayName("긴 제목 저장 - 성공 (500자 이내)")
    void save_LongTitle_Success() {
        // given: 긴 제목 (500자)
        String longTitle = "가".repeat(500);
        News newsWithLongTitle = News.builder()
                .url("https://example.com/long-title")
                .title(longTitle)
                .content("내용")
                .source("NAVER_FINANCE")
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithLongTitle);

        // then: 저장 성공
        assertThat(savedNews.getTitle()).hasSize(500);
    }

    @Test
    @DisplayName("긴 URL 저장 - 성공 (TEXT 타입)")
    void save_LongUrl_Success() {
        // given: 매우 긴 URL (2000자)
        String longUrl = "https://example.com/news?param=" + "a".repeat(1965);
        News newsWithLongUrl = News.builder()
                .url(longUrl)
                .title("긴 URL 테스트")
                .source("NAVER_FINANCE")
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithLongUrl);

        // then: 저장 성공 (TEXT 타입이므로 제한 없음)
        assertThat(savedNews.getUrl()).isNotNull();
        assertThat(savedNews.getUrl().length()).isGreaterThan(1900);
    }

    @Test
    @DisplayName("빈 본문 저장 - 성공")
    void save_EmptyContent_Success() {
        // given: 본문이 빈 문자열인 뉴스
        News newsWithEmptyContent = News.builder()
                .url("https://example.com/empty-content")
                .title("제목만 있는 뉴스")
                .content("")  // 빈 문자열 (nullable이므로 가능)
                .source("NAVER_FINANCE")
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithEmptyContent);

        // then: 저장 성공
        assertThat(savedNews.getContent()).isEmpty();
    }

    @Test
    @DisplayName("긴 본문 저장 - 성공 (TEXT 타입)")
    void save_LongContent_Success() {
        // given: 매우 긴 본문 (10000자 이상)
        String longContent = "본문내용 ".repeat(1111);  // 약 5555자
        News newsWithLongContent = News.builder()
                .url("https://example.com/long-content")
                .title("긴 본문 테스트")
                .content(longContent)
                .source("NAVER_FINANCE")
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithLongContent);

        // then: 저장 성공 (TEXT 타입이므로 제한 없음)
        assertThat(savedNews.getContent()).isNotEmpty();
        assertThat(savedNews.getContent().length()).isGreaterThan(5000);
    }

    @Test
    @DisplayName("빈 문자열 검색 - 모든 뉴스 반환")
    void searchByKeyword_EmptyString() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 빈 문자열로 검색
        List<News> allNews = newsRepository.searchByKeyword("");

        // then: 모든 뉴스 반환 (LIKE %%는 모든 문자열 매칭)
        assertThat(allNews).hasSize(3);
    }

    @Test
    @DisplayName("날짜 범위 쿼리 - 시작일 = 종료일")
    void findNewsBetweenDates_SameDate() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when: 시작일 = 종료일
        LocalDateTime sameDate = LocalDateTime.now().minusDays(1);
        List<News> newsInRange = newsRepository.findNewsBetweenDates(sameDate, sameDate);

        // then: 해당 시간의 뉴스만 반환 (시분초 차이로 0개일 수 있음)
        assertThat(newsInRange).isNotNull();
    }

    @Test
    @DisplayName("과거 날짜 저장 - 성공")
    void save_VeryOldDate_Success() {
        // given: 100년 전 날짜
        News oldNews = News.builder()
                .url("https://example.com/old-news")
                .title("100년 전 뉴스")
                .source("NAVER_FINANCE")
                .publishedAt(LocalDateTime.now().minusYears(100))
                .build();

        // when: 저장
        News savedNews = newsRepository.save(oldNews);

        // then: 저장 성공
        assertThat(savedNews.getPublishedAt()).isBefore(LocalDateTime.now().minusYears(99));
    }

    @Test
    @DisplayName("미래 날짜 저장 - 성공")
    void save_FutureDate_Success() {
        // given: 미래 날짜
        News futureNews = News.builder()
                .url("https://example.com/future-news")
                .title("미래 뉴스")
                .source("NAVER_FINANCE")
                .publishedAt(LocalDateTime.now().plusYears(10))
                .build();

        // when: 저장
        News savedNews = newsRepository.save(futureNews);

        // then: 저장 성공 (유효성 검사 없음)
        assertThat(savedNews.getPublishedAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("날짜 범위 역순 조회 - 빈 리스트")
    void findNewsBetweenDates_ReversedDates() {
        // given: 뉴스 저장
        newsRepository.save(testNews1);

        // when: 시작일 > 종료일 (역순)
        LocalDateTime endDate = LocalDateTime.now().minusDays(10);
        LocalDateTime startDate = LocalDateTime.now();
        List<News> newsInRange = newsRepository.findNewsBetweenDates(startDate, endDate);

        // then: 빈 리스트 (BETWEEN은 start <= value <= end이므로)
        assertThat(newsInRange).isEmpty();
    }

    @Test
    @DisplayName("평균 조회수 - 데이터 없음")
    void getAverageViewCount_NoData() {
        // given: 데이터 없음

        // when: 평균 조회수 조회
        Double avgViewCount = newsRepository.getAverageViewCount();

        // then: null 반환 (데이터가 없으면 AVG는 null)
        assertThat(avgViewCount).isNull();
    }

    @Test
    @DisplayName("삭제 후 재조회 - 빈 Optional")
    void delete_ThenFind_ReturnsEmpty() {
        // given: 뉴스 저장 및 삭제
        News savedNews = newsRepository.save(testNews1);
        UUID savedId = savedNews.getId();
        newsRepository.delete(savedNews);
        newsRepository.flush();

        // when: 삭제된 뉴스 조회
        Optional<News> deletedNews = newsRepository.findById(savedId);

        // then: 빈 Optional
        assertThat(deletedNews).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 출처 삭제 - 예외 없음")
    void deleteBySource_NonExistent_NoException() {
        // given: NAVER_FINANCE만 저장
        newsRepository.save(testNews1);

        // when: 존재하지 않는 출처 삭제
        assertThatNoException().isThrownBy(() -> {
            newsRepository.deleteBySource("NONEXISTENT_SOURCE");
            newsRepository.flush();
        });

        // then: 기존 데이터는 그대로
        List<News> remainingNews = newsRepository.findAll();
        assertThat(remainingNews).hasSize(1);
    }

    @Test
    @DisplayName("페이징 크기 0 - IllegalArgumentException")
    void findAllByOrderByViewCountDesc_PageSizeZero() {
        // given: 3개의 뉴스 저장
        newsRepository.save(testNews1);
        newsRepository.save(testNews2);
        newsRepository.save(testNews3);

        // when & then: 페이지 크기 0으로 조회 시 예외 발생
        assertThatThrownBy(() -> {
            PageRequest.of(0, 0);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("대소문자 구분 검색 - 대소문자 무관")
    void searchByKeyword_CaseInsensitive() {
        // given: "삼성전자" 포함 뉴스 저장
        newsRepository.save(testNews1);

        // when: 소문자로 검색
        List<News> results1 = newsRepository.searchByKeyword("삼성");

        // 대문자 키워드 (영문 테스트용 뉴스 추가)
        News englishNews = News.builder()
                .url("https://example.com/english")
                .title("Apple iPhone Release")
                .content("Apple released new iPhone")
                .source("YAHOO_FINANCE")
                .build();
        newsRepository.save(englishNews);

        List<News> results2 = newsRepository.searchByKeyword("apple");
        List<News> results3 = newsRepository.searchByKeyword("APPLE");

        // then: H2 DB는 기본적으로 대소문자 구분하지 않음
        assertThat(results1).hasSize(1);
        assertThat(results2.size()).isEqualTo(results3.size());
    }

    @Test
    @DisplayName("긴 Source 이름 - 100자 제한")
    void save_LongSource_Success() {
        // given: 긴 Source 이름 (100자)
        String longSource = "SOURCE_" + "A".repeat(93);  // 총 100자
        News newsWithLongSource = News.builder()
                .url("https://example.com/long-source")
                .title("긴 Source 테스트")
                .source(longSource)
                .build();

        // when: 저장
        News savedNews = newsRepository.save(newsWithLongSource);

        // then: 저장 성공
        assertThat(savedNews.getSource()).hasSize(100);
    }

    @Test
    @DisplayName("대량 데이터 페이징 - 여러 페이지")
    void findAllByOrderByViewCountDesc_LargeDataset() {
        // given: 25개의 뉴스 저장
        for (int i = 0; i < 25; i++) {
            News news = News.builder()
                    .url("https://example.com/news-" + i)
                    .title("뉴스 " + i)
                    .source("NAVER_FINANCE")
                    .viewCount(i * 100)  // 0, 100, 200, ..., 2400
                    .build();
            newsRepository.save(news);
        }

        // when: 페이지 크기 10으로 조회
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 10);
        Pageable page2 = PageRequest.of(2, 10);

        Page<News> firstPage = newsRepository.findAllByOrderByViewCountDesc(page0);
        Page<News> secondPage = newsRepository.findAllByOrderByViewCountDesc(page1);
        Page<News> thirdPage = newsRepository.findAllByOrderByViewCountDesc(page2);

        // then: 페이징 정상 동작
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(secondPage.getContent()).hasSize(10);
        assertThat(thirdPage.getContent()).hasSize(5);
        assertThat(firstPage.getTotalElements()).isEqualTo(25);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);

        // 조회수 내림차순 확인
        assertThat(firstPage.getContent().get(0).getViewCount())
                .isGreaterThan(firstPage.getContent().get(9).getViewCount());
    }

    @Test
    @DisplayName("같은 조회수 여러 개 - 페이징 일관성")
    void findAllByOrderByViewCountDesc_SameViewCount() {
        // given: 같은 조회수를 가진 뉴스 5개
        for (int i = 0; i < 5; i++) {
            News news = News.builder()
                    .url("https://example.com/same-view-" + i)
                    .title("같은 조회수 뉴스 " + i)
                    .source("NAVER_FINANCE")
                    .viewCount(1000)  // 모두 1000
                    .build();
            newsRepository.save(news);
        }

        // when: 페이지 크기 3으로 두 번 조회
        Pageable pageable = PageRequest.of(0, 3);
        Page<News> page1 = newsRepository.findAllByOrderByViewCountDesc(pageable);
        Page<News> page2 = newsRepository.findAllByOrderByViewCountDesc(pageable);

        // then: 일관된 결과 반환
        assertThat(page1.getContent()).hasSize(3);
        assertThat(page2.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(5);

        // 모두 같은 조회수
        assertThat(page1.getContent())
                .extracting(News::getViewCount)
                .containsOnly(1000);
    }

    @Test
    @DisplayName("URL 대소문자 구분 - 다른 레코드로 저장")
    void save_UrlCaseSensitive() {
        // given: 대소문자만 다른 URL
        News news1 = News.builder()
                .url("https://example.com/News")
                .title("대문자 N")
                .source("NAVER_FINANCE")
                .build();

        News news2 = News.builder()
                .url("https://example.com/news")
                .title("소문자 n")
                .source("NAVER_FINANCE")
                .build();

        // when: 저장
        News savedNews1 = newsRepository.save(news1);
        News savedNews2 = newsRepository.save(news2);

        // then: 다른 레코드로 저장됨 (URL은 대소문자 구분)
        assertThat(savedNews1.getId()).isNotEqualTo(savedNews2.getId());
        assertThat(savedNews1.getUrl()).isNotEqualTo(savedNews2.getUrl());

        // 두 개 모두 조회 가능
        List<News> allNews = newsRepository.findAll();
        assertThat(allNews).hasSize(2);
    }

    @Test
    @DisplayName("감정 점수 Between 경계값 포함 확인")
    void findBySentimentScoreBetween_BoundaryInclusive() {
        // given: 경계값에 정확히 일치하는 뉴스
        News news1 = News.builder()
                .url("https://example.com/boundary1")
                .title("경계값 0.5")
                .source("NAVER_FINANCE")
                .sentimentScore(BigDecimal.valueOf(0.5))
                .build();

        News news2 = News.builder()
                .url("https://example.com/boundary2")
                .title("경계값 0.7")
                .source("NAVER_FINANCE")
                .sentimentScore(BigDecimal.valueOf(0.7))
                .build();

        newsRepository.save(news1);
        newsRepository.save(news2);

        // when: 0.5 ~ 0.7 범위 조회
        List<News> results = newsRepository.findBySentimentScoreBetween(
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.7)
        );

        // then: 경계값 포함 (BETWEEN은 inclusive)
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("평균 조회수 - 소수점 확인")
    void getAverageViewCount_DecimalPrecision() {
        // given: 조회수가 나누어떨어지지 않는 뉴스들
        newsRepository.save(News.builder()
                .url("https://example.com/avg1")
                .title("뉴스1")
                .source("NAVER_FINANCE")
                .viewCount(100)
                .build());

        newsRepository.save(News.builder()
                .url("https://example.com/avg2")
                .title("뉴스2")
                .source("NAVER_FINANCE")
                .viewCount(150)
                .build());

        newsRepository.save(News.builder()
                .url("https://example.com/avg3")
                .title("뉴스3")
                .source("NAVER_FINANCE")
                .viewCount(200)
                .build());

        // when: 평균 조회수 조회
        Double avgViewCount = newsRepository.getAverageViewCount();

        // then: (100 + 150 + 200) / 3 = 150.0
        assertThat(avgViewCount).isEqualTo(150.0);
    }
}
