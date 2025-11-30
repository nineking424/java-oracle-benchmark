# CLAUDE.md - AI 개발 지침

## 프로젝트 개요
Oracle Insert 성능 비교 벤치마크 (JDBC vs MyBatis)
- Java 1.8, Spring Boot 2.7, Maven
- Oracle 19c 대상

---

## 핵심 원칙

### 1. 단계별 검증 필수 (Incremental Verified Development)
모든 코드 변경 후 반드시 검증하고, 통과 후에만 다음 단계로 진행한다.

```
코드 작성 → 컴파일 검증 → 테스트 검증 → 다음 단계
     ↑                              │
     └──────── 실패 시 수정 ─────────┘
```

### 2. 다층 검증 체계

#### Level 1: 정적 검증 (매 변경 시)
```bash
mvn compile test-compile
```
- 컴파일 에러 0개 필수
- 통과 후에만 다음 진행

#### Level 2: 단위 테스트 (기능 완성 시)
```bash
mvn test
```
- 새 코드에 대한 테스트 필수 포함
- 기존 테스트 깨지지 않아야 함

#### Level 3: 커버리지 검증 (Phase 완료 시)
```bash
mvn jacoco:report
```
- 신규 클래스 라인 커버리지 80% 이상
- 미달 시 테스트 보강
- 결과: target/site/jacoco/index.html

#### Level 4: 실제 실행 검증 (통합 단계)
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--benchmark.record-count=100"
```
- 실제 DB 연동 동작 확인

---

## 코딩 컨벤션

### Java
- 모든 클래스에 package 선언
- DTO/Entity는 불변 객체 권장 (final 필드, 생성자 초기화, getter만 제공)
- Java 8 사용으로 record 클래스 미지원 - 전통적 불변 클래스 사용
- Lombok 사용 시 @Data 금지 (불변성 위반)
- 모든 public 메서드에 JavaDoc

### 네이밍
- 클래스: PascalCase (예: JdbcBatchInsertRepository)
- 메서드/변수: camelCase (예: insertBatch)
- 상수: UPPER_SNAKE_CASE (예: DEFAULT_BATCH_SIZE)
- 패키지: lowercase (예: com.example.benchmark)

### 패키지 구조
```
com.example.benchmark/
├── BenchmarkApplication.java      # 메인 클래스
├── config/                        # 설정 클래스
│   └── DataSourceConfig.java
├── domain/                        # 엔티티
│   └── TestRecord.java
├── repository/                    # 데이터 접근
│   ├── BatchInsertRepository.java # 인터페이스
│   ├── JdbcBatchInsertRepository.java
│   └── MyBatisBatchInsertRepository.java
├── mapper/                        # MyBatis Mapper
│   └── TestRecordMapper.java
├── benchmark/                     # 벤치마크 로직
│   ├── BenchmarkRunner.java
│   ├── BenchmarkResult.java
│   └── BenchmarkReportGenerator.java
└── util/                          # 유틸리티
    └── TestDataGenerator.java
```

### 예외 처리
- Checked Exception은 RuntimeException으로 래핑
- 예외 메시지에 컨텍스트 포함
- 리소스는 try-with-resources 필수

```java
// Good
try (Connection conn = dataSource.getConnection()) {
    // ...
} catch (SQLException e) {
    throw new DataAccessException("Failed to insert batch: " + records.size() + " records", e);
}

// Bad
Connection conn = dataSource.getConnection();
// ... conn.close() 누락 가능
```

### 로깅
- SLF4J + Logback 사용
- 클래스별 Logger 선언
- MDC로 컨텍스트 추가

```java
private static final Logger log = LoggerFactory.getLogger(JdbcBatchInsertRepository.class);

log.info("Starting batch insert: records={}, batchSize={}", records.size(), batchSize);
```

---

## 테스트 작성 원칙

### 필수 테스트 케이스
각 public 메서드당 최소:
1. 정상 케이스 (Happy Path)
2. 경계값 케이스 (빈 리스트, 단일 요소, 배치 크기 경계)
3. 예외 케이스 (null 입력, DB 에러)

### 테스트 구조
```java
@Nested
@DisplayName("insertBatch 메서드")
class InsertBatchTest {
    
