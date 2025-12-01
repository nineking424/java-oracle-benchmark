package com.example.benchmark.util;

/**
 * 문자열 유틸리티 클래스.
 *
 * <p>Java 8 호환성을 위한 문자열 헬퍼 메서드를 제공합니다.</p>
 *
 * @author Developer Agent
 * @since 1.0
 */
public final class StringUtils {

    private StringUtils() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 문자열을 지정된 횟수만큼 반복합니다.
     *
     * <p>Java 11의 String.repeat() 대체 메서드입니다.</p>
     *
     * @param str 반복할 문자열
     * @param count 반복 횟수 (0 이상)
     * @return 반복된 문자열
     * @throws IllegalArgumentException count가 음수인 경우
     */
    public static String repeat(String str, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative: " + count);
        }
        if (count == 0 || str == null || str.isEmpty()) {
            return "";
        }
        if (count == 1) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
