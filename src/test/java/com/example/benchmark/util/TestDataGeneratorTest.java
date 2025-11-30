package com.example.benchmark.util;

import com.example.benchmark.domain.TestRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TestDataGenerator 테스트 클래스.
 *
 * @author Developer Agent
 * @since 1.0
 */
@DisplayName("TestDataGenerator 테스트")
class TestDataGeneratorTest {

    @Nested
    @DisplayName("generate 메서드")
    class GenerateTest {

        @Test
        @DisplayName("정상: N건 생성 시 N개 레코드 반환")
        void shouldGenerateSpecifiedNumberOfRecords() {
            // given
            TestDataGenerator generator = new TestDataGenerator();
            int count = 100;

            // when
            List<TestRecord> records = generator.generate(count);

            // then
            assertThat(records).hasSize(count);
        }

        @Test
        @DisplayName("경계: 0건 요청 시 빈 리스트 반환")
        void shouldReturnEmptyListForZeroCount() {
            // given
            TestDataGenerator generator = new TestDataGenerator();

            // when
            List<TestRecord> records = generator.generate(0);

            // then
            assertThat(records).isEmpty();
        }

        @Test
        @DisplayName("정상: 생성된 레코드는 필수 필드가 채워져 있음")
        void shouldGenerateRecordsWithRequiredFields() {
            // given
            TestDataGenerator generator = new TestDataGenerator();

            // when
            List<TestRecord> records = generator.generate(10);

            // then
            for (TestRecord record : records) {
                assertThat(record.getData1()).isNotNull().isNotEmpty();
                assertThat(record.getCreatedAt()).isNotNull();
                assertThat(record.getStatus()).isNotNull();
                assertThat(record.getAmount()).isNotNull();
            }
        }

        @Test
        @DisplayName("예외: 음수 요청 시 IllegalArgumentException")
        void shouldThrowExceptionForNegativeCount() {
            // given
            TestDataGenerator generator = new TestDataGenerator();

            // when & then
            assertThatThrownBy(() -> generator.generate(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count must be non-negative");
        }
    }

    @Nested
    @DisplayName("seed 기반 생성")
    class SeedBasedGenerationTest {

        @Test
        @DisplayName("정상: 같은 seed로 생성 시 동일한 결과")
        void shouldGenerateSameRecordsWithSameSeed() {
            // given
            long seed = 12345L;
            int count = 50;

            // when
            List<TestRecord> records1 = TestDataGenerator.generateWithSeed(count, seed);
            List<TestRecord> records2 = TestDataGenerator.generateWithSeed(count, seed);

            // then
            assertThat(records1).hasSize(count);
            assertThat(records2).hasSize(count);

            for (int i = 0; i < count; i++) {
                assertThat(records1.get(i).getData1()).isEqualTo(records2.get(i).getData1());
                assertThat(records1.get(i).getAmount()).isEqualTo(records2.get(i).getAmount());
                assertThat(records1.get(i).getStatus()).isEqualTo(records2.get(i).getStatus());
            }
        }

        @Test
        @DisplayName("정상: 다른 seed로 생성 시 다른 결과")
        void shouldGenerateDifferentRecordsWithDifferentSeed() {
            // given
            int count = 50;

            // when
            List<TestRecord> records1 = TestDataGenerator.generateWithSeed(count, 1L);
            List<TestRecord> records2 = TestDataGenerator.generateWithSeed(count, 2L);

            // then
            // 모든 레코드가 동일할 확률은 거의 0에 수렴
            boolean allSame = true;
            for (int i = 0; i < count; i++) {
                if (!records1.get(i).getData1().equals(records2.get(i).getData1())) {
                    allSame = false;
                    break;
                }
            }
            assertThat(allSame).isFalse();
        }
    }

    @Nested
    @DisplayName("generateDefault 정적 메서드")
    class GenerateDefaultTest {

        @Test
        @DisplayName("정상: 지정된 개수만큼 레코드 생성")
        void shouldGenerateRecordsWithDefaultSettings() {
            // given
            int count = 25;

            // when
            List<TestRecord> records = TestDataGenerator.generateDefault(count);

            // then
            assertThat(records).hasSize(count);
        }
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상: seed 생성자로 생성된 인스턴스")
        void shouldCreateInstanceWithSeed() {
            // given & when
            TestDataGenerator generator = new TestDataGenerator(42L);

            // then
            List<TestRecord> records = generator.generate(10);
            assertThat(records).hasSize(10);
        }

        @Test
        @DisplayName("정상: 기본 생성자로 생성된 인스턴스")
        void shouldCreateInstanceWithDefaultConstructor() {
            // given & when
            TestDataGenerator generator = new TestDataGenerator();

            // then
            List<TestRecord> records = generator.generate(10);
            assertThat(records).hasSize(10);
        }
    }
}
