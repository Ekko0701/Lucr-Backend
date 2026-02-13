package com.lucr.controller;

import tools.jackson.databind.ObjectMapper;
import com.lucr.dto.request.StockCreateRequest;
import com.lucr.dto.response.NewsResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.dto.response.StockResponse;
import com.lucr.entity.Market;
import com.lucr.exception.DuplicateResourceException;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.security.JwtAccessDeniedHandler;
import com.lucr.security.JwtAuthenticationEntryPoint;
import com.lucr.security.JwtAuthenticationFilter;
import com.lucr.security.JwtTokenProvider;
import com.lucr.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StockController 단위 테스트
 *
 * <p>종목 CRUD, 검색, 시장별 조회, 종목 관련 뉴스 조회 API 테스트</p>
 *
 * <h3>인증 전략</h3>
 * <p>프로덕션 {@code SecurityConfig}의 RBAC 규칙을 그대로 적용하기 위해,
 * {@code @WithMockUser} 대신 <strong>JWT 토큰 기반 모의 인증</strong>을 사용합니다.</p>
 *
 * <ul>
 *   <li>{@code SecurityConfig}의 {@code SessionCreationPolicy.STATELESS}가 {@code @WithMockUser}의
 *       SecurityContext를 무효화하므로, {@code Authorization: Bearer <token>} 헤더를 직접 전송</li>
 *   <li>Mocked {@code JwtTokenProvider}가 테스트 토큰을 검증하고,
 *       미리 정의된 역할(ADMIN/USER)의 Authentication을 반환</li>
 *   <li>프로덕션과 동일한 인증 흐름(JWT 필터 → SecurityContext 설정 → 인가 규칙 적용)으로 테스트</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@WebMvcTest(StockController.class)
@Import({com.lucr.config.SecurityConfig.class, StockControllerTest.TestSecurityBeans.class})
@DisplayName("StockController 테스트")
class StockControllerTest {

    /** 테스트용 ADMIN JWT 토큰 (실제 JWT 형식이 아니며, Mock에서 인식하는 문자열) */
    private static final String ADMIN_TOKEN = "test-admin-token";

    /** 테스트용 USER JWT 토큰 */
    private static final String USER_TOKEN = "test-user-token";

    /** ADMIN 사용자 UUID */
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** USER 사용자 UUID */
    private static final UUID NORMAL_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * SecurityConfig가 필요로 하는 Security 빈을 실제 인스턴스로 제공하는 테스트 설정.
     *
     * <p>{@code @WebMvcTest}는 {@code @Component}를 자동 스캔하지 않으므로,
     * SecurityConfig가 constructor injection으로 받는 빈들을 수동 생성한다.</p>
     *
     * <h3>왜 @MockitoBean이 아닌 실제 인스턴스인가?</h3>
     * <ul>
     *   <li>JwtAuthenticationFilter Mock → {@code doFilterInternal()} no-op →
     *       {@code filterChain.doFilter()} 미호출 → 필터 체인 중단</li>
     *   <li>JwtAuthenticationEntryPoint Mock → 401 응답 미작성</li>
     *   <li>JwtAccessDeniedHandler Mock → 403 응답 미작성</li>
     * </ul>
     */
    @TestConfiguration
    static class TestSecurityBeans {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
            return new JwtAuthenticationFilter(jwtTokenProvider);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
            return new JwtAuthenticationEntryPoint();
        }

        @Bean
        JwtAccessDeniedHandler jwtAccessDeniedHandler() {
            return new JwtAccessDeniedHandler();
        }

        // FilterRegistrationBean은 SecurityConfig.disableJwtFilterAutoRegistration()에서 제공
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    /**
     * JwtAuthenticationFilter가 의존하는 JwtTokenProvider Mock.
     * 테스트 토큰(ADMIN_TOKEN, USER_TOKEN)에 대해 미리 정의된 인증 정보를 반환한다.
     */
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    /** SecurityConfig.authenticationManager()가 필요로 하는 UserDetailsService Mock */
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private StockCreateRequest validCreateRequest;
    private StockResponse stockResponse;
    private PageResponse<StockResponse> stockPageResponse;

