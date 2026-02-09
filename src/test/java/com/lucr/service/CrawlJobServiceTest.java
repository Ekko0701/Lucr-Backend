package com.lucr.service;

import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import com.lucr.exception.BusinessException;
import com.lucr.exception.ResourceNotFoundException;
import com.lucr.repository.CrawlJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * CrawlJobService 비즈니스 로직 테스트
 *
 * ======== 테스트 구조 설명 ========
 *
 * @ExtendWith(MockitoExtension.class)
 *   - JUnit 5에서 Mockito를 사용하기 위한 설정
 *   - @Mock, @InjectMocks 어노테이션을 활성화
 *   - Spring Context를 로드하지 않아서 매우 빠름
 *
 * @Mock
 *   - 가짜 객체 생성. 실제 DB에 접근하지 않음
 *   - given()으로 "이렇게 호출하면 이걸 반환해"라고 설정
 *
 * @InjectMocks
 *   - 테스트 대상 클래스. @Mock 객체들이 자동으로 주입됨
 *   - 실제 비즈니스 로직이 실행됨
 *
 * ======== BDD 패턴 ========
 *   given  → Mock 설정 (전제조건)
 *   when   → 실제 메서드 호출
 *   then   → 결과 검증 (assertThat) + Mock 호출 검증 (then().should())
 *
 * @author Ekko0701
 * @since 2026-01-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlJobService 테스트")
class CrawlJobServiceTest {

    /**
     * @Mock: CrawlJobRepository의 가짜 객체
     * 실제 DB 대신 given()으로 설정한 값을 반환
     */
    @Mock
    private CrawlJobRepository crawlJobRepository;

    /**
     * @InjectMocks: 테스트 대상인 CrawlJobServiceImpl
     * 위의 @Mock 객체가 자동으로 생성자에 주입됨
     */
    @InjectMocks
    private CrawlJobServiceImpl crawlJobService;

    // 테스트에서 공통으로 사용할 데이터
    private UUID testJobId;
    private CrawlJob pendingJob;

    /**
     * @BeforeEach: 각 테스트 메서드 실행 전에 호출
     * 테스트 데이터를 초기화하여 테스트 간 독립성 보장
     */
    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();

