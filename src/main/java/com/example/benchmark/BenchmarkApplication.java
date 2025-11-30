package com.example.benchmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Oracle Insert 성능 비교 벤치마크 애플리케이션.
 *
 * <p>JDBC batch insert와 MyBatis batch insert의 성능을 비교하여
 * 대용량 데이터 처리 시 최적의 방법을 선정합니다.</p>
 *
 * <p>측정 항목:</p>
 * <ul>
 *   <li>JDBC Batch Insert 성능</li>
 *   <li>JDBC Single Insert 성능</li>
 *   <li>MyBatis Batch Insert 성능</li>
 *   <li>MyBatis Single Insert 성능</li>
 * </ul>
 *
 * @author Developer Agent
 * @since 1.0
 */
@SpringBootApplication
public class BenchmarkApplication {

    /**
     * 애플리케이션 진입점.
     *
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }
}
