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
 * MyBatisSingleInsertRepository 테스트.
 *
 * @author Developer Agent
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MyBatisSingleInsertRepository 테스트")
class MyBatisSingleInsertRepositoryTest {

    @Autowired
    private MyBatisSingleInsertRepository repository;

    private TestDataGenerator dataGenerator;

    @BeforeEach
    void setUp() {
        repository.truncateTable();
        dataGenerator = new TestDataGenerator(12345L);
    }

    @Nested
    @DisplayName("insertSingle 메서드")
    class InsertSingleTest {

        @Test
        @DisplayName("정상: 50건 삽입 시 50 반환")
        void shouldReturnInsertedCount() {
            // given
            List<TestRecord> records = dataGenerator.generate(50);

            // when
            int result = repository.insertSingle(records);

            // then
            assertThat(result).isEqualTo(50);
            assertThat(repository.count()).isEqualTo(50);
        }

        @Test
        @DisplayName("정상: 단일 레코드 삽입")
        void shouldInsertSingleRecord() {
            // given
            List<TestRecord> records = dataGenerator.generate(1);

            // when
            int result = repository.insertSingle(records);

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
            int result = repository.insertSingle(records);

            // then
            assertThat(result).isZero();
            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("예외: null 입력 시 IllegalArgumentException")
        void shouldThrowExceptionForNull() {
            assertThatThrownBy(() -> repository.insertSingle(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("truncateTable 메서드")
    class TruncateTableTest {

        @Test
        @DisplayName("정상: 테이블 비우기")
        void shouldTruncateTable() {
            // given
            List<TestRecord> records = dataGenerator.generate(30);
            repository.insertSingle(records);
            assertThat(repository.count()).isEqualTo(30);

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
            assertThat(repository.getTypeName()).isEqualTo("MyBatis-Single");
        }
    }
}
