package com.example.benchmark.benchmark;

import com.example.benchmark.config.BenchmarkProperties;
import com.example.benchmark.domain.TestRecord;
import com.example.benchmark.repository.BatchInsertRepository;
import com.example.benchmark.repository.SingleInsertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BenchmarkRunner 테스트.
 *
 * @author Developer Agent
 * @since 1.0
 */
@DisplayName("BenchmarkRunner 테스트")
class BenchmarkRunnerTest {

    private BenchmarkProperties properties;
    private MockBatchInsertRepository mockBatchRepository;
    private MockSingleInsertRepository mockSingleRepository;
    private MockBenchmarkReportGenerator mockReportGenerator;

    @BeforeEach
    void setUp() {
        properties = new BenchmarkProperties();
        properties.setBatchSize(100);
        properties.setRecordCount(100);
        properties.setIterations(1);
        properties.setWarmupCount(10);

        mockBatchRepository = new MockBatchInsertRepository("JDBC Batch");
        mockSingleRepository = new MockSingleInsertRepository("JDBC Single");
        mockReportGenerator = new MockBenchmarkReportGenerator();
    }

    @Nested
    @DisplayName("생성자")
    class ConstructorTest {

        @Test
        @DisplayName("정상: 유효한 파라미터로 생성")
        void shouldCreateWithValidParameters() {
            // when & then
            assertThatCode(() -> new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    Collections.singletonList(mockSingleRepository),
                    mockReportGenerator
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("예외: null properties")
        void shouldThrowExceptionForNullProperties() {
            assertThatThrownBy(() -> new BenchmarkRunner(
                    null,
                    Collections.singletonList(mockBatchRepository),
                    Collections.singletonList(mockSingleRepository),
                    mockReportGenerator
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("properties");
        }

        @Test
        @DisplayName("예외: null batchRepositories")
        void shouldThrowExceptionForNullBatchRepositories() {
            assertThatThrownBy(() -> new BenchmarkRunner(
                    properties,
                    null,
                    Collections.singletonList(mockSingleRepository),
                    mockReportGenerator
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("batchRepositories");
        }

        @Test
        @DisplayName("예외: null singleRepositories")
        void shouldThrowExceptionForNullSingleRepositories() {
            assertThatThrownBy(() -> new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    null,
                    mockReportGenerator
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("singleRepositories");
        }

        @Test
        @DisplayName("예외: null reportGenerator")
        void shouldThrowExceptionForNullReportGenerator() {
            assertThatThrownBy(() -> new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    Collections.singletonList(mockSingleRepository),
                    null
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("reportGenerator");
        }
    }

    @Nested
    @DisplayName("run 메서드")
    class RunTest {

        @Test
        @DisplayName("정상: 벤치마크 실행 및 리포트 생성")
        void shouldRunBenchmarkAndGenerateReport() throws Exception {
            // given
            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    Collections.singletonList(mockSingleRepository),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then
            assertThat(mockReportGenerator.isReportGenerated()).isTrue();
            assertThat(mockReportGenerator.getResults()).hasSize(2); // batch + single
        }

        @Test
        @DisplayName("정상: 빈 repository 목록으로 실행")
        void shouldRunWithEmptyRepositories() throws Exception {
            // given
            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then
            assertThat(mockReportGenerator.isReportGenerated()).isTrue();
            assertThat(mockReportGenerator.getResults()).isEmpty();
        }

        @Test
        @DisplayName("정상: warmup이 0일 때 웜업 스킵")
        void shouldSkipWarmupWhenZero() throws Exception {
            // given
            properties.setWarmupCount(0);
            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    Collections.singletonList(mockSingleRepository),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then - warmup이 스킵되어도 벤치마크는 실행됨
            assertThat(mockReportGenerator.isReportGenerated()).isTrue();
        }

        @Test
        @DisplayName("정상: 다중 repository 벤치마크 실행")
        void shouldRunWithMultipleRepositories() throws Exception {
            // given
            MockBatchInsertRepository batchRepo1 = new MockBatchInsertRepository("JDBC Batch");
            MockBatchInsertRepository batchRepo2 = new MockBatchInsertRepository("MyBatis Batch");
            MockSingleInsertRepository singleRepo1 = new MockSingleInsertRepository("JDBC Single");
            MockSingleInsertRepository singleRepo2 = new MockSingleInsertRepository("MyBatis Single");

            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Arrays.asList(batchRepo1, batchRepo2),
                    Arrays.asList(singleRepo1, singleRepo2),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then
            assertThat(mockReportGenerator.getResults()).hasSize(4); // 2 batch + 2 single
        }

        @Test
        @DisplayName("정상: single insert 레코드 수 제한 (최대 1000건)")
        void shouldLimitSingleInsertRecordCount() throws Exception {
            // given
            properties.setRecordCount(10000); // 10000건 요청하지만 single은 1000건으로 제한
            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Collections.emptyList(),
                    Collections.singletonList(mockSingleRepository),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then
            assertThat(mockSingleRepository.getLastRecordCount()).isLessThanOrEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("벤치마크 결과")
    class BenchmarkResultTest {

        @Test
        @DisplayName("정상: 다중 iteration 실행")
        void shouldRunMultipleIterations() throws Exception {
            // given
            properties.setIterations(3);
            properties.setWarmupCount(0); // warmup 비활성화
            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    Collections.emptyList(),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then
            assertThat(mockBatchRepository.getInsertCount()).isEqualTo(3); // 3 iterations
        }

        @Test
        @DisplayName("정상: 결과에 올바른 repository type 포함")
        void shouldIncludeCorrectRepositoryType() throws Exception {
            // given
            BenchmarkRunner runner = new BenchmarkRunner(
                    properties,
                    Collections.singletonList(mockBatchRepository),
                    Collections.emptyList(),
                    mockReportGenerator
            );

            // when
            runner.run();

            // then
            assertThat(mockReportGenerator.getResults().get(0).getRepositoryType())
                    .isEqualTo("JDBC Batch");
        }
    }

    // ========== Mock Classes ==========

    /**
     * 테스트용 BatchInsertRepository Mock.
     */
    private static class MockBatchInsertRepository implements BatchInsertRepository {
        private final String typeName;
        private int batchSize = 1000;
        private int insertCount = 0;

        MockBatchInsertRepository(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public int insertBatch(List<TestRecord> records) {
            insertCount++;
            return records.size();
        }

        @Override
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public int getBatchSize() {
            return batchSize;
        }

        @Override
        public void truncateTable() {
            // no-op
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        int getInsertCount() {
            return insertCount;
        }
    }

    /**
     * 테스트용 SingleInsertRepository Mock.
     */
    private static class MockSingleInsertRepository implements SingleInsertRepository {
        private final String typeName;
        private int lastRecordCount = 0;

        MockSingleInsertRepository(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public int insertSingle(List<TestRecord> records) {
            lastRecordCount = records.size();
            return records.size();
        }

        @Override
        public void truncateTable() {
            // no-op
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        int getLastRecordCount() {
            return lastRecordCount;
        }
    }

    /**
     * 테스트용 BenchmarkReportGenerator Mock.
     */
    private static class MockBenchmarkReportGenerator extends BenchmarkReportGenerator {
        private boolean reportGenerated = false;
        private List<BenchmarkResult> results = new ArrayList<>();

        @Override
        public void generateReport(List<BenchmarkResult> results) {
            this.reportGenerated = true;
            this.results = results != null ? results : new ArrayList<>();
        }

        boolean isReportGenerated() {
            return reportGenerated;
        }

        List<BenchmarkResult> getResults() {
            return results;
        }
    }
}
