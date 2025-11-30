package com.example.benchmark.util;

import com.example.benchmark.domain.TestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 벤치마크용 테스트 데이터 생성기.
 *
 * <p>지정된 개수만큼 랜덤 TestRecord 인스턴스를 생성합니다.
 * 재현 가능한 테스트를 위해 seed 설정이 가능합니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
public final class TestDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestDataGenerator.class);

    private static final String[] STATUSES = {"ACTIVE", "INACTIVE", "PENDING", "COMPLETED"};
    private static final int DATA1_LENGTH = 50;
    private static final int DATA2_LENGTH = 100;
    private static final double MAX_AMOUNT = 1000000.0;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final Random random;

    /**
     * 기본 생성자.
     *
     * <p>현재 시간을 seed로 사용합니다.</p>
     */
    public TestDataGenerator() {
        this.random = new Random();
    }

    /**
     * seed를 지정하는 생성자.
     *
     * @param seed 랜덤 생성기 seed
     */
    public TestDataGenerator(long seed) {
        this.random = new Random(seed);
        log.debug("TestDataGenerator initialized with seed: {}", seed);
    }

    /**
     * 지정된 개수만큼 TestRecord 리스트를 생성합니다.
     *
     * @param count 생성할 레코드 수 (0 이상)
     * @return TestRecord 리스트
     * @throws IllegalArgumentException count가 음수인 경우
     */
    public List<TestRecord> generate(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative, but was: " + count);
        }

        log.debug("Generating {} test records", count);
        long startTime = System.currentTimeMillis();

        List<TestRecord> records = new ArrayList<>(count);
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            records.add(generateRecord(now));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Generated {} records in {}ms", count, duration);

        return records;
    }

    private TestRecord generateRecord(Instant baseTime) {
        return TestRecord.builder()
                .data1(generateRandomString(DATA1_LENGTH))
                .data2(random.nextBoolean() ? generateRandomString(DATA2_LENGTH) : null)
                .amount(generateRandomAmount())
                .status(STATUSES[random.nextInt(STATUSES.length)])
                .createdAt(baseTime)
                .build();
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private BigDecimal generateRandomAmount() {
        double value = random.nextDouble() * MAX_AMOUNT;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 고정된 seed로 재현 가능한 레코드 리스트를 생성합니다.
     *
     * <p>같은 count와 seed로 호출하면 항상 동일한 결과를 반환합니다.</p>
     *
     * @param count 생성할 레코드 수
     * @param seed 랜덤 생성기 seed
     * @return TestRecord 리스트
     */
    public static List<TestRecord> generateWithSeed(int count, long seed) {
        return new TestDataGenerator(seed).generate(count);
    }

    /**
     * 기본 설정으로 레코드 리스트를 생성합니다.
     *
     * @param count 생성할 레코드 수
     * @return TestRecord 리스트
     */
    public static List<TestRecord> generateDefault(int count) {
        return new TestDataGenerator().generate(count);
    }
}