    @BeforeEach
    void setUp() {
        // ── 테스트 데이터 설정 ──
        validCreateRequest = StockCreateRequest.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();

        stockResponse = StockResponse.builder()
                .code("005930")
                .name("삼성전자")
                .market(Market.KOSPI)
                .newsCount(0)
                .createdAt(LocalDateTime.of(2026, 2, 12, 10, 0))
                .build();

        stockPageResponse = PageResponse.<StockResponse>builder()
                .content(List.of(stockResponse))
                .currentPage(0)
                .pageSize(50)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        // ── JWT Mock 설정: ADMIN 토큰 ──
        given(jwtTokenProvider.validateToken(ADMIN_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUserId(ADMIN_TOKEN)).willReturn(ADMIN_USER_ID);
        given(jwtTokenProvider.getEmail(ADMIN_TOKEN)).willReturn("admin@lucr.com");
        given(jwtTokenProvider.getRole(ADMIN_TOKEN)).willReturn("ADMIN");

        // ── JWT Mock 설정: USER 토큰 ──
        given(jwtTokenProvider.validateToken(USER_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUserId(USER_TOKEN)).willReturn(NORMAL_USER_ID);
        given(jwtTokenProvider.getEmail(USER_TOKEN)).willReturn("user@lucr.com");
        given(jwtTokenProvider.getRole(USER_TOKEN)).willReturn("USER");
    }

    // ========== POST /api/v1/stocks — 종목 등록 ==========

    @Nested
    @DisplayName("POST /api/v1/stocks - 종목 등록")
    class CreateStockTests {

        @Test
        @DisplayName("성공 — ADMIN 역할, 201 Created")
        void createStock_AsAdmin_Success() throws Exception {
            // given
            given(stockService.createStock(any(StockCreateRequest.class))).willReturn(stockResponse);

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validCreateRequest))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.code").value("005930"))
                    .andExpect(jsonPath("$.data.name").value("삼성전자"))
                    .andExpect(jsonPath("$.data.market").value("KOSPI"))
                    .andExpect(jsonPath("$.data.newsCount").value(0));

            then(stockService).should(times(1)).createStock(any(StockCreateRequest.class));
        }

        @Test
        @DisplayName("실패 — USER 역할, 403 Forbidden")
        void createStock_AsUser_Forbidden() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + USER_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validCreateRequest))
                    )
                    .andExpect(status().isForbidden());

            then(stockService).should(never()).createStock(any());
        }

        @Test
        @DisplayName("실패 — 인증 없음, 401 Unauthorized")
        void createStock_Unauthenticated_Unauthorized() throws Exception {
            // when & then — Authorization 헤더 없음
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validCreateRequest))
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 — 종목코드 중복, 409 Conflict")
        void createStock_DuplicateCode_Conflict() throws Exception {
            // given
            given(stockService.createStock(any(StockCreateRequest.class)))
                    .willThrow(new DuplicateResourceException(ErrorCode.DUPLICATE_STOCK_CODE));

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validCreateRequest))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("E409005"));
        }

        @Test
        @DisplayName("실패 — 종목코드 누락, 400 Bad Request")
        void createStock_MissingCode_BadRequest() throws Exception {
            // given
            StockCreateRequest invalidRequest = StockCreateRequest.builder()
                    .name("삼성전자")
                    .market(Market.KOSPI)
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(stockService).should(never()).createStock(any());
        }

        @Test
        @DisplayName("실패 — 시장 구분 누락, 400 Bad Request")
        void createStock_MissingMarket_BadRequest() throws Exception {
            // given
            StockCreateRequest invalidRequest = StockCreateRequest.builder()
                    .code("005930")
                    .name("삼성전자")
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(stockService).should(never()).createStock(any());
        }

        @Test
        @DisplayName("실패 — 종목명 누락, 400 Bad Request")
        void createStock_MissingName_BadRequest() throws Exception {
            // given
            StockCreateRequest invalidRequest = StockCreateRequest.builder()
                    .code("005930")
                    .market(Market.KOSPI)
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(stockService).should(never()).createStock(any());
        }

        @Test
        @DisplayName("실패 — 종목코드 20자 초과, 400 Bad Request")
        void createStock_CodeTooLong_BadRequest() throws Exception {
            // given — 21자 종목코드
            StockCreateRequest invalidRequest = StockCreateRequest.builder()
                    .code("A".repeat(21))
                    .name("테스트종목")
                    .market(Market.KOSPI)
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest());

            then(stockService).should(never()).createStock(any());
        }

        @Test
        @DisplayName("실패 — Validation 에러 응답 본문 상세 검증")
        void createStock_ValidationError_DetailedResponse() throws Exception {
            // given — code와 name 모두 빈 문자열
            StockCreateRequest invalidRequest = StockCreateRequest.builder()
                    .code("")
                    .name("")
                    .market(Market.KOSPI)
                    .build();

            // when & then
            mockMvc.perform(
                            post("/api/v1/stocks")
                                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400001"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors.length()").value(2));

            then(stockService).should(never()).createStock(any());
        }
    }

    // ========== GET /api/v1/stocks — 전체 종목 목록 ==========

    @Nested
    @DisplayName("GET /api/v1/stocks - 전체 종목 목록")
    class GetAllStocksTests {

        @Test
        @DisplayName("성공 — 인증된 사용자, 200 OK")
        void getAllStocks_Success() throws Exception {
            // given
            given(stockService.getAllStocks(any())).willReturn(stockPageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/stocks")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].code").value("005930"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("실패 — 인증 없음, 401 Unauthorized")
        void getAllStocks_Unauthenticated() throws Exception {
            // when & then — Authorization 헤더 없음
            mockMvc.perform(get("/api/v1/stocks"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET /api/v1/stocks/{code} — 종목 상세 ==========

    @Nested
    @DisplayName("GET /api/v1/stocks/{code} - 종목 상세")
    class GetStockTests {

        @Test
        @DisplayName("성공 — 200 OK")
        void getStock_Success() throws Exception {
            // given
            given(stockService.getStockByCode("005930")).willReturn(stockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/005930")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.code").value("005930"))
                    .andExpect(jsonPath("$.data.name").value("삼성전자"));
        }

        @Test
        @DisplayName("실패 — 종목 없음, 404 Not Found")
        void getStock_NotFound() throws Exception {
            // given
            given(stockService.getStockByCode("999999"))
                    .willThrow(new ResourceNotFoundException(ErrorCode.STOCK_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/stocks/999999")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("E404006"));
        }
    }

    // ========== DELETE /api/v1/stocks/{code} — 종목 삭제 ==========

    @Nested
    @DisplayName("DELETE /api/v1/stocks/{code} - 종목 삭제")
    class DeleteStockTests {

        @Test
        @DisplayName("성공 — ADMIN 역할, 200 OK")
        void deleteStock_AsAdmin_Success() throws Exception {
            // given
            willDoNothing().given(stockService).deleteStock("005930");

            // when & then
            mockMvc.perform(delete("/api/v1/stocks/005930")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(stockService).should(times(1)).deleteStock("005930");
        }

        @Test
        @DisplayName("실패 — USER 역할, 403 Forbidden")
        void deleteStock_AsUser_Forbidden() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/stocks/005930")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isForbidden());

            then(stockService).should(never()).deleteStock(anyString());
        }

        @Test
        @DisplayName("실패 — 종목 없음, 404 Not Found")
        void deleteStock_NotFound() throws Exception {
            // given
            willThrow(new ResourceNotFoundException(ErrorCode.STOCK_NOT_FOUND))
                    .given(stockService).deleteStock("999999");

            // when & then
            mockMvc.perform(delete("/api/v1/stocks/999999")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 — 인증 없음, 401 Unauthorized")
        void deleteStock_Unauthenticated_Unauthorized() throws Exception {
            // when & then — Authorization 헤더 없음
            mockMvc.perform(delete("/api/v1/stocks/005930"))
                    .andExpect(status().isUnauthorized());

            then(stockService).should(never()).deleteStock(anyString());
        }
    }

    // ========== GET /api/v1/stocks/search — 종목 검색 ==========

    @Nested
    @DisplayName("GET /api/v1/stocks/search - 종목 검색")
    class SearchStocksTests {

        @Test
        @DisplayName("성공 — 키워드 검색, 200 OK")
        void searchStocks_Success() throws Exception {
            // given
            given(stockService.searchStocks(eq("삼성"), any())).willReturn(stockPageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/search")
                            .header("Authorization", "Bearer " + USER_TOKEN)
                            .param("keyword", "삼성"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].code").value("005930"));
        }

        @Test
        @DisplayName("실패 — keyword 파라미터 누락, 400 Bad Request")
        void searchStocks_MissingKeyword_BadRequest() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/stocks/search")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== GET /api/v1/stocks/market/{market} — 시장별 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/stocks/market/{market} - 시장별 조회")
    class GetStocksByMarketTests {

        @Test
        @DisplayName("성공 — KOSPI 시장, 200 OK")
        void getStocksByMarket_Success() throws Exception {
            // given
            given(stockService.getStocksByMarket(eq(Market.KOSPI), any())).willReturn(stockPageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/market/KOSPI")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].market").value("KOSPI"));
        }

        @Test
        @DisplayName("실패 — 유효하지 않은 market 값, 400 Bad Request")
        void getStocksByMarket_InvalidMarketValue_BadRequest() throws Exception {
            // when & then — enum에 없는 값
            mockMvc.perform(get("/api/v1/stocks/market/INVALID_MARKET")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== GET /api/v1/stocks/{code}/news — 종목 관련 뉴스 ==========

    @Nested
    @DisplayName("GET /api/v1/stocks/{code}/news - 종목 관련 뉴스")
    class GetNewsByStockTests {

        @Test
        @DisplayName("성공 — 200 OK")
        void getNewsByStock_Success() throws Exception {
            // given
            NewsResponse newsResponse = NewsResponse.builder()
                    .id(UUID.randomUUID())
                    .title("삼성전자 주가 급등")
                    .contentSummary("삼성전자가 5% 이상 상승했습니다...")
                    .source("NAVER_FINANCE")
                    .build();

            PageResponse<NewsResponse> newsPageResponse = PageResponse.<NewsResponse>builder()
                    .content(List.of(newsResponse))
                    .currentPage(0)
                    .pageSize(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            given(stockService.getNewsByStockCode(eq("005930"), any())).willReturn(newsPageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/005930/news")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].title").value("삼성전자 주가 급등"));
        }

        @Test
        @DisplayName("실패 — 종목 없음, 404 Not Found")
        void getNewsByStock_StockNotFound() throws Exception {
            // given
            given(stockService.getNewsByStockCode(eq("999999"), any()))
                    .willThrow(new ResourceNotFoundException(ErrorCode.STOCK_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/stocks/999999/news")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET /api/v1/stocks/exists — 종목코드 존재 확인 ==========

    @Nested
    @DisplayName("GET /api/v1/stocks/exists - 종목코드 존재 확인")
    class CheckExistsTests {

        @Test
        @DisplayName("존재하는 종목코드 — true 반환")
        void checkExists_Exists_ReturnsTrue() throws Exception {
            // given
            given(stockService.existsByCode("005930")).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/exists")
                            .header("Authorization", "Bearer " + USER_TOKEN)
                            .param("code", "005930"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true))
                    .andExpect(jsonPath("$.message").value("존재하는 종목코드입니다."));
        }

        @Test
        @DisplayName("존재하지 않는 종목코드 — false 반환")
        void checkExists_NotExists_ReturnsFalse() throws Exception {
            // given
            given(stockService.existsByCode("999999")).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/exists")
                            .header("Authorization", "Bearer " + USER_TOKEN)
                            .param("code", "999999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false))
                    .andExpect(jsonPath("$.message").value("존재하지 않는 종목코드입니다."));
        }

        @Test
        @DisplayName("실패 — code 파라미터 누락, 400 Bad Request")
        void checkExists_MissingCodeParam_BadRequest() throws Exception {
            // when & then — code 파라미터 없이 요청
            mockMvc.perform(get("/api/v1/stocks/exists")
                            .header("Authorization", "Bearer " + USER_TOKEN))
                    .andExpect(status().isBadRequest());
        }
    }
}