        // PENDING 상태의 CrawlJob (새로 생성된 작업)
        pendingJob = CrawlJob.builder()
                .id(testJobId)
                .status(CrawlJobStatus.PENDING)
                .totalArticles(0)
                .build();
    }

    // ========== 1. createJob() 테스트 ==========

    @Nested
    @DisplayName("createJob() - 크롤링 작업 생성")
    class CreateJobTests {

        /**
         * 정상 케이스: RUNNING 상태 작업이 없으면 새 작업 생성
         *
         * 검증 포인트:
         *   1. existsByStatus(RUNNING)이 false → 중복 없음
         *   2. save()가 호출됨 → DB에 저장
         *   3. 반환된 객체가 null이 아님
         */
        @Test
        @DisplayName("성공 - RUNNING 작업 없으면 새 작업 생성")
        void createJob_Success() {
            // given: 실행 중인 작업이 없음
            given(crawlJobRepository.existsByStatus(CrawlJobStatus.RUNNING))
                    .willReturn(false);

            // save() 호출 시 전달받은 객체를 그대로 반환 (DB 저장 시뮬레이션)
            // any(CrawlJob.class): 어떤 CrawlJob 객체든 매칭
            given(crawlJobRepository.save(any(CrawlJob.class)))
                    .willReturn(pendingJob);

            // when: 작업 생성
            CrawlJob result = crawlJobService.createJob();

            // then: 생성된 작업 검증
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CrawlJobStatus.PENDING);

            // Mock 호출 검증: existsByStatus 1번, save 1번 호출됨
            then(crawlJobRepository).should(times(1)).existsByStatus(CrawlJobStatus.RUNNING);
            then(crawlJobRepository).should(times(1)).save(any(CrawlJob.class));
        }

        /**
         * 실패 케이스: 이미 RUNNING 상태 작업이 있으면 BusinessException 발생
         *
         * 이 테스트가 중요한 이유:
         *   크롤링은 리소스 집약적인 작업이므로 동시에 2개 이상 실행되면 안 됨
         *   Service에서 이 비즈니스 규칙을 강제하는지 검증
         */
        @Test
        @DisplayName("실패 - RUNNING 작업 존재 시 BusinessException (409)")
        void createJob_AlreadyRunning_ThrowsException() {
            // given: 이미 실행 중인 작업이 있음
            given(crawlJobRepository.existsByStatus(CrawlJobStatus.RUNNING))
                    .willReturn(true);

            // when & then: BusinessException 발생
            // assertThatThrownBy: 람다 내부에서 예외가 발생하는지 검증
            assertThatThrownBy(() -> crawlJobService.createJob())
                    .isInstanceOf(BusinessException.class);

            // save()는 호출되지 않아야 함 (예외 발생 전에 중단)
            then(crawlJobRepository).should(never()).save(any());
        }
    }

    // ========== 2. getJobById() 테스트 ==========

    @Nested
    @DisplayName("getJobById() - 작업 ID로 조회")
    class GetJobByIdTests {

        /**
         * 정상 케이스: 존재하는 jobId로 조회하면 CrawlJob 반환
         */
        @Test
        @DisplayName("성공 - 존재하는 jobId로 조회")
        void getJobById_Success() {
            // given: findById() 호출 시 pendingJob 반환
            // Optional.of(): 값이 있는 Optional 생성
            given(crawlJobRepository.findById(testJobId))
                    .willReturn(Optional.of(pendingJob));

            // when: jobId로 조회
            CrawlJob result = crawlJobService.getJobById(testJobId);

            // then: 반환된 작업 검증
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testJobId);
            assertThat(result.getStatus()).isEqualTo(CrawlJobStatus.PENDING);

            then(crawlJobRepository).should(times(1)).findById(testJobId);
        }

        /**
         * 실패 케이스: 존재하지 않는 jobId로 조회하면 ResourceNotFoundException
         */
        @Test
        @DisplayName("실패 - 존재하지 않는 jobId (404)")
        void getJobById_NotFound_ThrowsException() {
            // given: findById() 호출 시 빈 Optional 반환 (DB에 없음)
            UUID nonExistentId = UUID.randomUUID();
            given(crawlJobRepository.findById(nonExistentId))
                    .willReturn(Optional.empty());

            // when & then: ResourceNotFoundException 발생
            assertThatThrownBy(() -> crawlJobService.getJobById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== 3. getJobsByStatus() 테스트 ==========

    @Nested
    @DisplayName("getJobsByStatus() - 상태별 작업 조회")
    class GetJobsByStatusTests {

        @Test
        @DisplayName("성공 - PENDING 상태 작업 목록 조회")
        void getJobsByStatus_Success() {
            // given: PENDING 상태 작업 2개 존재
            List<CrawlJob> pendingJobs = List.of(pendingJob, pendingJob);
            given(crawlJobRepository.findByStatus(CrawlJobStatus.PENDING))
                    .willReturn(pendingJobs);

            // when: PENDING 상태로 조회
            List<CrawlJob> result = crawlJobService.getJobsByStatus(CrawlJobStatus.PENDING);

            // then: 2개 반환
            assertThat(result).hasSize(2);
            then(crawlJobRepository).should(times(1)).findByStatus(CrawlJobStatus.PENDING);
        }

        @Test
        @DisplayName("성공 - 해당 상태 작업 없으면 빈 리스트")
        void getJobsByStatus_EmptyList() {
            // given: COMPLETED 상태 작업 없음
            given(crawlJobRepository.findByStatus(CrawlJobStatus.COMPLETED))
                    .willReturn(List.of());

            // when: COMPLETED 상태로 조회
            List<CrawlJob> result = crawlJobService.getJobsByStatus(CrawlJobStatus.COMPLETED);

            // then: 빈 리스트
            assertThat(result).isEmpty();
        }
    }

    // ========== 4. markRunning() 테스트 ==========

    @Nested
    @DisplayName("markRunning() - 작업 상태를 RUNNING으로 변경")
    class MarkRunningTests {

        /**
         * 정상 케이스: PENDING → RUNNING 상태 변경
         *
         * 검증 포인트:
         *   1. findById()로 작업 조회
         *   2. Entity의 markRunning() 호출 (상태 변경)
         *   3. JPA 더티 체킹으로 자동 저장 (save() 호출 불필요)
         */
        @Test
        @DisplayName("성공 - PENDING → RUNNING 상태 변경")
        void markRunning_Success() {
            // given: findById()로 PENDING 작업 반환
            given(crawlJobRepository.findById(testJobId))
                    .willReturn(Optional.of(pendingJob));

            // when: RUNNING으로 변경
            CrawlJob result = crawlJobService.markRunning(testJobId);

            // then: 상태가 RUNNING으로 변경됨
            assertThat(result.getStatus()).isEqualTo(CrawlJobStatus.RUNNING);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 jobId")
        void markRunning_NotFound_ThrowsException() {
            UUID nonExistentId = UUID.randomUUID();
            given(crawlJobRepository.findById(nonExistentId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> crawlJobService.markRunning(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== 5. markCompleted() 테스트 ==========

    @Nested
    @DisplayName("markCompleted() - 작업 완료 처리")
    class MarkCompletedTests {

        @Test
        @DisplayName("성공 - 작업 완료 + 결과 저장")
        void markCompleted_Success() {
            // given
            given(crawlJobRepository.findById(testJobId))
                    .willReturn(Optional.of(pendingJob));

            // when: 150건 수집, 결과 JSON과 함께 완료 처리
            String mediaResults = "{\"hankyung\":50,\"mk\":50,\"edaily\":50}";
            CrawlJob result = crawlJobService.markCompleted(testJobId, 150, mediaResults);

            // then: 상태, 기사 수, 미디어 결과, 완료 시간 검증
            assertThat(result.getStatus()).isEqualTo(CrawlJobStatus.COMPLETED);
            assertThat(result.getTotalArticles()).isEqualTo(150);
            assertThat(result.getMediaResults()).isEqualTo(mediaResults);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 jobId")
        void markCompleted_NotFound_ThrowsException() {
            UUID nonExistentId = UUID.randomUUID();
            given(crawlJobRepository.findById(nonExistentId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    crawlJobService.markCompleted(nonExistentId, 100, "{}"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== 6. markFailed() 테스트 ==========

    @Nested
    @DisplayName("markFailed() - 작업 실패 처리")
    class MarkFailedTests {

        @Test
        @DisplayName("성공 - 작업 실패 + 에러 메시지 저장")
        void markFailed_Success() {
            // given
            given(crawlJobRepository.findById(testJobId))
                    .willReturn(Optional.of(pendingJob));

            // when: 에러 메시지와 함께 실패 처리
            String errorMessage = "크롤러 연결 시간 초과";
            CrawlJob result = crawlJobService.markFailed(testJobId, errorMessage);

            // then: 상태, 에러 메시지, 완료 시간 검증
            assertThat(result.getStatus()).isEqualTo(CrawlJobStatus.FAILED);
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 jobId")
        void markFailed_NotFound_ThrowsException() {
            UUID nonExistentId = UUID.randomUUID();
            given(crawlJobRepository.findById(nonExistentId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    crawlJobService.markFailed(nonExistentId, "에러"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
