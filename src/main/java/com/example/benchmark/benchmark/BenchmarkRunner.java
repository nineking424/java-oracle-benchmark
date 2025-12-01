package com.example.benchmark.benchmark;

import com.example.benchmark.config.BenchmarkProperties;
import com.example.benchmark.domain.TestRecord;
import com.example.benchmark.repository.BatchInsertRepository;
import com.example.benchmark.repository.SingleInsertRepository;
import com.example.benchmark.util.TestDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.benchmark.util.StringUtils.repeat;

/**
 * 벤치마크 실행기.
 *
 * <p>JDBC와 MyBatis의 배치/단건 삽입 성능을 측정하고 비교합니다.</p>
 *
 * <p>실행 순서:</p>
 * <ol>
 *   <li>JVM 웜업 (warmupCount 건 삽입 후 삭제)</li>
 *   <li>JDBC Batch Insert 측정</li>
 *   <li>JDBC Single Insert 측정</li>
 *   <li>MyBatis Batch Insert 측정</li>
 *   <li>MyBatis Single Insert 측정</li>
 *   <li>결과 리포트 생성</li>
 * </ol>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Component
public class BenchmarkRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    private final BenchmarkProperties properties;
    private final List<BatchInsertRepository> batchRepositories;
    private final List<SingleInsertRepository> singleRepositories;
    private final BenchmarkReportGenerator reportGenerator;
    private final TestDataGenerator dataGenerator;

    /**
     * BenchmarkRunner 생성자.
     *
     * @param properties 벤치마크 설정
     * @param batchRepositories 배치 삽입 Repository 목록
     * @param singleRepositories 단건 삽입 Repository 목록
     * @param reportGenerator 리포트 생성기
     */
    public BenchmarkRunner(
            BenchmarkProperties properties,
            List<BatchInsertRepository> batchRepositories,
            List<SingleInsertRepository> singleRepositories,
            BenchmarkReportGenerator reportGenerator) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.batchRepositories = Objects.requireNonNull(batchRepositories, "batchRepositories must not be null");
        this.singleRepositories = Objects.requireNonNull(singleRepositories, "singleRepositories must not be null");
        this.reportGenerator = Objects.requireNonNull(reportGenerator, "reportGenerator must not be null");
        this.dataGenerator = new TestDataGenerator();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info(repeat("=", 60));
        log.info("Starting Oracle Insert Performance Benchmark");
        log.info(repeat("=", 60));
        log.info("Configuration: {}", properties);

        List<BenchmarkResult> results = new ArrayList<>();

        // JVM 웜업
        performWarmup();

        // 배치 삽입 벤치마크
        for (BatchInsertRepository repository : batchRepositories) {
            BenchmarkResult result = runBatchBenchmark(repository);
            results.add(result);
        }

        // 단건 삽입 벤치마크 (레코드 수가 작은 경우만)
        int singleRecordCount = Math.min(properties.getRecordCount(), 1000);
        for (SingleInsertRepository repository : singleRepositories) {
            BenchmarkResult result = runSingleBenchmark(repository, singleRecordCount);
            results.add(result);
        }

        // 리포트 생성
        reportGenerator.generateReport(results);

        log.info(repeat("=", 60));
        log.info("Benchmark completed");
        log.info(repeat("=", 60));
    }

    private void performWarmup() {
        if (properties.getWarmupCount() <= 0) {
            log.info("Skipping warmup (warmupCount=0)");
            return;
        }

        log.info(repeat("-", 60));
        log.info("Performing JVM warmup with {} records...", properties.getWarmupCount());

        List<TestRecord> warmupRecords = dataGenerator.generate(properties.getWarmupCount());

        for (BatchInsertRepository repository : batchRepositories) {
            repository.setBatchSize(properties.getBatchSize());
            repository.truncateTable();
            repository.insertBatch(warmupRecords);
            repository.truncateTable();
        }

        log.info("Warmup completed");
        log.info(repeat("-", 60));
    }

    private BenchmarkResult runBatchBenchmark(BatchInsertRepository repository) {
        log.info(repeat("-", 60));
        log.info("Running batch benchmark: {}", repository.getTypeName());
        log.info(repeat("-", 60));

        repository.setBatchSize(properties.getBatchSize());

        List<Long> durations = new ArrayList<>();

        for (int i = 1; i <= properties.getIterations(); i++) {
            log.info("Iteration {}/{}", i, properties.getIterations());

            // 테이블 초기화
            repository.truncateTable();

            // 테스트 데이터 생성
            List<TestRecord> records = dataGenerator.generate(properties.getRecordCount());

            // 벤치마크 실행
            long startTime = System.currentTimeMillis();
            repository.insertBatch(records);
            long duration = System.currentTimeMillis() - startTime;

            durations.add(duration);
            log.info("  Duration: {}ms, TPS: {}", duration, calculateTps(properties.getRecordCount(), duration));
        }

        // 테이블 정리
        repository.truncateTable();

        BenchmarkResult result = BenchmarkResult.builder()
                .repositoryType(repository.getTypeName())
                .recordCount(properties.getRecordCount())
                .batchSize(properties.getBatchSize())
                .iterations(properties.getIterations())
                .durations(durations)
                .executedAt(Instant.now())
                .build();

        log.info("Result: {}", result);
        return result;
    }

    private BenchmarkResult runSingleBenchmark(SingleInsertRepository repository, int recordCount) {
        log.info(repeat("-", 60));
        log.info("Running single insert benchmark: {} (limited to {} records)",
                repository.getTypeName(), recordCount);
        log.info(repeat("-", 60));

        List<Long> durations = new ArrayList<>();

        for (int i = 1; i <= properties.getIterations(); i++) {
            log.info("Iteration {}/{}", i, properties.getIterations());

            // 테이블 초기화
            repository.truncateTable();

            // 테스트 데이터 생성
            List<TestRecord> records = dataGenerator.generate(recordCount);

            // 벤치마크 실행
            long startTime = System.currentTimeMillis();
            repository.insertSingle(records);
            long duration = System.currentTimeMillis() - startTime;

            durations.add(duration);
            log.info("  Duration: {}ms, TPS: {}", duration, calculateTps(recordCount, duration));
        }

        // 테이블 정리
        repository.truncateTable();

        BenchmarkResult result = BenchmarkResult.builder()
                .repositoryType(repository.getTypeName())
                .recordCount(recordCount)
                .batchSize(1)
                .iterations(properties.getIterations())
                .durations(durations)
                .executedAt(Instant.now())
                .build();

        log.info("Result: {}", result);
        return result;
    }

    private String calculateTps(int count, long durationMs) {
        if (durationMs == 0) {
            return "N/A";
        }
        double tps = (count * 1000.0) / durationMs;
        return String.format("%.2f", tps);
    }
}
