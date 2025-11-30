package com.example.benchmark.repository;

import com.example.benchmark.domain.TestRecord;

import java.util.List;

/**
 * 단건 삽입 Repository 인터페이스.
 *
 * <p>레코드를 개별적으로 삽입하는 기능을 정의합니다.
 * 배치 삽입과의 성능 비교를 위해 사용됩니다.</p>
 *
 * <p>구현체:</p>
 * <ul>
 *   <li>{@link JdbcSingleInsertRepository} - JDBC 기반 단건 삽입</li>
 *   <li>{@link MyBatisSingleInsertRepository} - MyBatis 기반 단건 삽입</li>
 * </ul>
 *
 * @author Developer Agent
 * @since 1.0
 */
public interface SingleInsertRepository {

    /**
     * 레코드 목록을 개별적으로 삽입합니다.
     *
     * <p>각 레코드를 개별 INSERT 문으로 처리합니다.
     * 배치 처리를 사용하지 않습니다.</p>
     *
     * @param records 삽입할 레코드 목록 (null 불가)
     * @return 삽입된 레코드 수
     * @throws IllegalArgumentException records가 null인 경우
     * @throws org.springframework.dao.DataAccessException 데이터 접근 오류 시
     */
    int insertSingle(List<TestRecord> records);

    /**
     * 테이블의 모든 데이터를 삭제합니다.
     *
     * <p>벤치마크 실행 전 테이블 초기화에 사용됩니다.</p>
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
     * @return 구현체 타입명 (예: "JDBC-Single", "MyBatis-Single")
     */
    String getTypeName();
}
