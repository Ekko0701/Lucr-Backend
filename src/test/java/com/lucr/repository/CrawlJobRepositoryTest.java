package com.lucr.repository;

import com.lucr.entity.CrawlJob;
import com.lucr.entity.CrawlJob.CrawlJobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * CrawlJobRepository 테스트 - 실제 DB로 검증
 *
 * ======== @DataJpaTest 동작 원리 ========
 *
 * @DataJpaTest가 하는 일:
 *   1. JPA 관련 컴포넌트만 로드 (Repository, Entity, EntityManager)
 *   2. 인메모리 H2 DB를 자동으로 띄움 (실제 PostgreSQL 대신)
 *   3. 각 테스트마다 @Transactional이 자동 적용됨
 *   4. 테스트 종료 후 자동 롤백 → 테스트 간 데이터 격리
 *
 * Mock을 사용하지 않음:
 *   - newsRepository.save(entity) → 진짜 H2 DB에 INSERT
 *   - newsRepository.findById(id) → 진짜 H2 DB에서 SELECT
 *   - "진짜 저장되는가?" "진짜 조회되는가?"를 검증
 *
 * NewsRepositoryTest와 동일한 패턴으로 작성됨
 *
 * @author Ekko0701
 * @since 2026-01-28
 */
@DataJpaTest
@DisplayName("CrawlJobRepository 테스트")
class CrawlJobRepositoryTest {

    /**
     * @Autowired: Spring이 H2 DB와 연결된 실제 Repository 구현체를 주입
     * (Mock이 아님! Spring Data JPA가 자동 생성한 진짜 구현체)
     */
    @Autowired
    private CrawlJobRepository crawlJobRepository;

    // ========== 1. 기본 CRUD 테스트 ==========

    @Nested
    @DisplayName("기본 CRUD")
    class CrudTests {

