# Oracle Insert Performance Benchmark

JDBC batch insert와 MyBatis batch insert의 성능을 비교하는 벤치마크 프로젝트입니다.

## 🚀 Multi-Agent 개발 워크플로우

이 프로젝트는 AI Agent 기반의 자동화된 개발 워크플로우를 사용합니다.

### 구조

```
.
├── PRD.txt                 # 📋 요구사항 정의서
├── CLAUDE.md               # 🤖 AI 개발 지침
├── .agents/                # Agent 역할 정의
│   ├── orchestrator.md     # PM Agent
│   ├── architect.md        # Architect Agent
│   ├── developer.md        # Developer Agent
│   ├── reviewer.md         # Reviewer Agent
│   ├── qa.md              # QA Agent
│   └── fixer.md           # Fixer Agent
├── .workflow/              # 워크플로우 상태
│   ├── state.json         # 진행 상태
│   ├── checkpoints/       # 체크포인트
│   └── artifacts/         # 산출물
└── scripts/
    ├── workflow.sh        # 메인 워크플로우
    ├── status.sh          # 상태 확인
    └── resume.sh          # 재개
```

### 워크플로우 실행

```bash
# 스크립트 실행 권한 부여
chmod +x scripts/*.sh

# 워크플로우 시작
./scripts/workflow.sh start

# 상태 확인
./scripts/workflow.sh status

# 중단 후 재개
./scripts/workflow.sh resume

# 처음부터 다시 시작
./scripts/workflow.sh new
```

### 워크플로우 단계

```
INIT → DESIGN → PLAN → IMPLEMENT → REVIEW → QA → COMPLETE
```

| 단계 | Agent | 산출물 |
|------|-------|--------|
| DESIGN | Architect | architecture.md |
| PLAN | Orchestrator | implementation-plan.md |
| IMPLEMENT | Developer | 소스 코드 |
| REVIEW | Reviewer | review-report.md |
| QA | QA | qa-report.md |

### 품질 게이트

각 단계 전환 시 자동 검증:
- **Compile**: 컴파일 성공
- **Test**: 테스트 통과
- **Coverage**: 80% 이상 (QA 단계)

---

## 📋 요구사항

### 환경
- Java 1.8 (Java 8)
- Maven 3.8+
- Oracle 19c
- jq (JSON 파싱용)

### 설치

```bash
# jq 설치 (Ubuntu/Debian)
sudo apt-get install jq

# jq 설치 (macOS)
brew install jq

# Claude CLI 설치 (권장)
# https://github.com/anthropics/claude-code 참조
```

### 환경 변수

```bash
export DB_URL=jdbc:oracle:thin:@localhost:1521:xe
export DB_USERNAME=benchmark
export DB_PASSWORD=benchmark
export BATCH_SIZE=1000
export RECORD_COUNT=100000
```

---

## 🛠️ 수동 빌드 & 실행

```bash
# 빌드
mvn clean package

# 테스트
mvn test

# 커버리지 리포트
mvn jacoco:report
# 결과: target/site/jacoco/index.html

# 애플리케이션 실행
mvn spring-boot:run
```

---

## 📊 벤치마크 결과

벤치마크 실행 후 결과:
- 콘솔 출력
- `benchmark-results/` 디렉토리에 CSV 저장

---

## 📁 프로젝트 구조 (구현 후)

```
src/main/java/com/example/benchmark/
├── BenchmarkApplication.java
├── config/
│   ├── DataSourceConfig.java
│   └── MyBatisConfig.java
├── domain/
│   └── TestRecord.java
├── repository/
│   ├── BatchInsertRepository.java
│   ├── JdbcBatchInsertRepository.java
│   └── MyBatisBatchInsertRepository.java
├── benchmark/
│   ├── BenchmarkRunner.java
│   ├── BenchmarkResult.java
│   └── BenchmarkReportGenerator.java
└── util/
    └── TestDataGenerator.java
```

---

## 🔧 트러블슈팅

### 워크플로우가 멈춘 경우

```bash
# 상태 확인
./scripts/workflow.sh status

# 상태 초기화 후 재시작
./scripts/workflow.sh reset
./scripts/workflow.sh new
```

### 컴파일 에러가 해결되지 않는 경우

1. `.workflow/artifacts/` 산출물 확인
2. 수동으로 코드 수정
3. `./scripts/workflow.sh resume`로 재개

### PRD 변경 후

PRD.txt 수정 후 워크플로우 실행 시 변경 감지 경고가 표시됩니다.
필요시 새로 시작: `./scripts/workflow.sh new`

---

## 📝 라이센스

MIT License
