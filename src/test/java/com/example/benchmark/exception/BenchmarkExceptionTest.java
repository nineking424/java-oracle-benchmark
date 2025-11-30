package com.example.benchmark.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BenchmarkException 단위 테스트.
 *
 * @author Developer Agent
 * @since 1.0
 */
@DisplayName("BenchmarkException 테스트")
class BenchmarkExceptionTest {

    private static final String TEST_MESSAGE = "Test exception message";

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상: 메시지만 포함한 예외 생성")
        void shouldCreateWithMessageOnly() {
            BenchmarkException exception = new BenchmarkException(TEST_MESSAGE);

            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("정상: 메시지와 원인 예외 포함한 예외 생성")
        void shouldCreateWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            BenchmarkException exception = new BenchmarkException(TEST_MESSAGE, cause);

            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
            assertThat(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("정상: 원인 예외만 포함한 예외 생성")
        void shouldCreateWithCauseOnly() {
            RuntimeException cause = new RuntimeException("Root cause");
            BenchmarkException exception = new BenchmarkException(cause);

            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getMessage()).contains("Root cause");
        }
    }

    @Nested
    @DisplayName("상속 구조 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("정상: RuntimeException 상속 확인")
        void shouldExtendRuntimeException() {
            BenchmarkException exception = new BenchmarkException(TEST_MESSAGE);

            assertThat(exception).isInstanceOf(RuntimeException.class);
            assertThat(exception).isInstanceOf(Exception.class);
            assertThat(exception).isInstanceOf(Throwable.class);
        }
    }
}
