package com.lucr.controller;

import com.lucr.dto.response.CrawlJobResponse;
import com.lucr.dto.response.PageResponse;
import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import com.lucr.exception.BusinessException;
import com.lucr.exception.ErrorCode;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.messaging.CrawlJobPublisher;
import com.lucr.service.CrawlJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 단위 테스트
 *
 * @WebMvcTest: Controller 레이어만 로드 (가벼운 테스트)
 * MockMvc: HTTP 요청/응답 시뮬레이션
 * @MockitoBean: Service, Publisher를 Mock으로 대체
 *
 * @author Ekko0701
 * @since 2026-01-28
 */
@WebMvcTest(AdminController.class)
@WithMockUser(roles = "ADMIN")  // 모든 테스트에 ADMIN 역할 적용
@DisplayName("AdminController 테스트")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlJobService crawlJobService;

    @MockitoBean
    private CrawlJobPublisher crawlJobPublisher;

    // Security 의존성 Mock (SecurityConfig가 주입받는 빈)
    @MockitoBean
    private com.lucr.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.lucr.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private com.lucr.security.JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // 테스트 데이터
    private UUID testJobId;
    private CrawlJob pendingJob;
    private CrawlJob completedJob;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();

        // PENDING 상태의 CrawlJob
        pendingJob = CrawlJob.builder()
                .id(testJobId)
                .status(CrawlJobStatus.PENDING)
                .totalArticles(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // COMPLETED 상태의 CrawlJob
        completedJob = CrawlJob.builder()
                .id(testJobId)
                .status(CrawlJobStatus.COMPLETED)
                .totalArticles(150)
                .mediaResults("{\"hankyung\":50,\"maekyung\":50,\"edaily\":50}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    // ========== POST /api/v1/admin/crawl/trigger - 크롤링 트리거 ==========

    @Nested
    @DisplayName("POST /api/v1/admin/crawl/trigger - triggerCrawl()")
    class TriggerCrawlTests {

        @Test
        @DisplayName("성공 - 기본값(50)으로 크롤링 트리거")
        void triggerCrawl_DefaultMaxArticles_Success() throws Exception {
            // given
            given(crawlJobService.createJob()).willReturn(pendingJob);
            willDoNothing().given(crawlJobPublisher).publish(testJobId, 50);

            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("크롤링 작업이 시작되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(testJobId.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalArticles").value(0))
                    .andExpect(jsonPath("$.timestamp").exists());

            // Service, Publisher 호출 검증
            then(crawlJobService).should(times(1)).createJob();
            then(crawlJobPublisher).should(times(1)).publish(testJobId, 50);
        }

        @Test
        @DisplayName("성공 - maxArticles 커스텀 값(10) 지정")
        void triggerCrawl_CustomMaxArticles_Success() throws Exception {
            // given
            given(crawlJobService.createJob()).willReturn(pendingJob);
            willDoNothing().given(crawlJobPublisher).publish(testJobId, 10);

            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .param("maxArticles", "10")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testJobId.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            then(crawlJobService).should(times(1)).createJob();
            then(crawlJobPublisher).should(times(1)).publish(testJobId, 10);
        }

        @Test
        @DisplayName("실패 - 이미 실행 중인 작업 존재 (409)")
        void triggerCrawl_AlreadyRunning_Fail() throws Exception {
            // given - createJob()이 BusinessException 발생 (중복 실행 방지)
            given(crawlJobService.createJob())
                    .willThrow(new BusinessException(ErrorCode.CRAWL_JOB_ALREADY_RUNNING));

            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("E409003"))
                    .andExpect(jsonPath("$.message").exists());

            // Service만 호출되고, Publisher는 호출되지 않음
            then(crawlJobService).should(times(1)).createJob();
            then(crawlJobPublisher).should(times(0)).publish(any(), anyInt());
        }

        @Test
        @DisplayName("실패 - maxArticles 타입 오류 (400)")
        void triggerCrawl_InvalidMaxArticlesType_Fail() throws Exception {
            // when & then - 숫자가 아닌 값 전달
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .param("maxArticles", "abc")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest());

            // Service, Publisher 모두 호출되지 않음
            then(crawlJobService).should(times(0)).createJob();
            then(crawlJobPublisher).should(times(0)).publish(any(), anyInt());
        }

        @Test
        @DisplayName("실패 - RabbitMQ 발행 실패 시 예외 전파 (500)")
        void triggerCrawl_PublishFail_ThrowsException() throws Exception {
            // given - createJob 성공, publish 실패
            given(crawlJobService.createJob()).willReturn(pendingJob);
            willThrow(new RuntimeException("RabbitMQ connection refused"))
                    .given(crawlJobPublisher).publish(eq(testJobId), eq(50));

            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("E500001"))
                    .andExpect(jsonPath("$.message").exists());

            // Service, Publisher 모두 호출됨
            then(crawlJobService).should(times(1)).createJob();
            then(crawlJobPublisher).should(times(1)).publish(testJobId, 50);
        }

        @Test
        @DisplayName("성공 - maxArticles=0 허용 (유효성 검증 없음)")
        void triggerCrawl_ZeroMaxArticles_Success() throws Exception {
            // given
            given(crawlJobService.createJob()).willReturn(pendingJob);
            willDoNothing().given(crawlJobPublisher).publish(testJobId, 0);

            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .param("maxArticles", "0")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            then(crawlJobPublisher).should(times(1)).publish(testJobId, 0);
        }

        @Test
        @DisplayName("성공 - maxArticles 음수 허용 (유효성 검증 없음)")
        void triggerCrawl_NegativeMaxArticles_Success() throws Exception {
            // given
            given(crawlJobService.createJob()).willReturn(pendingJob);
            willDoNothing().given(crawlJobPublisher).publish(testJobId, -1);

            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/crawl/trigger")
                                    .param("maxArticles", "-1")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            then(crawlJobPublisher).should(times(1)).publish(testJobId, -1);
        }
    }

    // ========== GET /api/v1/admin/crawl/jobs/{jobId} - 작업 상태 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/admin/crawl/jobs/{jobId} - getJobStatus()")
    class GetJobStatusTests {

        @Test
        @DisplayName("성공 - PENDING 상태 조회")
        void getJobStatus_Pending_Success() throws Exception {
            // given
            given(crawlJobService.getJobById(testJobId)).willReturn(pendingJob);

            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/crawl/jobs/{jobId}", testJobId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testJobId.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalArticles").value(0))
                    .andExpect(jsonPath("$.timestamp").exists());

            then(crawlJobService).should(times(1)).getJobById(testJobId);
        }

        @Test
        @DisplayName("성공 - COMPLETED 상태 조회")
        void getJobStatus_Completed_Success() throws Exception {
            // given
            given(crawlJobService.getJobById(testJobId)).willReturn(completedJob);

            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/crawl/jobs/{jobId}", testJobId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testJobId.toString()))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.totalArticles").value(150))
                    .andExpect(jsonPath("$.data.mediaResults").exists())
                    .andExpect(jsonPath("$.data.completedAt").exists());

            then(crawlJobService).should(times(1)).getJobById(testJobId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 jobId (404)")
        void getJobStatus_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();
            given(crawlJobService.getJobById(nonExistentId))
                    .willThrow(ResourceNotFoundException.crawlJobNotFound(nonExistentId.toString()));

            // when & then
            mockMvc.perform(get("/api/v1/admin/crawl/jobs/{jobId}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());

            then(crawlJobService).should(times(1)).getJobById(nonExistentId);
        }

        @Test
        @DisplayName("실패 - 잘못된 UUID 형식 (400)")
        void getJobStatus_InvalidUuidFormat_Fail() throws Exception {
            // when & then - UUID 형식이 아닌 값 전달
            mockMvc.perform(get("/api/v1/admin/crawl/jobs/{jobId}", "invalid-uuid"))
                    .andExpect(status().isBadRequest());

            // Service 호출되지 않음
            then(crawlJobService).should(times(0)).getJobById(any());
        }
    }

    // ========== GET /api/v1/admin/crawl/jobs - 작업 이력 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/admin/crawl/jobs - getCrawlJobs()")
    class GetCrawlJobsTests {

        @Test
        @DisplayName("전체 이력 조회 - 성공")
        void getCrawlJobs_AllJobs_Success() throws Exception {
            PageResponse<CrawlJobResponse> pageResponse = PageResponse.<CrawlJobResponse>builder()
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
            given(crawlJobService.getAllJobs(any(Pageable.class))).willReturn(pageResponse);

            mockMvc.perform(get("/api/v1/admin/crawl/jobs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("상태별 필터 조회 - 성공")
        void getCrawlJobs_WithStatusFilter_Success() throws Exception {
            PageResponse<CrawlJobResponse> pageResponse = PageResponse.<CrawlJobResponse>builder()
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
            given(crawlJobService.getJobsByStatus(eq("COMPLETED"), any(Pageable.class)))
                    .willReturn(pageResponse);

            mockMvc.perform(get("/api/v1/admin/crawl/jobs")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(crawlJobService).should(times(1))
                    .getJobsByStatus(eq("COMPLETED"), any(Pageable.class));
        }
    }
}
