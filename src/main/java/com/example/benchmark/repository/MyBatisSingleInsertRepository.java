package com.example.benchmark.repository;

import com.example.benchmark.domain.TestRecord;
import com.example.benchmark.mapper.TestRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * MyBatis를 사용한 단건 삽입 구현체.
 *
 * <p>각 레코드를 개별 INSERT 문으로 처리합니다.
 * 배치 삽입과의 성능 비교를 위해 사용됩니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Repository
public class MyBatisSingleInsertRepository implements SingleInsertRepository {

    private static final Logger log = LoggerFactory.getLogger(MyBatisSingleInsertRepository.class);

    private static final String TYPE_NAME = "MyBatis-Single";

    private final TestRecordMapper mapper;

    /**
     * MyBatisSingleInsertRepository 생성자.
     *
     * @param mapper TestRecordMapper (null 불가)
     */
    public MyBatisSingleInsertRepository(TestRecordMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public int insertSingle(List<TestRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }

        if (records.isEmpty()) {
            log.debug("Empty record list, nothing to insert");
            return 0;
        }

        log.info("Starting MyBatis single insert: records={}", records.size());
        long startTime = System.currentTimeMillis();

        int totalInserted = 0;

        for (int i = 0; i < records.size(); i++) {
            TestRecord record = records.get(i);
            int result = mapper.insert(record);
            totalInserted += result;

            if ((i + 1) % 1000 == 0) {
                log.debug("Single insert progress: processed={}, totalInserted={}", i + 1, totalInserted);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("MyBatis single insert completed: totalInserted={}, duration={}ms, tps={}",
                totalInserted, duration, calculateTps(totalInserted, duration));

        return totalInserted;
    }

    private double calculateTps(int count, long durationMs) {
        if (durationMs == 0) {
            return 0;
        }
        return (count * 1000.0) / durationMs;
    }

    @Override
    public void truncateTable() {
        log.info("Truncating test_record table via MyBatis");
        mapper.truncateTable();
    }

    @Override
    public long count() {
        return mapper.count();
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }
}
