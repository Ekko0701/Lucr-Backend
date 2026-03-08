package com.lucr.repository;

import com.lucr.entity.Keyword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KeywordRepository 통합 테스트
 *
 * 핵심 목적:
 * 1) 메서드 이름 기반 쿼리(findByWord, existsByWord, findByWordContaining)가
 *    실제 DB에서 의도대로 동작하는지 검증
 * 2) 커스텀 쿼리(findTopKeywords, findTopNKeywords)의 정렬/개수 조건 검증
 *
 * @DataJpaTest:
 * - JPA 관련 빈만 로드
 * - H2 인메모리 DB 사용
 * - 테스트 종료 시 자동 롤백
 */
@DataJpaTest
@DisplayName("KeywordRepository 테스트")
class KeywordRepositoryTest {

    @Autowired
    private KeywordRepository keywordRepository;

    @BeforeEach
    void setUp() {
        keywordRepository.save(Keyword.builder().word("삼성전자").frequency(15).build());
        keywordRepository.save(Keyword.builder().word("반도체").frequency(30).build());
        keywordRepository.save(Keyword.builder().word("금리").frequency(7).build());
    }

    @Nested
    @DisplayName("findByWord()/existsByWord()")
    class BasicLookupTests {

        @Test
        @DisplayName("존재하는 단어 조회 - Optional 반환")
        void findByWord_Exists_ReturnsKeyword() {
            Optional<Keyword> result = keywordRepository.findByWord("삼성전자");

            assertThat(result).isPresent();
            assertThat(result.get().getFrequency()).isEqualTo(15);
        }

        @Test
        @DisplayName("존재하지 않는 단어 조회 - 빈 Optional")
        void findByWord_NotExists_ReturnsEmpty() {
            Optional<Keyword> result = keywordRepository.findByWord("테슬라");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("단어 존재 여부 확인")
        void existsByWord_WorksAsExpected() {
            assertThat(keywordRepository.existsByWord("반도체")).isTrue();
            assertThat(keywordRepository.existsByWord("환율")).isFalse();
        }
    }

    @Nested
    @DisplayName("정렬/검색 쿼리")
    class QueryTests {

        @Test
        @DisplayName("빈도수 내림차순 상위 키워드 조회")
        void findTopKeywords_OrderByFrequencyDesc() {
            Page<Keyword> page = keywordRepository.findTopKeywords(PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent())
                    .extracting(Keyword::getWord)
                    .containsExactly("반도체", "삼성전자");
        }

        @Test
        @DisplayName("부분 문자열 검색")
        void findByWordContaining_PartialMatch() {
            List<Keyword> result = keywordRepository.findByWordContaining("삼성");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getWord()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("네이티브 쿼리로 상위 N개 조회")
        void findTopNKeywords_ReturnsLimitedRows() {
            List<Keyword> result = keywordRepository.findTopNKeywords(2);

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(Keyword::getWord)
                    .containsExactly("반도체", "삼성전자");
        }
    }
}