    @Test
    @DisplayName("정상: 1000건 삽입 성공")
    void shouldInsertRecordsSuccessfully() { }
    
    @Test
    @DisplayName("경계: 빈 리스트 입력 시 0 반환")
    void shouldReturnZeroForEmptyList() { }
    
    @Test
    @DisplayName("예외: null 입력 시 IllegalArgumentException")
    void shouldThrowExceptionForNullInput() { }
}
```

### Mocking 최소화
- 실제 DB 연동 테스트 우선 (Testcontainers 또는 H2)
- 외부 시스템만 Mocking

---

## Multi-Agent 워크플로우

### Agent 역할
- **Orchestrator**: 전체 워크플로우 제어, 품질 게이트 관리
- **Architect**: 기술 설계, 구조 결정
- **Developer**: 코드 구현, 기본 테스트 작성
- **Reviewer**: 코드 품질 검토, 설계 준수 확인
- **QA**: 테스트 커버리지 강화, 품질 검증
- **Fixer**: 리뷰/QA 이슈 수정

### 품질 게이트
| 전환 | 필수 조건 |
|------|----------|
| DESIGN → IMPLEMENT | 아키텍처 문서 완성 |
| IMPLEMENT → REVIEW | 컴파일 통과, 기본 테스트 통과 |
| REVIEW → QA | 리뷰 APPROVED |
| QA → COMPLETE | 커버리지 80%+, 전체 테스트 통과 |

### 상태 관리
- `.workflow/state.json`: 현재 진행 상태
- `.workflow/checkpoints/`: 단계별 체크포인트
- 중단 후 재개 시 마지막 체크포인트부터 계속

---

## 금지 사항

### 코드 품질
- [ ] System.out.println 사용 (Logger 사용)
- [ ] 하드코딩된 연결 정보
- [ ] 매직 넘버 (상수로 추출)
- [ ] 빈 catch 블록
- [ ] @SuppressWarnings 남용

### 워크플로우
- [ ] 검증 없이 다음 파일로 진행
- [ ] TODO 주석 남기고 넘어가기
- [ ] 설계 문서에 없는 임의 구조 추가
- [ ] 한 번에 3개 이상 파일 동시 생성

---

## 과거 실수 기반 주의사항

### Oracle 관련
- JDBC URL 형식: `jdbc:oracle:thin:@host:port:sid` 또는 `jdbc:oracle:thin:@//host:port/service`
- Oracle TIMESTAMP는 java.time.Instant로 매핑
- SEQUENCE 사용 시 CURRVAL은 NEXTVAL 호출 후에만 사용 가능
- MERGE 구문 사용 시 조건절 주의

### MyBatis 관련
- BATCH 모드에서는 selectKey 동작 다름
- flushStatements() 호출 잊지 말 것
- SqlSession은 반드시 close

### Spring Boot 관련
- @Transactional은 public 메서드에만 적용
- DataSource 빈 중복 정의 주의
- 프로퍼티 바인딩 시 kebab-case 사용

---

## 유용한 명령어

```bash
# 전체 빌드
mvn clean package

# 컴파일만
mvn compile test-compile

# 테스트 실행
mvn test

# 특정 테스트 클래스 실행
mvn test -Dtest=JdbcBatchInsertRepositoryTest

# 커버리지 리포트
mvn jacoco:report
# 결과: target/site/jacoco/index.html

# 애플리케이션 실행
mvn spring-boot:run

# 클린 빌드
mvn clean package

# 의존성 트리 확인
mvn dependency:tree
```

---

## 파일 위치 규칙

| 파일 유형 | 위치 |
|----------|------|
| Java 소스 | src/main/java/com/example/benchmark/ |
| 테스트 소스 | src/test/java/com/example/benchmark/ |
| 설정 파일 | src/main/resources/ |
| MyBatis Mapper XML | src/main/resources/mapper/ |
| SQL 스크립트 | src/main/resources/ |
| 테스트 리소스 | src/test/resources/ |
| 빌드 산출물 | target/ |
| 커버리지 리포트 | target/site/jacoco/ |

---

## 의존성 버전 (참조용)

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<properties>
    <java.version>1.8</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.2</version>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
        <version>21.9.0.0</version>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```