        /**
         * save() → findById()로 실제 저장 확인
         *
         * 이 테스트가 ItemRepositoryTest의 save()와 같은 패턴:
         *   1. 실제 객체 생성
         *   2. 실제로 저장
         *   3. 실제로 조회해서 비교
         */
        @Test
        @DisplayName("저장 후 조회 - 성공")
        void save_ThenFindById_Success() {
            // given: CrawlJob 생성 (ID는 DB가 자동 생성)
            CrawlJob job = CrawlJob.builder().build();

            // when: 실제 H2 DB에 저장
            // saveAndFlush(): save() + 즉시 INSERT SQL 실행
            // save()만 쓰면 INSERT가 지연되어 @CreationTimestamp 값이 아직 null일 수 있음
            CrawlJob savedJob = crawlJobRepository.saveAndFlush(job);

            // then: 저장된 결과 검증
            assertThat(savedJob.getId()).isNotNull();           // UUID 자동 생성됨
            assertThat(savedJob.getStatus()).isEqualTo(CrawlJobStatus.PENDING);  // @Builder.Default
            assertThat(savedJob.getTotalArticles()).isEqualTo(0);  // @Builder.Default
            assertThat(savedJob.getCreatedAt()).isNotNull();    // @CreationTimestamp (flush 후 채워짐)
            assertThat(savedJob.getUpdatedAt()).isNotNull();    // @UpdateTimestamp (flush 후 채워짐)

            // 실제로 DB에서 다시 조회하여 확인
            Optional<CrawlJob> foundJob = crawlJobRepository.findById(savedJob.getId());
            assertThat(foundJob).isPresent();
            assertThat(foundJob.get().getStatus()).isEqualTo(CrawlJobStatus.PENDING);
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 - 빈 Optional")
        void findById_NotFound() {
            // given: 존재하지 않는 UUID
            UUID nonExistentId = UUID.randomUUID();

            // when: 조회
            Optional<CrawlJob> result = crawlJobRepository.findById(nonExistentId);

            // then: 빈 Optional (예외가 아님!)
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제 후 조회 - 빈 Optional")
        void delete_ThenFindById_ReturnsEmpty() {
            // given: 저장 후 삭제
            CrawlJob savedJob = crawlJobRepository.save(CrawlJob.builder().build());
            UUID savedId = savedJob.getId();
            crawlJobRepository.delete(savedJob);
            crawlJobRepository.flush();  // DELETE SQL 즉시 실행

            // when: 삭제된 작업 조회
            Optional<CrawlJob> result = crawlJobRepository.findById(savedId);

            // then: 빈 Optional
            assertThat(result).isEmpty();
        }
    }

    // ========== 2. findByStatus() 테스트 ==========

    @Nested
    @DisplayName("findByStatus() - 상태별 조회")
    class FindByStatusTests {

        /**
         * 여러 상태의 작업을 저장하고 특정 상태만 조회
         *
         * 이것이 Mock 테스트와 다른 점:
         *   - Mock: given(repo.findByStatus(PENDING)).willReturn(목록) → "반환하라고 시킨 것"
         *   - 여기: 실제 저장 후 조회 → "진짜 필터링이 되는가?"
         */
        @Test
        @DisplayName("PENDING 상태만 조회")
        void findByStatus_Pending() {
            // given: PENDING 2개, RUNNING 1개 저장
            crawlJobRepository.save(CrawlJob.builder().build());  // PENDING (기본값)
            crawlJobRepository.save(CrawlJob.builder().build());  // PENDING

            CrawlJob runningJob = CrawlJob.builder().build();
            runningJob.markRunning();
            crawlJobRepository.save(runningJob);                   // RUNNING

            // when: PENDING 상태 조회
            List<CrawlJob> pendingJobs = crawlJobRepository.findByStatus(CrawlJobStatus.PENDING);

            // then: 2개만 조회됨 (RUNNING은 제외)
            assertThat(pendingJobs).hasSize(2);
            assertThat(pendingJobs)
                    .extracting(CrawlJob::getStatus)
                    .containsOnly(CrawlJobStatus.PENDING);
        }

        @Test
        @DisplayName("RUNNING 상태만 조회")
        void findByStatus_Running() {
            // given: PENDING 1개, RUNNING 1개
            crawlJobRepository.save(CrawlJob.builder().build());  // PENDING

            CrawlJob runningJob = CrawlJob.builder().build();
            runningJob.markRunning();
            crawlJobRepository.save(runningJob);                   // RUNNING

            // when
            List<CrawlJob> runningJobs = crawlJobRepository.findByStatus(CrawlJobStatus.RUNNING);

            // then
            assertThat(runningJobs).hasSize(1);
            assertThat(runningJobs.getFirst().getStatus()).isEqualTo(CrawlJobStatus.RUNNING);
        }

        @Test
        @DisplayName("해당 상태 없으면 빈 리스트")
        void findByStatus_NoMatch() {
            // given: PENDING만 저장
            crawlJobRepository.save(CrawlJob.builder().build());

            // when: COMPLETED 상태 조회
            List<CrawlJob> completedJobs = crawlJobRepository.findByStatus(CrawlJobStatus.COMPLETED);

            // then: 빈 리스트 (예외가 아님!)
            assertThat(completedJobs).isEmpty();
        }
    }

    // ========== 3. existsByStatus() 테스트 ==========

    @Nested
    @DisplayName("existsByStatus() - 상태별 존재 여부")
    class ExistsByStatusTests {

        /**
         * 이 메서드가 CrawlJobService.createJob()에서 중복 실행 방지에 사용됨
         *
         *   if (crawlJobRepository.existsByStatus(RUNNING)) {
         *       throw new BusinessException(CRAWL_JOB_ALREADY_RUNNING);
         *   }
         *
         * 따라서 이 테스트는 "진짜 RUNNING 작업이 있으면 true를 반환하는가?"를 검증
         */
        @Test
        @DisplayName("RUNNING 작업 존재 - true")
        void existsByStatus_Running_True() {
            // given: RUNNING 상태 작업 저장
            CrawlJob runningJob = CrawlJob.builder().build();
            runningJob.markRunning();
            crawlJobRepository.save(runningJob);

            // when: RUNNING 존재 여부 확인
            boolean exists = crawlJobRepository.existsByStatus(CrawlJobStatus.RUNNING);

            // then: true
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("RUNNING 작업 없음 - false")
        void existsByStatus_Running_False() {
            // given: PENDING만 저장 (RUNNING 없음)
            crawlJobRepository.save(CrawlJob.builder().build());

            // when: RUNNING 존재 여부 확인
            boolean exists = crawlJobRepository.existsByStatus(CrawlJobStatus.RUNNING);

            // then: false
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("DB가 비어있으면 - false")
        void existsByStatus_EmptyDb_False() {
            // given: 아무것도 저장하지 않음

            // when & then: 모든 상태에 대해 false
            assertThat(crawlJobRepository.existsByStatus(CrawlJobStatus.PENDING)).isFalse();
            assertThat(crawlJobRepository.existsByStatus(CrawlJobStatus.RUNNING)).isFalse();
            assertThat(crawlJobRepository.existsByStatus(CrawlJobStatus.COMPLETED)).isFalse();
            assertThat(crawlJobRepository.existsByStatus(CrawlJobStatus.FAILED)).isFalse();
        }
    }

    // ========== 4. Entity 상태 변경 테스트 ==========

    @Nested
    @DisplayName("Entity 상태 변경 - 실제 DB 반영")
    class EntityStateTests {

        /**
         * PENDING → RUNNING → COMPLETED 전체 생명주기 테스트
         *
         * 이것이 진정한 의미의 테스트:
         *   실제 DB에 저장하고, 상태를 변경하고, 다시 조회해서 확인
         */
        @Test
        @DisplayName("PENDING → RUNNING → COMPLETED 전체 생명주기")
        void fullLifecycle() {
            // 1단계: PENDING으로 생성
            CrawlJob job = crawlJobRepository.save(CrawlJob.builder().build());
            assertThat(job.getStatus()).isEqualTo(CrawlJobStatus.PENDING);

            // 2단계: RUNNING으로 변경
            job.markRunning();
            crawlJobRepository.save(job);
            assertThat(job.getStatus()).isEqualTo(CrawlJobStatus.RUNNING);

            // 3단계: COMPLETED로 변경
            job.markCompleted(150, "{\"hankyung\":50}");
            crawlJobRepository.save(job);
            assertThat(job.getStatus()).isEqualTo(CrawlJobStatus.COMPLETED);
            assertThat(job.getTotalArticles()).isEqualTo(150);
            assertThat(job.getCompletedAt()).isNotNull();

            // DB에서 다시 조회해서 최종 상태 확인
            CrawlJob reloaded = crawlJobRepository.findById(job.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(CrawlJobStatus.COMPLETED);
        }

        @Test
        @DisplayName("PENDING → FAILED 실패 경로")
        void failurePath() {
            // 1단계: PENDING으로 생성
            CrawlJob job = crawlJobRepository.save(CrawlJob.builder().build());

            // 2단계: FAILED로 변경
            job.markFailed("크롤러 타임아웃");
            crawlJobRepository.save(job);

            // then: 실패 상태 + 에러 메시지 + 완료 시간
            CrawlJob reloaded = crawlJobRepository.findById(job.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(CrawlJobStatus.FAILED);
            assertThat(reloaded.getErrorMessage()).isEqualTo("크롤러 타임아웃");
            assertThat(reloaded.getCompletedAt()).isNotNull();
        }
    }
}
