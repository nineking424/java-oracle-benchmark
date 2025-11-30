package com.example.benchmark.exception;

/**
 * 벤치마크 실행 중 발생하는 예외.
 *
 * <p>데이터 접근 오류, 설정 오류 등 벤치마크 실행 중 발생하는
 * 모든 예외를 래핑합니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
public class BenchmarkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 메시지만 포함하는 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public BenchmarkException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인 예외를 포함하는 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public BenchmarkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인 예외만 포함하는 예외를 생성합니다.
     *
     * @param cause 원인 예외
     */
    public BenchmarkException(Throwable cause) {
        super(cause);
    }
}
