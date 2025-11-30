package com.example.benchmark.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 벤치마크 실행 결과를 담는 불변 클래스.
 *
 * <p>각 반복 실행의 결과와 통계 정보를 포함합니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
public final class BenchmarkResult {

    private final String repositoryType;
    private final int recordCount;
    private final int batchSize;
    private final int iterations;
    private final List<Long> durations;
    private final Instant executedAt;

    private BenchmarkResult(Builder builder) {
        this.repositoryType = Objects.requireNonNull(builder.repositoryType, "repositoryType must not be null");
        this.recordCount = builder.recordCount;
        this.batchSize = builder.batchSize;
        this.iterations = builder.iterations;
        this.durations = Collections.unmodifiableList(new ArrayList<>(builder.durations));
        this.executedAt = Objects.requireNonNull(builder.executedAt, "executedAt must not be null");
    }

    /**
     * Repository 타입을 반환합니다.
     *
     * @return Repository 타입명
     */
    public String getRepositoryType() {
        return repositoryType;
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
     * 배치 크기를 반환합니다.
     *
     * @return 배치 크기
     */
    public int getBatchSize() {
        return batchSize;
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
     * 각 반복의 실행 시간 리스트를 반환합니다.
     *
     * @return 실행 시간 리스트 (ms)
     */
    public List<Long> getDurations() {
        return durations;
    }

    /**
     * 실행 시각을 반환합니다.
     *
     * @return 실행 시각
     */
    public Instant getExecutedAt() {
        return executedAt;
    }

    /**
     * 평균 실행 시간을 계산합니다.
     *
     * @return 평균 시간 (ms)
     */
    public double getAverageDuration() {
        if (durations.isEmpty()) {
            return 0.0;
        }
        return durations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 최소 실행 시간을 반환합니다.
     *
     * @return 최소 시간 (ms)
     */
    public long getMinDuration() {
        return durations.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
    }

    /**
     * 최대 실행 시간을 반환합니다.
     *
     * @return 최대 시간 (ms)
     */
    public long getMaxDuration() {
        return durations.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
    }

    /**
     * 표준 편차를 계산합니다.
     *
     * @return 표준 편차 (ms)
     */
    public double getStandardDeviation() {
        if (durations.size() < 2) {
            return 0.0;
        }

        double mean = getAverageDuration();
        double sumSquaredDiffs = durations.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .sum();

        return Math.sqrt(sumSquaredDiffs / (durations.size() - 1));
    }

    /**
     * 평균 TPS (Transactions Per Second)를 계산합니다.
     *
     * @return 평균 TPS
     */
    public double getAverageTps() {
        double avgDuration = getAverageDuration();
        if (avgDuration <= 0) {
            return 0.0;
        }
        return (recordCount * 1000.0) / avgDuration;
    }

    /**
     * 총 실행 시간을 반환합니다.
     *
     * @return 총 시간 (ms)
     */
    public long getTotalDuration() {
        return durations.stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    @Override
    public String toString() {
        return "BenchmarkResult{" +
                "repositoryType='" + repositoryType + '\'' +
                ", recordCount=" + recordCount +
                ", batchSize=" + batchSize +
                ", iterations=" + iterations +
                ", avgDuration=" + String.format("%.2f", getAverageDuration()) + "ms" +
                ", avgTps=" + String.format("%.2f", getAverageTps()) +
                '}';
    }

    /**
     * 새로운 Builder 인스턴스를 생성합니다.
     *
     * @return Builder 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * BenchmarkResult 빌더 클래스.
     */
    public static final class Builder {
        private String repositoryType;
        private int recordCount;
        private int batchSize;
        private int iterations;
        private List<Long> durations = new ArrayList<>();
        private Instant executedAt = Instant.now();

        private Builder() {
        }

        /**
         * Repository 타입을 설정합니다.
         *
         * @param repositoryType Repository 타입명
         * @return this builder
         */
        public Builder repositoryType(String repositoryType) {
            this.repositoryType = repositoryType;
            return this;
        }

        /**
         * 레코드 수를 설정합니다.
         *
         * @param recordCount 레코드 수
         * @return this builder
         */
        public Builder recordCount(int recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        /**
         * 배치 크기를 설정합니다.
         *
         * @param batchSize 배치 크기
         * @return this builder
         */
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * 반복 횟수를 설정합니다.
         *
         * @param iterations 반복 횟수
         * @return this builder
         */
        public Builder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        /**
         * 실행 시간 리스트를 설정합니다.
         *
         * @param durations 실행 시간 리스트
         * @return this builder
         */
        public Builder durations(List<Long> durations) {
            this.durations = new ArrayList<>(durations);
            return this;
        }

        /**
         * 실행 시간을 추가합니다.
         *
         * @param duration 실행 시간 (ms)
         * @return this builder
         */
        public Builder addDuration(long duration) {
            this.durations.add(duration);
            return this;
        }

        /**
         * 실행 시각을 설정합니다.
         *
         * @param executedAt 실행 시각
         * @return this builder
         */
        public Builder executedAt(Instant executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        /**
         * BenchmarkResult 인스턴스를 생성합니다.
         *
         * @return 새로운 BenchmarkResult 인스턴스
         */
        public BenchmarkResult build() {
            return new BenchmarkResult(this);
        }
    }
}
