package com.example.benchmark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 벤치마크 실행 관련 설정 프로퍼티.
 *
 * <p>application.yml의 benchmark 설정을 바인딩합니다.</p>
 *
 * <p>설정 항목:</p>
 * <ul>
 *   <li>batchSize: 배치 처리 크기 (기본값: 1000)</li>
 *   <li>recordCount: 테스트 레코드 수 (기본값: 100000)</li>
 *   <li>iterations: 반복 횟수 (기본값: 3)</li>
 *   <li>warmupCount: 웜업 레코드 수 (기본값: 1000)</li>
 * </ul>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkProperties {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_RECORD_COUNT = 100000;
    private static final int DEFAULT_ITERATIONS = 3;
    private static final int DEFAULT_WARMUP_COUNT = 1000;

    private int batchSize = DEFAULT_BATCH_SIZE;
    private int recordCount = DEFAULT_RECORD_COUNT;
    private int iterations = DEFAULT_ITERATIONS;
    private int warmupCount = DEFAULT_WARMUP_COUNT;

    /**
     * 배치 처리 크기를 반환합니다.
     *
     * @return 배치 크기
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 배치 처리 크기를 설정합니다.
     *
     * @param batchSize 배치 크기 (1 이상)
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 테스트 레코드 수를 반환합니다.
     *
     * @return 레코드 수
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * 테스트 레코드 수를 설정합니다.
     *
     * @param recordCount 레코드 수 (1 이상)
     */
    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    /**
     * 반복 횟수를 반환합니다.
     *
     * @return 반복 횟수
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * 반복 횟수를 설정합니다.
     *
     * @param iterations 반복 횟수 (1 이상)
     */
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    /**
     * 웜업 레코드 수를 반환합니다.
     *
     * @return 웜업 레코드 수
     */
    public int getWarmupCount() {
        return warmupCount;
    }

    /**
     * 웜업 레코드 수를 설정합니다.
     *
     * @param warmupCount 웜업 레코드 수 (0 이상)
     */
    public void setWarmupCount(int warmupCount) {
        this.warmupCount = warmupCount;
    }

    @Override
    public String toString() {
        return "BenchmarkProperties{" +
                "batchSize=" + batchSize +
                ", recordCount=" + recordCount +
                ", iterations=" + iterations +
                ", warmupCount=" + warmupCount +
                '}';
    }
}
