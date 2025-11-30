package com.example.benchmark.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * BenchmarkResult 테스트 클래스.
 *
 * @author Developer Agent
 * @since 1.0
 */
@DisplayName("BenchmarkResult 테스트")
class BenchmarkResultTest {

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("정상: 모든 필드가 설정된 인스턴스 생성")
        void shouldCreateInstanceWithAllFields() {
            // given
            Instant now = Instant.now();
            List<Long> durations = Arrays.asList(100L, 120L, 110L);

            // when
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .recordCount(10000)
                    .batchSize(1000)
                    .iterations(3)
                    .durations(durations)
                    .executedAt(now)
                    .build();

            // then
            assertThat(result.getRepositoryType()).isEqualTo("JDBC_BATCH");
            assertThat(result.getRecordCount()).isEqualTo(10000);
            assertThat(result.getBatchSize()).isEqualTo(1000);
            assertThat(result.getIterations()).isEqualTo(3);
            assertThat(result.getDurations()).containsExactly(100L, 120L, 110L);
            assertThat(result.getExecutedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("정상: addDuration으로 개별 추가")
        void shouldAddDurationIndividually() {
            // given & when
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .addDuration(100L)
                    .addDuration(150L)
                    .addDuration(125L)
                    .executedAt(Instant.now())
                    .build();

            // then
            assertThat(result.getDurations()).containsExactly(100L, 150L, 125L);
        }

        @Test
        @DisplayName("예외: repositoryType이 null인 경우")
        void shouldThrowExceptionWhenRepositoryTypeIsNull() {
            // given
            BenchmarkResult.Builder builder = BenchmarkResult.builder()
                    .recordCount(1000)
                    .executedAt(Instant.now());

            // when & then
            assertThatThrownBy(builder::build)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("repositoryType");
        }

        @Test
        @DisplayName("예외: executedAt이 null인 경우")
        void shouldThrowExceptionWhenExecutedAtIsNull() {
            // given
            BenchmarkResult.Builder builder = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .recordCount(1000)
                    .executedAt(null);

            // when & then
            assertThatThrownBy(builder::build)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("executedAt");
        }
    }

    @Nested
    @DisplayName("통계 계산 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("정상: 평균 실행 시간 계산")
        void shouldCalculateAverageDuration() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Arrays.asList(100L, 200L, 300L))
                    .executedAt(Instant.now())
                    .build();

            // when
            double avg = result.getAverageDuration();

            // then
            assertThat(avg).isCloseTo(200.0, within(0.01));
        }

        @Test
        @DisplayName("경계: 빈 durations일 때 평균은 0")
        void shouldReturnZeroAverageForEmptyDurations() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Collections.emptyList())
                    .executedAt(Instant.now())
                    .build();

            // when
            double avg = result.getAverageDuration();

            // then
            assertThat(avg).isZero();
        }

        @Test
        @DisplayName("정상: 최소/최대 시간 계산")
        void shouldCalculateMinAndMaxDuration() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Arrays.asList(150L, 100L, 200L, 125L))
                    .executedAt(Instant.now())
                    .build();

            // when & then
            assertThat(result.getMinDuration()).isEqualTo(100L);
            assertThat(result.getMaxDuration()).isEqualTo(200L);
        }

        @Test
        @DisplayName("경계: 빈 durations일 때 min/max는 0")
        void shouldReturnZeroMinMaxForEmptyDurations() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Collections.emptyList())
                    .executedAt(Instant.now())
                    .build();

            // when & then
            assertThat(result.getMinDuration()).isZero();
            assertThat(result.getMaxDuration()).isZero();
        }

        @Test
        @DisplayName("정상: 표준편차 계산")
        void shouldCalculateStandardDeviation() {
            // given
            // 표준편차 = sqrt(((100-200)^2 + (200-200)^2 + (300-200)^2) / 2) = sqrt(10000) = 100
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Arrays.asList(100L, 200L, 300L))
                    .executedAt(Instant.now())
                    .build();

            // when
            double stdDev = result.getStandardDeviation();

            // then
            assertThat(stdDev).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("경계: 단일 값일 때 표준편차는 0")
        void shouldReturnZeroStdDevForSingleValue() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Arrays.asList(100L))
                    .executedAt(Instant.now())
                    .build();

            // when
            double stdDev = result.getStandardDeviation();

            // then
            assertThat(stdDev).isZero();
        }

        @Test
        @DisplayName("정상: TPS 계산")
        void shouldCalculateAverageTps() {
            // given
            // 10000 records / 100ms = 100000 TPS
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .recordCount(10000)
                    .durations(Arrays.asList(100L, 100L, 100L))
                    .executedAt(Instant.now())
                    .build();

            // when
            double tps = result.getAverageTps();

            // then
            assertThat(tps).isCloseTo(100000.0, within(0.01));
        }

        @Test
        @DisplayName("경계: 평균 duration이 0일 때 TPS는 0")
        void shouldReturnZeroTpsWhenAverageDurationIsZero() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .recordCount(10000)
                    .durations(Collections.emptyList())
                    .executedAt(Instant.now())
                    .build();

            // when
            double tps = result.getAverageTps();

            // then
            assertThat(tps).isZero();
        }

        @Test
        @DisplayName("정상: 총 실행 시간 계산")
        void shouldCalculateTotalDuration() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Arrays.asList(100L, 200L, 300L))
                    .executedAt(Instant.now())
                    .build();

            // when
            long total = result.getTotalDuration();

            // then
            assertThat(total).isEqualTo(600L);
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("정상: durations 리스트는 불변")
        void shouldReturnUnmodifiableDurationsList() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .durations(Arrays.asList(100L, 200L))
                    .executedAt(Instant.now())
                    .build();

            // when & then
            assertThatThrownBy(() -> result.getDurations().add(300L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("정상: toString 출력 확인")
        void shouldReturnFormattedString() {
            // given
            BenchmarkResult result = BenchmarkResult.builder()
                    .repositoryType("JDBC_BATCH")
                    .recordCount(10000)
                    .batchSize(1000)
                    .iterations(3)
                    .durations(Arrays.asList(100L, 100L, 100L))
                    .executedAt(Instant.now())
                    .build();

            // when
            String str = result.toString();

            // then
            assertThat(str).contains("JDBC_BATCH");
            assertThat(str).contains("10000");
            assertThat(str).contains("1000");
        }
    }
}
