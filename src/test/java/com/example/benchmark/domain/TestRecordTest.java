package com.example.benchmark.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TestRecord 단위 테스트.
 *
 * @author Developer Agent
 * @since 1.0
 */
@DisplayName("TestRecord 테스트")
class TestRecordTest {

    private static final String DATA1 = "test-data1";
    private static final String DATA2 = "test-data2";
    private static final BigDecimal AMOUNT = new BigDecimal("12345.67");
    private static final String STATUS = "ACTIVE";
    private static final Instant CREATED_AT = Instant.parse("2024-01-01T00:00:00Z");

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("정상: 모든 필드로 객체 생성")
        void shouldBuildWithAllFields() {
            TestRecord record = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .data2(DATA2)
                    .amount(AMOUNT)
                    .status(STATUS)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record.getId()).isEqualTo(1L);
            assertThat(record.getData1()).isEqualTo(DATA1);
            assertThat(record.getData2()).isEqualTo(DATA2);
            assertThat(record.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(record.getStatus()).isEqualTo(STATUS);
            assertThat(record.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        @DisplayName("정상: 필수 필드만으로 객체 생성")
        void shouldBuildWithRequiredFieldsOnly() {
            TestRecord record = TestRecord.builder()
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record.getId()).isNull();
            assertThat(record.getData1()).isEqualTo(DATA1);
            assertThat(record.getData2()).isNull();
            assertThat(record.getAmount()).isNull();
            assertThat(record.getStatus()).isEqualTo("ACTIVE"); // 기본값
            assertThat(record.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        @DisplayName("예외: data1이 null인 경우 NullPointerException")
        void shouldThrowExceptionWhenData1IsNull() {
            assertThatThrownBy(() -> TestRecord.builder()
                    .data1(null)
                    .createdAt(CREATED_AT)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("data1");
        }

        @Test
        @DisplayName("예외: createdAt이 null인 경우 NullPointerException")
        void shouldThrowExceptionWhenCreatedAtIsNull() {
            assertThatThrownBy(() -> TestRecord.builder()
                    .data1(DATA1)
                    .createdAt(null)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("createdAt");
        }

        @Test
        @DisplayName("정상: status가 null이면 기본값 ACTIVE 적용")
        void shouldApplyDefaultStatusWhenNull() {
            TestRecord record = TestRecord.builder()
                    .data1(DATA1)
                    .status(null)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record.getStatus()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("withId 메서드 테스트")
    class WithIdTest {

        @Test
        @DisplayName("정상: 새로운 ID로 복사본 생성")
        void shouldCreateCopyWithNewId() {
            TestRecord original = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .data2(DATA2)
                    .amount(AMOUNT)
                    .status(STATUS)
                    .createdAt(CREATED_AT)
                    .build();

            TestRecord copied = original.withId(2L);

            assertThat(copied.getId()).isEqualTo(2L);
            assertThat(copied.getData1()).isEqualTo(original.getData1());
            assertThat(copied.getData2()).isEqualTo(original.getData2());
            assertThat(copied.getAmount()).isEqualTo(original.getAmount());
            assertThat(copied.getStatus()).isEqualTo(original.getStatus());
            assertThat(copied.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(copied).isNotSameAs(original);
        }

        @Test
        @DisplayName("정상: null ID로 복사본 생성")
        void shouldCreateCopyWithNullId() {
            TestRecord original = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            TestRecord copied = original.withId(null);

            assertThat(copied.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("equals 및 hashCode 테스트")
    class EqualsAndHashCodeTest {

        @Test
        @DisplayName("정상: 동일한 값을 가진 객체는 같음")
        void shouldBeEqualWhenSameValues() {
            TestRecord record1 = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .data2(DATA2)
                    .amount(AMOUNT)
                    .status(STATUS)
                    .createdAt(CREATED_AT)
                    .build();

            TestRecord record2 = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .data2(DATA2)
                    .amount(AMOUNT)
                    .status(STATUS)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record1).isEqualTo(record2);
            assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
        }

        @Test
        @DisplayName("정상: 다른 값을 가진 객체는 다름")
        void shouldNotBeEqualWhenDifferentValues() {
            TestRecord record1 = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            TestRecord record2 = TestRecord.builder()
                    .id(2L)
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("정상: 자기 자신과 같음")
        void shouldBeEqualToItself() {
            TestRecord record = TestRecord.builder()
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record).isEqualTo(record);
        }

        @Test
        @DisplayName("정상: null과 같지 않음")
        void shouldNotBeEqualToNull() {
            TestRecord record = TestRecord.builder()
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record).isNotEqualTo(null);
        }

        @Test
        @DisplayName("정상: 다른 타입과 같지 않음")
        void shouldNotBeEqualToDifferentType() {
            TestRecord record = TestRecord.builder()
                    .data1(DATA1)
                    .createdAt(CREATED_AT)
                    .build();

            assertThat(record).isNotEqualTo("string");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("정상: 모든 필드 포함")
        void shouldContainAllFields() {
            TestRecord record = TestRecord.builder()
                    .id(1L)
                    .data1(DATA1)
                    .data2(DATA2)
                    .amount(AMOUNT)
                    .status(STATUS)
                    .createdAt(CREATED_AT)
                    .build();

            String result = record.toString();

            assertThat(result).contains("TestRecord");
            assertThat(result).contains("id=1");
            assertThat(result).contains("data1='" + DATA1 + "'");
            assertThat(result).contains("data2='" + DATA2 + "'");
            assertThat(result).contains("amount=" + AMOUNT);
            assertThat(result).contains("status='" + STATUS + "'");
            assertThat(result).contains("createdAt=" + CREATED_AT);
        }
    }
}
