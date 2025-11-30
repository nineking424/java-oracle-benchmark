package com.example.benchmark.repository;

import com.example.benchmark.domain.TestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * JDBC를 사용한 배치 삽입 구현체.
 *
 * <p>PreparedStatement의 addBatch/executeBatch를 활용하여
 * 대량 데이터를 효율적으로 삽입합니다.</p>
 *
 * <p>특징:</p>
 * <ul>
 *   <li>배치 크기 설정 가능 (기본값: 1000)</li>
 *   <li>트랜잭션 지원</li>
 *   <li>try-with-resources를 통한 리소스 관리</li>
 * </ul>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Repository
public class JdbcBatchInsertRepository implements BatchInsertRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcBatchInsertRepository.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String TYPE_NAME = "JDBC-Batch";

    private static final String INSERT_SQL =
            "INSERT INTO test_record (id, data1, data2, amount, status, created_at) " +
                    "VALUES (test_record_seq.NEXTVAL, ?, ?, ?, ?, ?)";

    private static final String INSERT_SQL_H2 =
            "INSERT INTO test_record (id, data1, data2, amount, status, created_at) " +
                    "VALUES (NEXTVAL('test_record_seq'), ?, ?, ?, ?, ?)";

    private static final String TRUNCATE_SQL = "DELETE FROM test_record";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM test_record";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private boolean isH2Database;

    /**
     * JdbcBatchInsertRepository 생성자.
     *
     * @param dataSource 데이터소스 (null 불가)
     */
    public JdbcBatchInsertRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        detectDatabaseType();
    }

    private void detectDatabaseType() {
        try (Connection conn = dataSource.getConnection()) {
            String driverName = conn.getMetaData().getDriverName().toLowerCase();
            this.isH2Database = driverName.contains("h2");
            log.debug("Detected database driver: {}, isH2: {}", driverName, isH2Database);
        } catch (SQLException e) {
            log.warn("Failed to detect database type, defaulting to Oracle", e);
            this.isH2Database = false;
        }
    }

    @Override
    @Transactional
    public int insertBatch(List<TestRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }

        if (records.isEmpty()) {
            log.debug("Empty record list, nothing to insert");
            return 0;
        }

        log.info("Starting batch insert: records={}, batchSize={}", records.size(), batchSize);
        long startTime = System.currentTimeMillis();

        String sql = isH2Database ? INSERT_SQL_H2 : INSERT_SQL;
        int totalInserted = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (int i = 0; i < records.size(); i++) {
                TestRecord record = records.get(i);
                setParameters(ps, record);
                ps.addBatch();

                if ((i + 1) % batchSize == 0) {
                    int[] results = ps.executeBatch();
                    totalInserted += sumResults(results);
                    log.debug("Executed batch: processed={}, totalInserted={}", i + 1, totalInserted);
                }
            }

            // 남은 레코드 처리
            if (records.size() % batchSize != 0) {
                int[] results = ps.executeBatch();
                totalInserted += sumResults(results);
            }

            conn.commit();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Batch insert completed: totalInserted={}, duration={}ms, tps={}",
                    totalInserted, duration, calculateTps(totalInserted, duration));

            return totalInserted;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert batch: " + records.size() + " records", e) {};
        }
    }

    private void setParameters(PreparedStatement ps, TestRecord record) throws SQLException {
        ps.setString(1, record.getData1());
        ps.setString(2, record.getData2());
        ps.setBigDecimal(3, record.getAmount());
        ps.setString(4, record.getStatus());
        ps.setTimestamp(5, Timestamp.from(record.getCreatedAt()));
    }

    private int sumResults(int[] results) {
        int sum = 0;
        for (int result : results) {
            if (result >= 0) {
                sum += result;
            } else if (result == PreparedStatement.SUCCESS_NO_INFO) {
                sum += 1;
            }
        }
        return sum;
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
        log.info("Truncating test_record table");
        jdbcTemplate.update(TRUNCATE_SQL);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject(COUNT_SQL, Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }
}
