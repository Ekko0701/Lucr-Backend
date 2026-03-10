package com.lucr.service;

import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.RecommendationResponse;
import com.lucr.entity.Market;
import com.lucr.entity.Recommendation;
import com.lucr.entity.Stock;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.RecommendationMapper;
import com.lucr.repository.NewsStockRepository;
import com.lucr.repository.RecommendationRepository;
import com.lucr.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService 테스트")
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private NewsStockRepository newsStockRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private RecommendationMapper recommendationMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private Stock stock(String code, String name, Market market) {
        return Stock.builder()
                .code(code)
                .name(name)
                .market(market)
                .build();
    }

    @Nested
    @DisplayName("조회 메서드")
    class ReadMethodsTests {

        @Test
        @DisplayName("getRecommendationsByConfidence — minConfidence가 null이면 0.00으로 처리")
        void getRecommendationsByConfidence_NullThreshold_UsesZero() {
            Pageable pageable = PageRequest.of(0, 10);
            Recommendation entity = Recommendation.builder()
                    .id(UUID.randomUUID())
                    .stock(stock("005930", "삼성전자", Market.KOSPI))
                    .score(new BigDecimal("0.700"))
                    .confidence(new BigDecimal("0.80"))
                    .reason("[\"x\"]")
                    .relatedNewsCount(4)
                    .totalMentions(20)
                    .build();
            RecommendationResponse dto = RecommendationResponse.builder()
                    .stockCode("005930")
                    .score(new BigDecimal("0.700"))
                    .build();
            Page<Recommendation> page = new PageImpl<>(List.of(entity), pageable, 1);

            given(recommendationRepository.findByMinConfidence(
                    any(BigDecimal.class), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(page);
            given(recommendationMapper.toResponse(entity)).willReturn(dto);

            PageResponse<RecommendationResponse> result =
                    recommendationService.getRecommendationsByConfidence(null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getStockCode()).isEqualTo("005930");

            then(recommendationRepository).should(times(1))
                    .findByMinConfidence(
                            eq(BigDecimal.ZERO),
                            any(LocalDateTime.class),
                            any(Pageable.class)
                    );
        }

        @Test
        @DisplayName("getRecommendations — 유효한 추천 목록을 페이징 조회")
        void getRecommendations_ReturnsValidPagedResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Recommendation entity = Recommendation.builder()
                    .id(UUID.randomUUID())
                    .stock(stock("005930", "삼성전자", Market.KOSPI))
                    .score(new BigDecimal("0.800"))
                    .confidence(new BigDecimal("0.90"))
                    .reason("[\"긍정적 뉴스 감정\"]")
                    .relatedNewsCount(9)
                    .totalMentions(50)
                    .build();
            RecommendationResponse dto = RecommendationResponse.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .score(new BigDecimal("0.800"))
                    .build();
            Page<Recommendation> page = new PageImpl<>(List.of(entity), pageable, 1);

            given(recommendationRepository.findValidRecommendations(
                    any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(page);
            given(recommendationMapper.toResponse(entity)).willReturn(dto);

            PageResponse<RecommendationResponse> result =
                    recommendationService.getRecommendations(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getStockCode()).isEqualTo("005930");
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getIsFirst()).isTrue();

            then(recommendationRepository).should(times(1))
                    .findValidRecommendations(any(LocalDateTime.class), eq(pageable));
        }

        @Test
        @DisplayName("getRecommendationByStockCode — 존재하면 응답 DTO 반환")
        void getRecommendationByStockCode_Found_ReturnsResponse() {
            Recommendation entity = Recommendation.builder()
                    .id(UUID.randomUUID())
                    .stock(stock("005930", "삼성전자", Market.KOSPI))
                    .score(new BigDecimal("0.773"))
                    .confidence(new BigDecimal("1.00"))
                    .reason("[\"긍정적 뉴스 감정\"]")
                    .relatedNewsCount(12)
                    .totalMentions(180)
                    .build();
            RecommendationResponse dto = RecommendationResponse.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .market("KOSPI")
                    .score(new BigDecimal("0.773"))
                    .confidence(new BigDecimal("1.00"))
                    .build();

            given(recommendationRepository.findByStock_Code("005930"))
                    .willReturn(Optional.of(entity));
            given(recommendationMapper.toResponse(entity)).willReturn(dto);

            RecommendationResponse result =
                    recommendationService.getRecommendationByStockCode("005930");

            assertThat(result.getStockCode()).isEqualTo("005930");
            assertThat(result.getStockName()).isEqualTo("삼성전자");
            assertThat(result.getMarket()).isEqualTo("KOSPI");
            assertThat(result.getScore()).isEqualByComparingTo("0.773");

            then(recommendationRepository).should(times(1)).findByStock_Code("005930");
            then(recommendationMapper).should(times(1)).toResponse(entity);
        }

        @Test
        @DisplayName("getRecommendationByStockCode — 존재하지 않으면 ResourceNotFoundException")
        void getRecommendationByStockCode_NotFound_ThrowsException() {
            given(recommendationRepository.findByStock_Code("NOPE")).willReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getRecommendationByStockCode("NOPE"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("NOPE")
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("refreshAllRecommendations()")
    class RefreshAllRecommendationsTests {

        @Test
        @DisplayName("엣지 — 등록된 종목이 없으면 0 반환")
        void refreshAllRecommendations_NoStocks_ReturnsZero() {
            given(stockRepository.findAll()).willReturn(List.of());

            int updated = recommendationService.refreshAllRecommendations();

            assertThat(updated).isZero();
            then(recommendationRepository).should(never()).save(any());
            then(newsStockRepository).should(never()).avgSentimentByStockCode(anyString());
        }

        @Test
        @DisplayName("엣지 — relatedNewsCount < 2 인 종목은 스킵")
        void refreshAllRecommendations_SkipsWhenNewsCountTooSmall() {
            Stock stock = stock("005930", "삼성전자", Market.KOSPI);
            given(stockRepository.findAll()).willReturn(List.of(stock));
            given(newsStockRepository.sumMentionCountByStockCode("005930")).willReturn(0);
            given(newsStockRepository.countByStock_Code("005930")).willReturn(1);

            int updated = recommendationService.refreshAllRecommendations();

            assertThat(updated).isZero();
            then(recommendationRepository).should(never()).findByStock_Code(anyString());
            then(recommendationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("엣지 — 한 종목 실패 시 다른 종목은 계속 처리")
        void refreshAllRecommendations_ContinuesOnPerStockFailure() {
            Stock fail = stock("FAIL", "실패종목", Market.KOSPI);
            Stock ok = stock("OKAY", "정상종목", Market.KOSDAQ);

            given(stockRepository.findAll()).willReturn(List.of(fail, ok));

            // max 계산용 집계
            given(newsStockRepository.sumMentionCountByStockCode("FAIL")).willReturn(10);
            given(newsStockRepository.sumMentionCountByStockCode("OKAY")).willReturn(20);
            given(newsStockRepository.countByStock_Code("FAIL")).willReturn(2);
            given(newsStockRepository.countByStock_Code("OKAY")).willReturn(3);

            // FAIL 종목은 계산 중 예외
            given(newsStockRepository.avgSentimentByStockCode("FAIL"))
                    .willThrow(new RuntimeException("boom"));

            // OKAY 종목은 정상 계산
            given(newsStockRepository.avgSentimentByStockCode("OKAY"))
                    .willReturn(new BigDecimal("0.20"));
            given(newsStockRepository.countRecentNewsByStockCode(
                    anyString(), any(LocalDateTime.class)))
                    .willReturn(1);
            given(recommendationRepository.findByStock_Code("OKAY")).willReturn(Optional.empty());
            given(recommendationRepository.save(any(Recommendation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            int updated = recommendationService.refreshAllRecommendations();

            assertThat(updated).isEqualTo(1);
            then(recommendationRepository).should(times(1)).save(any(Recommendation.class));
        }

        @Test
        @DisplayName("신규 추천 생성 — score/confidence 계산값이 저장된다")
        void refreshAllRecommendations_NewRecommendation_CalculatesAndSaves() {
            Stock samsung = stock("005930", "삼성전자", Market.KOSPI);
            given(stockRepository.findAll()).willReturn(List.of(samsung));

            // 집계값: maxMentions=10, maxNewsCount=2
            given(newsStockRepository.sumMentionCountByStockCode("005930")).willReturn(10);
            given(newsStockRepository.countByStock_Code("005930")).willReturn(2);

            // sentimentRaw=0.0 -> sentimentNorm=0.500
            given(newsStockRepository.avgSentimentByStockCode("005930")).willReturn(BigDecimal.ZERO);
            // recency=1/2=0.500
            given(newsStockRepository.countRecentNewsByStockCode(
                    anyString(), any(LocalDateTime.class))).willReturn(1);

            given(recommendationRepository.findByStock_Code("005930")).willReturn(Optional.empty());
            given(recommendationRepository.save(any(Recommendation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            int updated = recommendationService.refreshAllRecommendations();

            assertThat(updated).isEqualTo(1);

            ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
            then(recommendationRepository).should(times(1)).save(captor.capture());
            Recommendation saved = captor.getValue();

            // score = 0.35*0.500 + 0.30*1.000 + 0.20*1.000 + 0.15*0.500 = 0.750
            assertThat(saved.getScore()).isEqualByComparingTo("0.750");
            assertThat(saved.getConfidence()).isEqualByComparingTo("0.20");
            assertThat(saved.getAvgSentiment()).isEqualByComparingTo("0.500");
            assertThat(saved.getRelatedNewsCount()).isEqualTo(2);
            assertThat(saved.getTotalMentions()).isEqualTo(10);
            assertThat(saved.getReason())
                    .contains("높은 언급 빈도")
                    .contains("관련 뉴스 다수")
                    .contains("최근 뉴스 활발");
            assertThat(saved.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("엣지 — avgSentiment가 null이면 0으로 보정 후 계산")
        void refreshAllRecommendations_NullSentiment_DefaultsToZero() {
            Stock samsung = stock("005930", "삼성전자", Market.KOSPI);
            given(stockRepository.findAll()).willReturn(List.of(samsung));

            given(newsStockRepository.sumMentionCountByStockCode("005930")).willReturn(10);
            given(newsStockRepository.countByStock_Code("005930")).willReturn(2);
            given(newsStockRepository.avgSentimentByStockCode("005930")).willReturn(null);
            given(newsStockRepository.countRecentNewsByStockCode(
                    anyString(), any(LocalDateTime.class))).willReturn(1);
            given(recommendationRepository.findByStock_Code("005930")).willReturn(Optional.empty());
            given(recommendationRepository.save(any(Recommendation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            int updated = recommendationService.refreshAllRecommendations();

            assertThat(updated).isEqualTo(1);

            ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
            then(recommendationRepository).should(times(1)).save(captor.capture());
            Recommendation saved = captor.getValue();

            // null -> 0.0, sentimentNorm = (0+1)/2 = 0.500
            assertThat(saved.getAvgSentiment()).isEqualByComparingTo("0.500");
            assertThat(saved.getScore()).isEqualByComparingTo("0.750");
        }

        @Test
        @DisplayName("부정 감정 종목 — reason에 '부정적 뉴스 감정 주의' 포함")
        void refreshAllRecommendations_NegativeSentiment_ContainsWarningReason() {
            Stock samsung = stock("005930", "삼성전자", Market.KOSPI);
            given(stockRepository.findAll()).willReturn(List.of(samsung));

            given(newsStockRepository.sumMentionCountByStockCode("005930")).willReturn(10);
            given(newsStockRepository.countByStock_Code("005930")).willReturn(2);
            // -0.80 → sentimentNorm = (-0.80+1)/2 = 0.100 (≤ 0.4)
            given(newsStockRepository.avgSentimentByStockCode("005930"))
                    .willReturn(new BigDecimal("-0.80"));
            given(newsStockRepository.countRecentNewsByStockCode(
                    anyString(), any(LocalDateTime.class))).willReturn(0);
            given(recommendationRepository.findByStock_Code("005930")).willReturn(Optional.empty());
            given(recommendationRepository.save(any(Recommendation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            recommendationService.refreshAllRecommendations();

            ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
            then(recommendationRepository).should(times(1)).save(captor.capture());
            Recommendation saved = captor.getValue();

            assertThat(saved.getReason()).contains("부정적 뉴스 감정 주의");
            assertThat(saved.getAvgSentiment()).isEqualByComparingTo("0.100");
        }

        @Test
        @DisplayName("기존 추천 존재 시 save 대신 updateScore로 갱신")
        void refreshAllRecommendations_ExistingRecommendation_UsesUpdatePath() {
            Stock samsung = stock("005930", "삼성전자", Market.KOSPI);
            Recommendation existing = org.mockito.Mockito.mock(Recommendation.class);

            given(stockRepository.findAll()).willReturn(List.of(samsung));
            given(newsStockRepository.sumMentionCountByStockCode("005930")).willReturn(10);
            given(newsStockRepository.countByStock_Code("005930")).willReturn(2);
            given(newsStockRepository.avgSentimentByStockCode("005930")).willReturn(BigDecimal.ZERO);
            given(newsStockRepository.countRecentNewsByStockCode(
                    anyString(), any(LocalDateTime.class))).willReturn(1);
            given(recommendationRepository.findByStock_Code("005930")).willReturn(Optional.of(existing));

            int updated = recommendationService.refreshAllRecommendations();

            assertThat(updated).isEqualTo(1);
            then(recommendationRepository).should(never()).save(any(Recommendation.class));
            then(existing).should(times(1)).updateScore(
                    any(BigDecimal.class),
                    any(BigDecimal.class),
                    anyString(),
                    anyInt(),
                    any(BigDecimal.class),
                    anyInt(),
                    any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("cleanupExpiredRecommendations()")
    class CleanupExpiredRecommendationsTests {

        @Test
        @DisplayName("deleteExpired 반환값을 그대로 반환")
        void cleanupExpiredRecommendations_ReturnsDeletedCount() {
            given(recommendationRepository.deleteExpired(any(LocalDateTime.class))).willReturn(3);

            int deleted = recommendationService.cleanupExpiredRecommendations();

            assertThat(deleted).isEqualTo(3);
            then(recommendationRepository).should(times(1)).deleteExpired(any(LocalDateTime.class));
        }
    }
}
