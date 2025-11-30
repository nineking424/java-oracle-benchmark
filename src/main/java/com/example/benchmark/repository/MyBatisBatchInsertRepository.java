package com.example.benchmark.repository;

import com.example.benchmark.domain.TestRecord;
import com.example.benchmark.mapper.TestRecordMapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * MyBatis를 사용한 배치 삽입 구현체.
 *
 * <p>ExecutorType.BATCH 모드의 SqlSession을 사용하여
 * 대량 데이터를 효율적으로 삽입합니다.</p>
 *
 * <p>특징:</p>
 * <ul>
 *   <li>배치 크기 설정 가능 (기본값: 1000)</li>
 *   <li>flushStatements를 통한 배치 실행</li>
 *   <li>트랜잭션 지원</li>
 * </ul>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Repository
public class MyBatisBatchInsertRepository implements BatchInsertRepository {

    private static final Logger log = LoggerFactory.getLogger(MyBatisBatchInsertRepository.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String TYPE_NAME = "MyBatis-Batch";

    private final SqlSessionFactory sqlSessionFactory;
    private final TestRecordMapper mapper;
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * MyBatisBatchInsertRepository 생성자.
     *
     * @param sqlSessionFactory SqlSessionFactory (null 불가)
     * @param mapper TestRecordMapper (null 불가)
     */
    public MyBatisBatchInsertRepository(SqlSessionFactory sqlSessionFactory, TestRecordMapper mapper) {
        this.sqlSessionFactory = Objects.requireNonNull(sqlSessionFactory, "sqlSessionFactory must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public int insertBatch(List<TestRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }

        if (records.isEmpty()) {
            log.debug("Empty record list, nothing to insert");
            return 0;
        }

        log.info("Starting MyBatis batch insert: records={}, batchSize={}", records.size(), batchSize);
        long startTime = System.currentTimeMillis();

        int totalInserted = 0;

        try (SqlSession batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            TestRecordMapper batchMapper = batchSession.getMapper(TestRecordMapper.class);

            for (int i = 0; i < records.size(); i++) {
                TestRecord record = records.get(i);
                batchMapper.insert(record);
                totalInserted++;

                if ((i + 1) % batchSize == 0) {
                    batchSession.flushStatements();
                    log.debug("Flushed batch: processed={}, totalInserted={}", i + 1, totalInserted);
                }
            }

            // 남은 레코드 처리
            if (records.size() % batchSize != 0) {
                batchSession.flushStatements();
            }

            batchSession.commit();

            long duration = System.currentTimeMillis() - startTime;
            log.info("MyBatis batch insert completed: totalInserted={}, duration={}ms, tps={}",
                    totalInserted, duration, calculateTps(totalInserted, duration));

            return totalInserted;
        }
    }

    private double calculateTps(int count, long durationMs) {
        if (durationMs == 0) {
            return 0;
        }
        return (count * 1000.0) / durationMs;
    }

    @Override
    public void setBatchSize(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1, but was: " + batchSize);
        }
        this.batchSize = batchSize;
        log.debug("Batch size set to: {}", batchSize);
    }

    @Override
    public int getBatchSize() {
        return batchSize;
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
