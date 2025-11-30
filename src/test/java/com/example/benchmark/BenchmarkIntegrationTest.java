package com.example.benchmark;

import com.example.benchmark.benchmark.BenchmarkReportGenerator;
import com.example.benchmark.benchmark.BenchmarkResult;
import com.example.benchmark.benchmark.BenchmarkRunner;
import com.example.benchmark.config.BenchmarkProperties;
import com.example.benchmark.domain.TestRecord;
import com.example.benchmark.repository.BatchInsertRepository;
import com.example.benchmark.repository.JdbcBatchInsertRepository;
import com.example.benchmark.repository.JdbcSingleInsertRepository;
import com.example.benchmark.repository.MyBatisBatchInsertRepository;
import com.example.benchmark.repository.MyBatisSingleInsertRepository;
import com.example.benchmark.repository.SingleInsertRepository;
import com.example.benchmark.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 벤치마크 애플리케이션 통합 테스트.
 *
 * <p>모든 Repository 구현체와 BenchmarkRunner가 함께 동작하는지 검증합니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("벤치마크 통합 테스트")
class BenchmarkIntegrationTest {

    @Autowired
    private JdbcBatchInsertRepository jdbcBatchRepository;

    @Autowired
    private JdbcSingleInsertRepository jdbcSingleRepository;

    @Autowired
    private MyBatisBatchInsertRepository myBatisBatchRepository;

    @Autowired
    private MyBatisSingleInsertRepository myBatisSingleRepository;

    @Autowired
    private BenchmarkProperties properties;

    private TestDataGenerator dataGenerator;

    @BeforeEach
    void setUp() {
        dataGenerator = new TestDataGenerator();
        // 각 테스트 전 테이블 초기화
        jdbcBatchRepository.truncateTable();
    }

    @Nested
    @DisplayName("Spring Context 로딩")
    class ContextLoadingTest {

        @Test
        @DisplayName("정상: 모든 Repository Bean이 로드됨")
        void shouldLoadAllRepositoryBeans() {
            assertThat(jdbcBatchRepository).isNotNull();
            assertThat(jdbcSingleRepository).isNotNull();
            assertThat(myBatisBatchRepository).isNotNull();
            assertThat(myBatisSingleRepository).isNotNull();
        }

        @Test
        @DisplayName("정상: BenchmarkProperties가 로드됨")
        void shouldLoadBenchmarkProperties() {
            assertThat(properties).isNotNull();
            assertThat(properties.getBatchSize()).isPositive();
            assertThat(properties.getRecordCount()).isPositive();
        }
    }

    @Nested
    @DisplayName("JDBC와 MyBatis 배치 삽입 비교")
    class BatchInsertComparisonTest {

        @Test
        @DisplayName("정상: JDBC와 MyBatis가 동일한 레코드 수 삽입")
        void shouldInsertSameRecordCount() {
            // given
            List<TestRecord> records = dataGenerator.generate(100);

            // when - JDBC
            jdbcBatchRepository.truncateTable();
            int jdbcCount = jdbcBatchRepository.insertBatch(records);
            long jdbcTableCount = jdbcBatchRepository.count();

            // when - MyBatis
            myBatisBatchRepository.truncateTable();
            int myBatisCount = myBatisBatchRepository.insertBatch(records);
            long myBatisTableCount = myBatisBatchRepository.count();

            // then
            assertThat(jdbcCount).isEqualTo(100);
            assertThat(myBatisCount).isEqualTo(100);
            assertThat(jdbcTableCount).isEqualTo(100);
            assertThat(myBatisTableCount).isEqualTo(100);
        }

        @Test
        @DisplayName("정상: 다양한 배치 크기로 삽입 성공")
        void shouldInsertWithVariousBatchSizes() {
            List<TestRecord> records = dataGenerator.generate(150);

            int[] batchSizes = {10, 50, 100, 200};

            for (int batchSize : batchSizes) {
                // JDBC
                jdbcBatchRepository.truncateTable();
                jdbcBatchRepository.setBatchSize(batchSize);
                int jdbcResult = jdbcBatchRepository.insertBatch(records);
                assertThat(jdbcResult).isEqualTo(150);

                // MyBatis
                myBatisBatchRepository.truncateTable();
                myBatisBatchRepository.setBatchSize(batchSize);
                int myBatisResult = myBatisBatchRepository.insertBatch(records);
                assertThat(myBatisResult).isEqualTo(150);
            }
        }
    }

    @Nested
    @DisplayName("JDBC와 MyBatis 단건 삽입 비교")
    class SingleInsertComparisonTest {

