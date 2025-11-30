package com.example.benchmark.mapper;

import com.example.benchmark.domain.TestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * TestRecord 엔티티를 위한 MyBatis Mapper 인터페이스.
 *
 * <p>SQL 매핑은 mapper/TestRecordMapper.xml에 정의되어 있습니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
@Mapper
public interface TestRecordMapper {

    /**
     * 단일 레코드를 삽입합니다.
     *
     * @param record 삽입할 레코드
     * @return 영향받은 행 수
     */
    int insert(@Param("record") TestRecord record);

    /**
     * 테이블의 모든 데이터를 삭제합니다.
     */
    void truncateTable();

    /**
     * 테이블의 레코드 수를 조회합니다.
     *
     * @return 레코드 수
     */
    long count();
}
