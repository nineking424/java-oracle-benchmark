package com.example.benchmark.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 벤치마크 테스트용 레코드 엔티티.
 *
 * <p>불변 객체로 설계되어 thread-safe합니다.
 * Builder 패턴을 통해 인스턴스를 생성합니다.</p>
 *
 * <p>테이블 매핑:</p>
 * <pre>
 * TEST_RECORD (
 *   ID NUMBER(19) PRIMARY KEY,
 *   DATA1 VARCHAR2(100) NOT NULL,
 *   DATA2 VARCHAR2(200),
 *   AMOUNT NUMBER(18,2),
 *   STATUS VARCHAR2(20) DEFAULT 'ACTIVE',
 *   CREATED_AT TIMESTAMP NOT NULL
 * )
 * </pre>
 *
 * @author Developer Agent
 * @since 1.0
 */
public final class TestRecord {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final Long id;
    private final String data1;
    private final String data2;
    private final BigDecimal amount;
    private final String status;
    private final Instant createdAt;

    private TestRecord(Builder builder) {
        this.id = builder.id;
        this.data1 = Objects.requireNonNull(builder.data1, "data1 must not be null");
        this.data2 = builder.data2;
        this.amount = builder.amount;
        this.status = builder.status != null ? builder.status : DEFAULT_STATUS;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt must not be null");
    }

    /**
     * 레코드 ID를 반환합니다.
     *
     * @return 레코드 ID (새 레코드인 경우 null)
     */
    public Long getId() {
        return id;
    }

    /**
     * 데이터1 필드를 반환합니다.
     *
     * @return data1 값 (not null)
     */
    public String getData1() {
        return data1;
    }

    /**
     * 데이터2 필드를 반환합니다.
     *
     * @return data2 값 (nullable)
     */
    public String getData2() {
        return data2;
    }

    /**
     * 금액 필드를 반환합니다.
     *
     * @return 금액 (nullable)
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 상태 필드를 반환합니다.
     *
     * @return 상태 (기본값: ACTIVE)
     */
    public String getStatus() {
        return status;
    }

    /**
     * 생성 시각을 반환합니다.
     *
     * @return 생성 시각 (not null)
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * ID가 설정된 새로운 TestRecord 인스턴스를 반환합니다.
     *
     * @param newId 새로운 ID
     * @return ID가 설정된 새로운 인스턴스
     */
    public TestRecord withId(Long newId) {
        return new Builder()
                .id(newId)
                .data1(this.data1)
                .data2(this.data2)
                .amount(this.amount)
                .status(this.status)
                .createdAt(this.createdAt)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestRecord that = (TestRecord) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(data1, that.data1) &&
                Objects.equals(data2, that.data2) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data1, data2, amount, status, createdAt);
    }

    @Override
    public String toString() {
        return "TestRecord{" +
                "id=" + id +
                ", data1='" + data1 + '\'' +
                ", data2='" + data2 + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
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
     * TestRecord 빌더 클래스.
     */
    public static final class Builder {
        private Long id;
        private String data1;
        private String data2;
        private BigDecimal amount;
        private String status;
        private Instant createdAt;

        private Builder() {
        }

        /**
         * ID를 설정합니다.
         *
         * @param id 레코드 ID
         * @return this builder
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * data1 필드를 설정합니다.
         *
         * @param data1 데이터1 (필수)
         * @return this builder
         */
        public Builder data1(String data1) {
            this.data1 = data1;
            return this;
        }

        /**
         * data2 필드를 설정합니다.
         *
         * @param data2 데이터2 (선택)
         * @return this builder
         */
        public Builder data2(String data2) {
            this.data2 = data2;
            return this;
        }

        /**
         * 금액 필드를 설정합니다.
         *
         * @param amount 금액 (선택)
         * @return this builder
         */
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * 상태 필드를 설정합니다.
         *
         * @param status 상태 (기본값: ACTIVE)
         * @return this builder
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * 생성 시각을 설정합니다.
         *
         * @param createdAt 생성 시각 (필수)
         * @return this builder
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * TestRecord 인스턴스를 생성합니다.
         *
         * @return 새로운 TestRecord 인스턴스
         * @throws NullPointerException data1 또는 createdAt이 null인 경우
         */
        public TestRecord build() {
            return new TestRecord(this);
        }
    }
}