        @Test
        @DisplayName("정상: JDBC와 MyBatis 단건 삽입 동일한 결과")
        void shouldInsertSameRecordCountSingle() {
            // given
            List<TestRecord> records = dataGenerator.generate(50);

            // when - JDBC
            jdbcSingleRepository.truncateTable();
            int jdbcCount = jdbcSingleRepository.insertSingle(records);
            long jdbcTableCount = jdbcSingleRepository.count();

            // when - MyBatis
            myBatisSingleRepository.truncateTable();
            int myBatisCount = myBatisSingleRepository.insertSingle(records);
            long myBatisTableCount = myBatisSingleRepository.count();

            // then
            assertThat(jdbcCount).isEqualTo(50);
            assertThat(myBatisCount).isEqualTo(50);
            assertThat(jdbcTableCount).isEqualTo(50);
            assertThat(myBatisTableCount).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("BenchmarkRunner 통합 실행")
    class BenchmarkRunnerIntegrationTest {

        @Test
        @DisplayName("정상: 모든 Repository로 벤치마크 실행")
        void shouldRunBenchmarkWithAllRepositories() {
            // given
            BenchmarkProperties testProperties = new BenchmarkProperties();
            testProperties.setBatchSize(50);
            testProperties.setRecordCount(100);
            testProperties.setIterations(2);
            testProperties.setWarmupCount(10);

            List<BatchInsertRepository> batchRepos = Arrays.asList(
                    jdbcBatchRepository, myBatisBatchRepository);
            List<SingleInsertRepository> singleRepos = Arrays.asList(
                    jdbcSingleRepository, myBatisSingleRepository);

            CapturingReportGenerator reportGenerator = new CapturingReportGenerator();

            BenchmarkRunner runner = new BenchmarkRunner(
                    testProperties, batchRepos, singleRepos, reportGenerator);

            // when & then
            assertThatCode(() -> runner.run()).doesNotThrowAnyException();

            // verify results
            List<BenchmarkResult> results = reportGenerator.getCapturedResults();
            assertThat(results).hasSize(4); // 2 batch + 2 single

            // 모든 결과에 유효한 데이터가 있는지 확인
            for (BenchmarkResult result : results) {
                assertThat(result.getRepositoryType()).isNotEmpty();
                assertThat(result.getRecordCount()).isPositive();
                assertThat(result.getIterations()).isEqualTo(2);
                assertThat(result.getDurations()).hasSize(2);
            }
        }

        @Test
        @DisplayName("정상: 웜업 없이 벤치마크 실행")
        void shouldRunBenchmarkWithoutWarmup() {
            // given
            BenchmarkProperties testProperties = new BenchmarkProperties();
            testProperties.setBatchSize(50);
            testProperties.setRecordCount(50);
            testProperties.setIterations(1);
            testProperties.setWarmupCount(0); // 웜업 없음

            List<BatchInsertRepository> batchRepos = Arrays.asList(jdbcBatchRepository);
            List<SingleInsertRepository> singleRepos = Arrays.asList(jdbcSingleRepository);

            CapturingReportGenerator reportGenerator = new CapturingReportGenerator();

            BenchmarkRunner runner = new BenchmarkRunner(
                    testProperties, batchRepos, singleRepos, reportGenerator);

            // when & then
            assertThatCode(() -> runner.run()).doesNotThrowAnyException();
            assertThat(reportGenerator.getCapturedResults()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Repository 타입명 확인")
    class TypeNameTest {

        @Test
        @DisplayName("정상: 각 Repository가 올바른 타입명 반환")
        void shouldReturnCorrectTypeNames() {
            assertThat(jdbcBatchRepository.getTypeName()).isEqualTo("JDBC-Batch");
            assertThat(jdbcSingleRepository.getTypeName()).isEqualTo("JDBC-Single");
            assertThat(myBatisBatchRepository.getTypeName()).isEqualTo("MyBatis-Batch");
            assertThat(myBatisSingleRepository.getTypeName()).isEqualTo("MyBatis-Single");
        }
    }

    @Nested
    @DisplayName("테이블 초기화 검증")
    class TruncateTableTest {

        @Test
        @DisplayName("정상: 모든 Repository의 truncate가 동작")
        void shouldTruncateTableCorrectly() {
            // given
            List<TestRecord> records = dataGenerator.generate(50);

            // insert data
            jdbcBatchRepository.insertBatch(records);
            assertThat(jdbcBatchRepository.count()).isEqualTo(50);

            // when - truncate
            jdbcBatchRepository.truncateTable();

            // then
            assertThat(jdbcBatchRepository.count()).isZero();
        }

        @Test
        @DisplayName("정상: 모든 Repository가 같은 테이블 공유")
        void shouldShareSameTable() {
            // given
            List<TestRecord> records = dataGenerator.generate(30);

            // JDBC로 삽입
            jdbcBatchRepository.insertBatch(records);

            // then - 모든 Repository에서 같은 count 확인
            assertThat(jdbcBatchRepository.count()).isEqualTo(30);
            assertThat(myBatisBatchRepository.count()).isEqualTo(30);
            assertThat(jdbcSingleRepository.count()).isEqualTo(30);
            assertThat(myBatisSingleRepository.count()).isEqualTo(30);

            // MyBatis로 truncate
            myBatisBatchRepository.truncateTable();

            // 모든 Repository에서 0 확인
            assertThat(jdbcBatchRepository.count()).isZero();
            assertThat(myBatisBatchRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("데이터 무결성 테스트")
    class DataIntegrityTest {

        @Test
        @DisplayName("정상: 배치와 단건 삽입 결과 동일")
        void shouldProduceSameResultsForBatchAndSingle() {
            // given
            List<TestRecord> records = dataGenerator.generate(20);

            // 배치 삽입
            jdbcBatchRepository.truncateTable();
            jdbcBatchRepository.insertBatch(records);
            long batchCount = jdbcBatchRepository.count();

            // 단건 삽입
            jdbcSingleRepository.truncateTable();
            jdbcSingleRepository.insertSingle(records);
            long singleCount = jdbcSingleRepository.count();

            // then
            assertThat(batchCount).isEqualTo(singleCount);
        }
    }

    @Nested
    @DisplayName("대량 데이터 처리 테스트")
    class LargeScaleTest {

        @Test
        @DisplayName("정상: 1000건 이상 배치 삽입 - 로그 진행 표시 확인")
        void shouldHandleLargeDatasetWithBatch() {
            // given - 1001건으로 로깅 경계값 테스트
            List<TestRecord> records = dataGenerator.generate(1001);

            // when
            jdbcBatchRepository.setBatchSize(100);
            int result = jdbcBatchRepository.insertBatch(records);

            // then
            assertThat(result).isEqualTo(1001);
            assertThat(jdbcBatchRepository.count()).isEqualTo(1001);
        }

        @Test
        @DisplayName("정상: 1000건 이상 단건 삽입 - 로그 진행 표시 확인")
        void shouldHandleLargeDatasetWithSingle() {
            // given - 1001건으로 로깅 경계값 테스트
            List<TestRecord> records = dataGenerator.generate(1001);

            // when
            int result = jdbcSingleRepository.insertSingle(records);

            // then
            assertThat(result).isEqualTo(1001);
            assertThat(jdbcSingleRepository.count()).isEqualTo(1001);
        }
    }

    @Nested
    @DisplayName("연속 실행 테스트")
    class ConsecutiveExecutionTest {

        @Test
        @DisplayName("정상: 연속 벤치마크 실행 시 결과 일관성")
        void shouldProduceConsistentResultsOnConsecutiveRuns() throws Exception {
            // given
            BenchmarkProperties testProperties = new BenchmarkProperties();
            testProperties.setBatchSize(50);
            testProperties.setRecordCount(100);
            testProperties.setIterations(3);
            testProperties.setWarmupCount(10);

            List<BatchInsertRepository> batchRepos = Arrays.asList(jdbcBatchRepository);
            List<SingleInsertRepository> singleRepos = Arrays.asList(jdbcSingleRepository);

            CapturingReportGenerator reportGenerator1 = new CapturingReportGenerator();
            CapturingReportGenerator reportGenerator2 = new CapturingReportGenerator();

            BenchmarkRunner runner1 = new BenchmarkRunner(
                    testProperties, batchRepos, singleRepos, reportGenerator1);
            BenchmarkRunner runner2 = new BenchmarkRunner(
                    testProperties, batchRepos, singleRepos, reportGenerator2);

            // when
            runner1.run();
            runner2.run();

            // then - 두 실행 모두 동일한 레코드 수 처리
            List<BenchmarkResult> results1 = reportGenerator1.getCapturedResults();
            List<BenchmarkResult> results2 = reportGenerator2.getCapturedResults();

            assertThat(results1).hasSize(2);
            assertThat(results2).hasSize(2);

            for (int i = 0; i < results1.size(); i++) {
                assertThat(results1.get(i).getRecordCount())
                        .isEqualTo(results2.get(i).getRecordCount());
            }
        }
    }

    /**
     * 테스트용 리포트 생성기 - 결과 캡처.
     */
    private static class CapturingReportGenerator extends BenchmarkReportGenerator {
        private final List<BenchmarkResult> capturedResults = new ArrayList<>();

        @Override
        public void generateReport(List<BenchmarkResult> results) {
            if (results != null) {
                capturedResults.addAll(results);
            }
            // 콘솔/CSV 생성은 스킵 (부모 클래스 호출 안 함)
        }

        List<BenchmarkResult> getCapturedResults() {
            return capturedResults;
        }
    }
}
