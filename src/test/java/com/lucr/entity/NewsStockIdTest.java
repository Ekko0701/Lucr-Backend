package com.lucr.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * NewsStockId 복합키 단위 테스트
 *
 * - equals / hashCode 검증 (JPA 복합키 필수 요구사항)
 * - Getter 검증
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@DisplayName("NewsStockId 복합키 테스트")
class NewsStockIdTest {

    private static final UUID NEWS_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID NEWS_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String STOCK_CODE_1 = "005930";
    private static final String STOCK_CODE_2 = "035720";

    // ========== equals 테스트 ==========

    @Nested
    @DisplayName("equals()")
    class EqualsTests {

        @Test
        @DisplayName("같은 newsId + stockCode → equals true")
        void sameValues_AreEqual() {
            NewsStockId id1 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId id2 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(id1).isEqualTo(id2);
        }

        @Test
        @DisplayName("다른 newsId → equals false")
        void differentNewsId_AreNotEqual() {
            NewsStockId id1 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId id2 = new NewsStockId(NEWS_ID_2, STOCK_CODE_1);

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("다른 stockCode → equals false")
        void differentStockCode_AreNotEqual() {
            NewsStockId id1 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId id2 = new NewsStockId(NEWS_ID_1, STOCK_CODE_2);

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("null과 비교 → equals false")
        void compareWithNull_NotEqual() {
            NewsStockId id = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(id).isNotEqualTo(null);
        }

        @Test
        @DisplayName("자기 자신과 비교 → equals true")
        void compareWithSelf_IsEqual() {
            NewsStockId id = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(id).isEqualTo(id);
        }

        @Test
        @DisplayName("대칭성 — a.equals(b)이면 b.equals(a)")
        void equals_Symmetry() {
            NewsStockId a = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId b = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(a).isEqualTo(b);
            assertThat(b).isEqualTo(a);
        }

        @Test
        @DisplayName("추이성 — a==b && b==c이면 a==c")
        void equals_Transitivity() {
            NewsStockId a = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId b = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId c = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(a).isEqualTo(b);
            assertThat(b).isEqualTo(c);
            assertThat(a).isEqualTo(c);
        }

        @Test
        @DisplayName("다른 타입과 비교 — equals false")
        void equals_DifferentType_NotEqual() {
            NewsStockId id = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            String notAnId = "not-a-newsstock-id";

            assertThat(id).isNotEqualTo(notAnId);
        }
    }

    // ========== hashCode 테스트 ==========

    @Nested
    @DisplayName("hashCode()")
    class HashCodeTests {

        @Test
        @DisplayName("같은 값이면 같은 hashCode")
        void sameValues_SameHashCode() {
            NewsStockId id1 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId id2 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 다른 hashCode (높은 확률)")
        void differentValues_DifferentHashCode() {
            NewsStockId id1 = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            NewsStockId id2 = new NewsStockId(NEWS_ID_2, STOCK_CODE_2);

            assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("일관성 — 여러 번 호출해도 같은 hashCode")
        void hashCode_Consistency_MultipleCalls() {
            NewsStockId id = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);
            int firstCall = id.hashCode();

            assertThat(id.hashCode()).isEqualTo(firstCall);
            assertThat(id.hashCode()).isEqualTo(firstCall);
            assertThat(id.hashCode()).isEqualTo(firstCall);
        }
    }

    // ========== Getter 테스트 ==========

    @Nested
    @DisplayName("Getter")
    class GetterTests {

        @Test
        @DisplayName("newsId Getter 동작")
        void getNewsId_Works() {
            NewsStockId id = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(id.getNewsId()).isEqualTo(NEWS_ID_1);
        }

        @Test
        @DisplayName("stockCode Getter 동작")
        void getStockCode_Works() {
            NewsStockId id = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            assertThat(id.getStockCode()).isEqualTo(STOCK_CODE_1);
        }
    }

    // ========== 기본 생성자 테스트 ==========

    @Test
    @DisplayName("기본 생성자 — JPA 요구사항")
    void noArgsConstructor_Works() {
        NewsStockId id = new NewsStockId();

        assertThat(id.getNewsId()).isNull();
        assertThat(id.getStockCode()).isNull();
    }

    // ========== Serializable 테스트 ==========

    @Nested
    @DisplayName("Serializable")
    class SerializableTests {

        @Test
        @DisplayName("직렬화/역직렬화 라운드트립 — JPA 복합키 요구사항")
        void serializable_RoundTrip() throws Exception {
            // given
            NewsStockId original = new NewsStockId(NEWS_ID_1, STOCK_CODE_1);

            // when — 직렬화
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(original);
            oos.close();

            // when — 역직렬화
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            NewsStockId deserialized = (NewsStockId) ois.readObject();
            ois.close();

            // then
            assertThat(deserialized).isEqualTo(original);
            assertThat(deserialized.getNewsId()).isEqualTo(NEWS_ID_1);
            assertThat(deserialized.getStockCode()).isEqualTo(STOCK_CODE_1);
        }
    }
}
