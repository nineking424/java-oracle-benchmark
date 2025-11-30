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
 * JDBC를 사용한 단건 삽입 구현체.
 *
 * <p>각 레코드를 개별 INSERT 문으로 처리합니다.
 * 배치 삽입과의 성능 비교를 위해 사용됩니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Repository
public class JdbcSingleInsertRepository implements SingleInsertRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcSingleInsertRepository.class);

    private static final String TYPE_NAME = "JDBC-Single";

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
    private boolean isH2Database;

    /**
     * JdbcSingleInsertRepository 생성자.
     *
     * @param dataSource 데이터소스 (null 불가)
     */
    public JdbcSingleInsertRepository(DataSource dataSource) {
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
    public int insertSingle(List<TestRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }

        if (records.isEmpty()) {
            log.debug("Empty record list, nothing to insert");
            return 0;
        }

        log.info("Starting single insert: records={}", records.size());
        long startTime = System.currentTimeMillis();

        String sql = isH2Database ? INSERT_SQL_H2 : INSERT_SQL;
        int totalInserted = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (int i = 0; i < records.size(); i++) {
                TestRecord record = records.get(i);
                setParameters(ps, record);
                int result = ps.executeUpdate();
                totalInserted += result;

                if ((i + 1) % 1000 == 0) {
                    log.debug("Single insert progress: processed={}, totalInserted={}", i + 1, totalInserted);
                }
            }

            conn.commit();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Single insert completed: totalInserted={}, duration={}ms, tps={}",
                    totalInserted, duration, calculateTps(totalInserted, duration));

            return totalInserted;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert single: " + records.size() + " records", e) {};
        }
    }

    private void setParameters(PreparedStatement ps, TestRecord record) throws SQLException {
        ps.setString(1, record.getData1());
        ps.setString(2, record.getData2());
        ps.setBigDecimal(3, record.getAmount());
        ps.setString(4, record.getStatus());
        ps.setTimestamp(5, Timestamp.from(record.getCreatedAt()));
    }

    private double calculateTps(int count, long durationMs) {
        if (durationMs == 0) {
            return 0;
        }
        return (count * 1000.0) / durationMs;
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
