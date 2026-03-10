package com.lucr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.RecommendationResponse;
import com.lucr.entity.Recommendation;
import com.lucr.entity.Stock;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.mapper.RecommendationMapper;
import com.lucr.repository.NewsStockRepository;
import com.lucr.repository.RecommendationRepository;
import com.lucr.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RecommendationService 구현체.
 *
 * 뉴스 분석 결과를 종목 단위로 집계해 추천 점수를 계산/저장한다.
 *
 * 데이터 관점:
 * - 입력: news_stocks(언급 횟수), news.sentiment_score(감정), news.published_at(최신성)
 * - 출력: recommendations(score, confidence, reason, expires_at ...)
 *
 * 계산 흐름:
 * 1) 종목별 뉴스 집계(언급수/뉴스수/평균감정/최근뉴스수)
 * 2) 0~1 범위로 정규화
 * 3) 가중합으로 score 계산
 * 4) confidence/reason/expiresAt 계산 후 UPSERT
 *
 * 최종 점수 공식:
 * score = 0.35*sentiment + 0.30*mention + 0.20*volume + 0.15*recency
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final NewsStockRepository newsStockRepository;
    private final StockRepository stockRepository;
    private final RecommendationMapper recommendationMapper;

    /**
     * 추천 이유(List<String>)를 DB 저장용 JSON 문자열로 만들 때 사용.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 감정 점수 가중치.
     * - 기사들의 평균 감정이 긍정적일수록 추천 점수를 높인다.
     */
    private static final BigDecimal W_SENTIMENT = new BigDecimal("0.35");

    /**
     * 언급 빈도 가중치.
     * - 기사에서 해당 종목이 많이 언급될수록 관심도가 높다고 본다.
     */
    private static final BigDecimal W_MENTION = new BigDecimal("0.30");

    /**
     * 뉴스 볼륨 가중치.
     * - 특정 종목과 관련된 뉴스 자체의 개수를 반영한다.
     */
    private static final BigDecimal W_VOLUME = new BigDecimal("0.20");

    /**
     * 최신성 가중치.
     * - 최근 24시간 내 뉴스 비율을 반영해 "지금 이슈인지"를 반영한다.
     */
    private static final BigDecimal W_RECENCY = new BigDecimal("0.15");

    /**
     * 추천 유효 기간(시간).
     * - refresh 시점 기준 현재 + 24시간까지 유효.
     */
    private static final int EXPIRY_HOURS = 24;

    /**
     * 추천 계산에 필요한 최소 관련 뉴스 수.
     * - 데이터가 너무 적은 종목은 계산 결과가 불안정하므로 제외한다.
     */
    private static final int MIN_NEWS_COUNT = 2;

    /**
     * 만료되지 않은 추천 목록을 점수 내림차순으로 조회한다.
     *
     * @param pageable 페이지/사이즈/정렬 정보
     * @return RecommendationResponse 페이징 결과
     */
    @Override
    public PageResponse<RecommendationResponse> getRecommendations(Pageable pageable) {
        // 만료되지 않은 추천만 점수 내림차순으로 조회
        Page<Recommendation> page = recommendationRepository
                .findValidRecommendations(LocalDateTime.now(), pageable);
        return toPageResponse(page);
    }

    /**
     * 신뢰도 하한선을 기준으로 추천 목록을 필터링 조회한다.
     *
     * @param minConfidence 최소 신뢰도(0.00~1.00), null이면 0.00으로 처리
     * @param pageable 페이지/사이즈/정렬 정보
     * @return RecommendationResponse 페이징 결과
     */
    @Override
    public PageResponse<RecommendationResponse> getRecommendationsByConfidence(
            BigDecimal minConfidence, Pageable pageable) {
        // minConfidence가 null이면 전체 추천(0.00 이상) 조회로 처리
        BigDecimal threshold = minConfidence == null ? BigDecimal.ZERO : minConfidence;
        Page<Recommendation> page = recommendationRepository
                .findByMinConfidence(threshold, LocalDateTime.now(), pageable);
        return toPageResponse(page);
    }

    /**
     * 종목 코드로 단일 추천 정보를 조회한다.
     *
     * @param stockCode 종목코드 (예: 005930, AAPL)
     * @return 종목 추천 응답 DTO
     * @throws ResourceNotFoundException 해당 종목 추천이 없을 때
     */
    @Override
    public RecommendationResponse getRecommendationByStockCode(String stockCode) {
        // stock_code UNIQUE 제약이 있으므로 최대 1건만 조회된다.
        Recommendation recommendation = recommendationRepository
                .findByStock_Code(stockCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "해당 종목의 추천 정보를 찾을 수 없습니다: " + stockCode
                ));
        return recommendationMapper.toResponse(recommendation);
    }

    /**
     * 전체 종목의 추천 점수를 다시 계산한다.
     *
     * 배치 특성:
     * - 종목별 계산 실패를 개별 처리하여 전체 작업을 계속 진행
     * - 반환값은 "실제로 생성/갱신된 종목 수"
     *
     * @return 생성/갱신 성공 종목 수
     */
    @Override
    @Transactional
    public int refreshAllRecommendations() {
        log.info("추천 점수 전체 갱신 시작");

        // 갱신 대상은 현재 등록된 전체 종목
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            log.info("추천 갱신 대상 종목이 없습니다.");
            return 0;
        }

        int updatedCount = 0;

        // 정규화 기준(분모)로 사용할 전체 최대값 계산
        int maxMentions = getMaxTotalMentions(stocks);
        int maxNewsCount = getMaxNewsCount(stocks);

        for (Stock stock : stocks) {
            try {
                if (refreshStockRecommendation(stock, maxMentions, maxNewsCount)) {
                    updatedCount++;
                }
            } catch (Exception e) {
                // 한 종목 실패가 전체 배치를 중단시키지 않도록 계속 진행
                log.warn("종목 {} 추천 갱신 실패: {}", stock.getCode(), e.getMessage());
            }
        }

        log.info("추천 점수 전체 갱신 완료: {}개 종목 갱신", updatedCount);
        return updatedCount;
    }

    /**
     * 현재 시각 기준 만료된 추천 레코드를 삭제한다.
     *
     * @return 삭제 건수
     */
    @Override
    @Transactional
    public int cleanupExpiredRecommendations() {
        // DB가 반환한 실제 삭제 건수를 그대로 사용
        int deleted = recommendationRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("만료된 추천 {}건 삭제", deleted);
        }
        return deleted;
    }

    /**
     * 단일 종목 추천을 계산하여 신규 저장 또는 기존 레코드 갱신.
     *
     * 계산 항목 정의:
     * - totalMentions: 해당 종목의 총 언급 횟수 합
     * - relatedNewsCount: 해당 종목이 연결된 뉴스 개수
     * - avgSentimentRaw: 원본 평균 감정(-1~1)
     * - recentNewsCount: 최근 24시간 내 관련 뉴스 개수
     *
     * 정규화 정의:
     * - sentimentNorm = (avgSentimentRaw + 1) / 2
     * - mentionNorm   = totalMentions / maxMentions
     * - volumeNorm    = relatedNewsCount / maxNewsCount
     * - recencyNorm   = recentNewsCount / relatedNewsCount
     *
     * score는 소수점 3자리, confidence는 소수점 2자리로 반올림한다.
     *
     * @param stock 대상 종목
     * @param maxMentions 전체 종목 중 최대 언급 수(정규화 분모)
     * @param maxNewsCount 전체 종목 중 최대 뉴스 수(정규화 분모)
     * @return true면 추천이 생성/갱신됨, false면 데이터 부족으로 스킵
     */
    private boolean refreshStockRecommendation(Stock stock, int maxMentions, int maxNewsCount) {
        String stockCode = stock.getCode();

        // 1) 집계 데이터 조회
        // totalMentions는 "관심 강도", relatedNewsCount는 "근거 양"으로 활용된다.
        int totalMentions = newsStockRepository.sumMentionCountByStockCode(stockCode);
        int relatedNewsCount = newsStockRepository.countByStock_Code(stockCode);

        // 뉴스가 너무 적은 종목은 점수 신뢰도가 낮아 추천 계산에서 제외
        if (relatedNewsCount < MIN_NEWS_COUNT) {
            return false;
        }

        // AVG 집계는 데이터가 없으면 null일 수 있어 기본값 보정
        BigDecimal avgSentimentRaw = newsStockRepository.avgSentimentByStockCode(stockCode);
        if (avgSentimentRaw == null) {
            avgSentimentRaw = BigDecimal.ZERO;
        }

        // 2) 최신성 계산용 "최근 24시간 뉴스 수"
        // publishedAt 기준으로 최근성 판단
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        int recentNewsCount = newsStockRepository.countRecentNewsByStockCode(stockCode, oneDayAgo);

        // 3) 정규화: 모든 지표를 0~1 범위로 맞춰 가중합 가능하게 변환
        BigDecimal sentimentNorm = normalizeSentiment(avgSentimentRaw);
        BigDecimal mentionNorm = normalize(totalMentions, maxMentions);
        BigDecimal volumeNorm = normalize(relatedNewsCount, maxNewsCount);
        BigDecimal recencyNorm = relatedNewsCount > 0
                ? BigDecimal.valueOf(recentNewsCount)
                .divide(BigDecimal.valueOf(relatedNewsCount), 3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 4) 최종 점수 = 각 정규화 지표 * 가중치의 합
        BigDecimal score = sentimentNorm.multiply(W_SENTIMENT)
                .add(mentionNorm.multiply(W_MENTION))
                .add(volumeNorm.multiply(W_VOLUME))
                .add(recencyNorm.multiply(W_RECENCY))
                .setScale(3, RoundingMode.HALF_UP);

        // 5) 신뢰도: min(relatedNewsCount / 10, 1.0)
        // 예) 2건 -> 0.20, 5건 -> 0.50, 10건 이상 -> 1.00
        BigDecimal confidence = BigDecimal.valueOf(
                        Math.min((double) relatedNewsCount / 10.0, 1.0))
                .setScale(2, RoundingMode.HALF_UP);

        // 6) 이유 텍스트/만료시각 생성
        String reasonJson = buildReasons(sentimentNorm, mentionNorm, volumeNorm, recencyNorm);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(EXPIRY_HOURS);

        // 7) UPSERT: 있으면 갱신, 없으면 신규 생성
        // 동일 stock_code에 대해 레코드 1건만 유지(UNIQUE 제약)
        Recommendation recommendation = recommendationRepository
                .findByStock_Code(stockCode)
                .orElse(null);

        if (recommendation != null) {
            recommendation.updateScore(score, confidence, reasonJson,
                    relatedNewsCount, sentimentNorm, totalMentions, expiresAt);
        } else {
            recommendation = Recommendation.builder()
                    .stock(stock)
                    .score(score)
                    .confidence(confidence)
                    .reason(reasonJson)
                    .relatedNewsCount(relatedNewsCount)
                    .avgSentiment(sentimentNorm)
                    .totalMentions(totalMentions)
                    .expiresAt(expiresAt)
                    .build();
            recommendationRepository.save(recommendation);
        }

        return true;
    }

    /**
     * 감정 점수 정규화: -1 ~ 1 -> 0 ~ 1.
     *
     * 예: -1 -> 0.000, 0 -> 0.500, 1 -> 1.000
     *
     * @param raw 원본 평균 감정 점수(-1~1)
     * @return 정규화 감정 점수(0~1)
     */
    private BigDecimal normalizeSentiment(BigDecimal raw) {
        return raw.add(BigDecimal.ONE)
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_UP);
    }

    /**
     * 일반 정규화: value / max.
     *
     * max가 0이면 0으로 처리해 0 나누기 예외를 방지한다.
     *
     * @param value 원본 값
     * @param max 비교 기준 최대값
     * @return 정규화 값(0~1)
     */
    private BigDecimal normalize(int value, int max) {
        if (max <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(max), 3, RoundingMode.HALF_UP);
    }

    /**
     * 추천 이유 JSON 배열 생성.
     *
     * 지표 임계값을 기준으로 사용자가 이해할 수 있는 설명 문자열을 만든다.
     * 임계값 정책:
     * - sentiment >= 0.6 : 긍정적 뉴스 감정
     * - sentiment <= 0.4 : 부정적 뉴스 감정 주의
     * - mention   >= 0.5 : 높은 언급 빈도
     * - volume    >= 0.5 : 관련 뉴스 다수
     * - recency   >= 0.3 : 최근 뉴스 활발
     *
     * 어떤 조건도 만족하지 않으면 기본 이유 1개를 넣는다.
     */
    private String buildReasons(BigDecimal sentiment, BigDecimal mention,
                                BigDecimal volume, BigDecimal recency) {
        List<String> reasons = new ArrayList<>();

        if (sentiment.compareTo(new BigDecimal("0.6")) >= 0) {
            reasons.add("긍정적 뉴스 감정");
        } else if (sentiment.compareTo(new BigDecimal("0.4")) <= 0) {
            reasons.add("부정적 뉴스 감정 주의");
        }

        if (mention.compareTo(new BigDecimal("0.5")) >= 0) {
            reasons.add("높은 언급 빈도");
        }

        if (volume.compareTo(new BigDecimal("0.5")) >= 0) {
            reasons.add("관련 뉴스 다수");
        }

        if (recency.compareTo(new BigDecimal("0.3")) >= 0) {
            reasons.add("최근 뉴스 활발");
        }

        if (reasons.isEmpty()) {
            reasons.add("종합 분석 기반 추천");
        }

        try {
            return objectMapper.writeValueAsString(reasons);
        } catch (Exception e) {
            return "[\"종합 분석 기반 추천\"]";
        }
    }

    /**
     * 전체 종목 중 최대 언급 수.
     *
     * 정규화 분모로 사용한다.
     * 조회 결과가 없으면 1을 반환해 0 나누기를 방지한다.
     */
    private int getMaxTotalMentions(List<Stock> stocks) {
        return stocks.stream()
                .mapToInt(stock -> newsStockRepository.sumMentionCountByStockCode(stock.getCode()))
                .max()
                .orElse(1);
    }

    /**
     * 전체 종목 중 최대 관련 뉴스 수.
     *
     * 정규화 분모로 사용한다.
     * 조회 결과가 없으면 1을 반환해 0 나누기를 방지한다.
     */
    private int getMaxNewsCount(List<Stock> stocks) {
        return stocks.stream()
                .mapToInt(stock -> newsStockRepository.countByStock_Code(stock.getCode()))
                .max()
                .orElse(1);
    }

    /**
     * Page<Entity> -> PageResponse<DTO> 공통 변환.
     *
     * 엔티티를 API 스펙용 DTO로 변환해 컨트롤러에 전달한다.
     * (엔티티 직접 노출 방지)
     */
    private PageResponse<RecommendationResponse> toPageResponse(Page<Recommendation> page) {
        List<RecommendationResponse> content = page.getContent().stream()
                .map(recommendationMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }
}
