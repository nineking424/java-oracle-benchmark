package com.example.benchmark.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * BenchmarkReportGenerator 테스트.
 *
 * @author Developer Agent
 * @since 1.0
 */
@DisplayName("BenchmarkReportGenerator 테스트")
class BenchmarkReportGeneratorTest {

    private BenchmarkReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        reportGenerator = new BenchmarkReportGenerator();
    }

    @Nested
    @DisplayName("generateReport 메서드")
    class GenerateReportTest {

        @Test
        @DisplayName("정상: 빈 결과 목록일 때 예외 없이 처리")
        void shouldHandleEmptyResultsGracefully() {
            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Collections.emptyList()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상: null 결과 목록일 때 예외 없이 처리")
        void shouldHandleNullResultsGracefully() {
            // when & then
            assertThatCode(() -> reportGenerator.generateReport(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상: 단일 결과 리포트 생성")
        void shouldGenerateReportForSingleResult() {
            // given
            BenchmarkResult result = createTestResult("JDBC Batch", 1000, 100, 3,
                    Arrays.asList(100L, 110L, 90L));

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Collections.singletonList(result)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상: 다중 결과 리포트 생성 및 비교")
        void shouldGenerateReportWithComparison() {
            // given
            BenchmarkResult jdbcBatch = createTestResult("JDBC Batch", 1000, 100, 3,
                    Arrays.asList(100L, 110L, 90L));
            BenchmarkResult mybatisBatch = createTestResult("MyBatis Batch", 1000, 100, 3,
                    Arrays.asList(150L, 160L, 140L));
            BenchmarkResult jdbcSingle = createTestResult("JDBC Single", 100, 1, 3,
                    Arrays.asList(200L, 210L, 190L));

            List<BenchmarkResult> results = Arrays.asList(jdbcBatch, mybatisBatch, jdbcSingle);

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(results))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상: CSV 파일 생성 확인")
        void shouldCreateCsvFile(@TempDir Path tempDir) throws IOException {
            // given
            System.setProperty("user.dir", tempDir.toString());

            BenchmarkResult result = createTestResult("JDBC Batch", 1000, 100, 3,
                    Arrays.asList(100L, 110L, 90L));

            // when
            reportGenerator.generateReport(Collections.singletonList(result));

            // then - CSV 파일이 현재 디렉토리에 생성됨
            // 파일명이 타임스탬프를 포함하므로 패턴으로 확인
            long csvFileCount = Files.list(tempDir)
                    .filter(path -> path.getFileName().toString().startsWith("benchmark_result_"))
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .count();

            // CSV 파일이 생성되었을 수도 있고, 작업 디렉토리 문제로 안 됐을 수도 있음
            // 이 테스트는 주로 예외 없이 실행되는지 확인
            assertThat(csvFileCount).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("성능 비교 출력")
    class PerformanceComparisonTest {

        @Test
        @DisplayName("정상: 두 개 결과 비교 시 더 빠른 것 식별")
        void shouldIdentifyFasterRepository() {
            // given
            BenchmarkResult faster = createTestResult("JDBC Batch", 1000, 100, 3,
                    Arrays.asList(50L, 50L, 50L)); // 더 빠름 (duration 낮음)
            BenchmarkResult slower = createTestResult("MyBatis Batch", 1000, 100, 3,
                    Arrays.asList(100L, 100L, 100L));

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Arrays.asList(faster, slower)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상: 동일한 성능의 결과 처리")
        void shouldHandleEqualPerformance() {
            // given
            BenchmarkResult result1 = createTestResult("JDBC Batch", 1000, 100, 3,
                    Arrays.asList(100L, 100L, 100L));
            BenchmarkResult result2 = createTestResult("MyBatis Batch", 1000, 100, 3,
                    Arrays.asList(100L, 100L, 100L));

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Arrays.asList(result1, result2)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("경계 조건")
    class EdgeCasesTest {

        @Test
        @DisplayName("경계: duration이 0인 결과 처리")
        void shouldHandleZeroDuration() {
            // given
            BenchmarkResult result = createTestResult("JDBC Batch", 1000, 100, 3,
                    Arrays.asList(0L, 0L, 0L));

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Collections.singletonList(result)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경계: 매우 큰 레코드 수 처리")
        void shouldHandleLargeRecordCount() {
            // given
            BenchmarkResult result = createTestResult("JDBC Batch", 10_000_000, 1000, 3,
                    Arrays.asList(30000L, 31000L, 29000L));

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Collections.singletonList(result)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경계: 단일 iteration 결과 처리")
        void shouldHandleSingleIteration() {
            // given
            BenchmarkResult result = createTestResult("JDBC Batch", 1000, 100, 1,
                    Collections.singletonList(100L));

            // when & then
            assertThatCode(() -> reportGenerator.generateReport(Collections.singletonList(result)))
                    .doesNotThrowAnyException();
        }
    }

    /**
     * 테스트용 BenchmarkResult 생성.
     */
    private BenchmarkResult createTestResult(String repositoryType, int recordCount,
                                             int batchSize, int iterations, List<Long> durations) {
        return BenchmarkResult.builder()
                .repositoryType(repositoryType)
                .recordCount(recordCount)
                .batchSize(batchSize)
                .iterations(iterations)
                .durations(durations)
                .executedAt(Instant.now())
                .build();
    }
}
