package com.example.benchmark.repository;

import com.example.benchmark.domain.TestRecord;
import com.example.benchmark.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MyBatisBatchInsertRepository 테스트.
 *
 * @author Developer Agent
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MyBatisBatchInsertRepository 테스트")
class MyBatisBatchInsertRepositoryTest {

    @Autowired
    private MyBatisBatchInsertRepository repository;

    private TestDataGenerator dataGenerator;

    @BeforeEach
    void setUp() {
        repository.truncateTable();
        dataGenerator = new TestDataGenerator(12345L);
    }

    @Nested
    @DisplayName("insertBatch 메서드")
    class InsertBatchTest {

        @Test
        @DisplayName("정상: 100건 삽입 시 100 반환")
        void shouldReturnInsertedCount() {
            // given
            List<TestRecord> records = dataGenerator.generate(100);

            // when
            int result = repository.insertBatch(records);

            // then
            assertThat(result).isEqualTo(100);
            assertThat(repository.count()).isEqualTo(100);
        }

        @Test
        @DisplayName("정상: 배치 크기보다 큰 데이터 삽입")
        void shouldHandleRecordsLargerThanBatchSize() {
            // given
            repository.setBatchSize(50);
            List<TestRecord> records = dataGenerator.generate(120);

            // when
            int result = repository.insertBatch(records);

            // then
            assertThat(result).isEqualTo(120);
            assertThat(repository.count()).isEqualTo(120);
        }

        @Test
        @DisplayName("정상: 배치 크기와 동일한 데이터 삽입")
        void shouldHandleRecordsEqualToBatchSize() {
            // given
            repository.setBatchSize(100);
            List<TestRecord> records = dataGenerator.generate(100);

            // when
            int result = repository.insertBatch(records);

            // then
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("정상: 단일 레코드 삽입")
        void shouldInsertSingleRecord() {
            // given
            List<TestRecord> records = dataGenerator.generate(1);

            // when
            int result = repository.insertBatch(records);

            // then
            assertThat(result).isEqualTo(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("경계: 빈 리스트 입력 시 0 반환")
        void shouldReturnZeroForEmptyList() {
            // given
            List<TestRecord> records = Collections.emptyList();

            // when
            int result = repository.insertBatch(records);

            // then
            assertThat(result).isZero();
            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("예외: null 입력 시 IllegalArgumentException")
        void shouldThrowExceptionForNull() {
            assertThatThrownBy(() -> repository.insertBatch(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("setBatchSize 메서드")
    class SetBatchSizeTest {

        @Test
        @DisplayName("정상: 배치 크기 설정")
        void shouldSetBatchSize() {
            // when
            repository.setBatchSize(500);

            // then
            assertThat(repository.getBatchSize()).isEqualTo(500);
        }

        @Test
        @DisplayName("예외: 0 이하의 배치 크기")
        void shouldThrowExceptionForInvalidBatchSize() {
            assertThatThrownBy(() -> repository.setBatchSize(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("batchSize");

            assertThatThrownBy(() -> repository.setBatchSize(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("truncateTable 메서드")
    class TruncateTableTest {

        @Test
        @DisplayName("정상: 테이블 비우기")
        void shouldTruncateTable() {
            // given
            List<TestRecord> records = dataGenerator.generate(50);
            repository.insertBatch(records);
            assertThat(repository.count()).isEqualTo(50);

            // when
            repository.truncateTable();

            // then
            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("getTypeName 메서드")
    class GetTypeNameTest {

        @Test
        @DisplayName("정상: 타입명 반환")
        void shouldReturnTypeName() {
            assertThat(repository.getTypeName()).isEqualTo("MyBatis-Batch");
        }
    }
}
