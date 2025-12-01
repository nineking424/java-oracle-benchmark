package com.example.benchmark.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.example.benchmark.util.StringUtils.repeat;

/**
 * 벤치마크 결과 리포트 생성기.
 *
 * <p>벤치마크 결과를 콘솔에 출력하고 CSV 파일로 저장합니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Component
public class BenchmarkReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkReportGenerator.class);

    private static final String CSV_HEADER = "Type,RecordCount,BatchSize,Iterations,AvgDuration(ms),MinDuration(ms),MaxDuration(ms),StdDev(ms),AvgTPS";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 벤치마크 결과 리포트를 생성합니다.
     *
     * @param results 벤치마크 결과 목록
     */
    public void generateReport(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            log.warn("No benchmark results to report");
            return;
        }

        printConsoleReport(results);
        saveCsvReport(results);
    }

    private void printConsoleReport(List<BenchmarkResult> results) {
        log.info("");
        log.info(repeat("=", 100));
        log.info("BENCHMARK RESULTS SUMMARY");
        log.info(repeat("=", 100));
        log.info("");

        // 헤더 출력
        String headerFormat = "| %-20s | %12s | %10s | %10s | %12s | %12s | %12s | %12s |";
        String rowFormat = "| %-20s | %,12d | %10d | %10d | %12.2f | %12.2f | %12d | %12d |";

        log.info(String.format(headerFormat,
                "Repository Type", "Records", "BatchSize", "Iterations", "Avg TPS", "Std Dev(ms)", "Min(ms)", "Max(ms)"));
        log.info(repeat("-", 120));

        for (BenchmarkResult result : results) {
            log.info(String.format(rowFormat,
                    result.getRepositoryType(),
                    result.getRecordCount(),
                    result.getBatchSize(),
                    result.getIterations(),
                    result.getAverageTps(),
                    result.getStandardDeviation(),
                    result.getMinDuration(),
                    result.getMaxDuration()));
        }

        log.info(repeat("-", 120));
        log.info("");

        // 상세 결과 출력
        log.info("DETAILED RESULTS:");
        log.info(repeat("-", 60));

        for (BenchmarkResult result : results) {
            log.info("");
            log.info("[{}]", result.getRepositoryType());
            log.info("  Records: {}, BatchSize: {}, Iterations: {}",
                    result.getRecordCount(), result.getBatchSize(), result.getIterations());
            log.info("  Durations: {} ms", result.getDurations());
            log.info("  Average Duration: {} ms", String.format("%.2f", result.getAverageDuration()));
            log.info("  Average TPS: {}", String.format("%.2f", result.getAverageTps()));
            log.info("  Std Deviation: {} ms", String.format("%.2f", result.getStandardDeviation()));
            log.info("  Min/Max: {} / {} ms", result.getMinDuration(), result.getMaxDuration());
        }

        log.info("");

        // 성능 비교
        printPerformanceComparison(results);
    }

    private void printPerformanceComparison(List<BenchmarkResult> results) {
        if (results.size() < 2) {
            return;
        }

        log.info("PERFORMANCE COMPARISON:");
        log.info(repeat("-", 60));

        BenchmarkResult fastest = results.get(0);
        for (BenchmarkResult result : results) {
            if (result.getAverageTps() > fastest.getAverageTps()) {
                fastest = result;
            }
        }

        log.info("Fastest: {} with {} TPS", fastest.getRepositoryType(),
                String.format("%.2f", fastest.getAverageTps()));
        log.info("");

        for (BenchmarkResult result : results) {
            if (!result.getRepositoryType().equals(fastest.getRepositoryType())) {
                double ratio = fastest.getAverageTps() / result.getAverageTps();
                log.info("  vs {}: {}x faster", result.getRepositoryType(), String.format("%.2f", ratio));
            }
        }

        log.info("");
    }

    private void saveCsvReport(List<BenchmarkResult> results) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        String filename = "benchmark_result_" + timestamp + ".csv";
        Path dirPath = Paths.get("benchmark-results");
        Path filePath = dirPath.resolve(filename);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            log.error("Failed to create benchmark-results directory: {}", e.getMessage());
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath, StandardCharsets.UTF_8))) {
            writer.println(CSV_HEADER);

            for (BenchmarkResult result : results) {
                writer.printf("%s,%d,%d,%d,%.2f,%d,%d,%.2f,%.2f%n",
                        result.getRepositoryType(),
                        result.getRecordCount(),
                        result.getBatchSize(),
                        result.getIterations(),
                        result.getAverageDuration(),
                        result.getMinDuration(),
                        result.getMaxDuration(),
                        result.getStandardDeviation(),
                        result.getAverageTps());
            }

            log.info("CSV report saved to: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save CSV report: {}", e.getMessage(), e);
        }
    }
}
