package com.example.benchmark.repository;

import com.example.benchmark.domain.TestRecord;

import java.util.List;

/**
 * 배치 삽입 Repository 인터페이스.
 *
 * <p>대량의 레코드를 배치로 삽입하는 기능을 정의합니다.
 * JDBC와 MyBatis 구현체가 이 인터페이스를 구현합니다.</p>
 *
 * <p>구현체:</p>
 * <ul>
 *   <li>{@link JdbcBatchInsertRepository} - JDBC 기반 배치 삽입</li>
 *   <li>{@link MyBatisBatchInsertRepository} - MyBatis 기반 배치 삽입</li>
 * </ul>
 *
 * @author Developer Agent
 * @since 1.0
 */
public interface BatchInsertRepository {

    /**
     * 레코드 목록을 배치로 삽입합니다.
     *
     * <p>설정된 배치 크기에 따라 레코드를 분할하여 삽입합니다.
     * 트랜잭션은 전체 작업에 대해 적용됩니다.</p>
     *
     * @param records 삽입할 레코드 목록 (null 불가)
     * @return 삽입된 레코드 수
     * @throws IllegalArgumentException records가 null인 경우
     * @throws org.springframework.dao.DataAccessException 데이터 접근 오류 시
     */
    int insertBatch(List<TestRecord> records);

    /**
     * 배치 크기를 설정합니다.
     *
     * <p>배치 크기는 한 번의 executeBatch 또는 flushStatements 호출 시
     * 처리되는 레코드 수를 결정합니다.</p>
     *
     * @param batchSize 배치 크기 (1 이상)
     * @throws IllegalArgumentException batchSize가 1 미만인 경우
     */
    void setBatchSize(int batchSize);

    /**
     * 현재 설정된 배치 크기를 반환합니다.
     *
     * @return 배치 크기
     */
    int getBatchSize();

    /**
     * 테이블의 모든 데이터를 삭제합니다.
     *
     * <p>벤치마크 실행 전 테이블 초기화에 사용됩니다.
     * 이 작업은 롤백되지 않습니다.</p>
     */
    void truncateTable();

    /**
     * 테이블의 레코드 수를 반환합니다.
     *
     * @return 레코드 수
     */
    long count();

    /**
     * Repository 타입명을 반환합니다.
     *
     * @return 구현체 타입명 (예: "JDBC", "MyBatis")
     */
    String getTypeName();
}
