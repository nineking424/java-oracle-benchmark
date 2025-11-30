#!/bin/bash
# ============================================================
# 다층 검증 스크립트
# Level 1: 컴파일 → Level 2: 테스트 → Level 3: 커버리지
# ============================================================

set -e

PROJECT_ROOT=$(dirname $(dirname $(realpath $0)))
cd "$PROJECT_ROOT"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'
BOLD='\033[1m'

echo ""
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}  🔍 Multi-Level Verification${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Maven pom.xml 확인
if [ ! -f "./pom.xml" ]; then
    echo -e "${YELLOW}⚠️  pom.xml not found. Skipping build verification.${NC}"
    echo "Ensure Maven project is properly configured"
    exit 0
fi

FAILED=0

# ============================================================
# Level 1: 컴파일 검증
# ============================================================
echo -e "${BOLD}Level 1: Compile Verification${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if mvn compile test-compile -q 2>/dev/null; then
    echo -e "  ${GREEN}✅ PASS${NC} - Source and test compilation successful"
else
    echo -e "  ${RED}❌ FAIL${NC} - Compilation errors found"
    echo ""
    echo -e "${YELLOW}Running with details:${NC}"
    mvn compile test-compile 2>&1 | tail -30
    FAILED=1
fi

if [ $FAILED -eq 1 ]; then
    echo ""
    echo -e "${RED}${BOLD}Verification stopped at Level 1 (Compile)${NC}"
    exit 1
fi

# ============================================================
# Level 2: 테스트 검증
# ============================================================
echo ""
echo -e "${BOLD}Level 2: Test Verification${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

TEST_OUTPUT=$(mvn test 2>&1) || true
TEST_RESULT=$?

if [ $TEST_RESULT -eq 0 ]; then
    # 테스트 수 추출 (Cross-platform)
    TEST_COUNT=$(echo "$TEST_OUTPUT" | grep -E 'Tests run: [0-9]+' | head -1 | sed 's/.*Tests run: \([0-9]*\).*/\1 tests/' || echo "? tests")
    echo -e "  ${GREEN}✅ PASS${NC} - All tests passed ($TEST_COUNT)"
else
    echo -e "  ${RED}❌ FAIL${NC} - Some tests failed"
    echo ""
    echo -e "${YELLOW}Failed tests:${NC}"
    echo "$TEST_OUTPUT" | grep -A 5 "FAILED\|AssertionError" | head -20
    FAILED=1
fi

if [ $FAILED -eq 1 ]; then
    echo ""
    echo -e "${RED}${BOLD}Verification stopped at Level 2 (Test)${NC}"
    echo "Test report: target/surefire-reports/"
    exit 1
fi

# ============================================================
# Level 3: 커버리지 검증
# ============================================================
echo ""
echo -e "${BOLD}Level 3: Coverage Verification${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 커버리지 추출 함수 (Cross-platform)
get_coverage() {
    local csv_file="target/site/jacoco/jacoco.csv"
    if [ -f "$csv_file" ]; then
        awk -F',' 'NR>1 {covered+=$5; missed+=$4} END {
            if(covered+missed>0) printf "%.0f", covered*100/(covered+missed)
            else print 0
        }' "$csv_file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

if mvn jacoco:report -q 2>/dev/null; then
    REPORT_FILE="target/site/jacoco/index.html"

    if [ -f "$REPORT_FILE" ]; then
        # 커버리지 추출 (CSV 우선 - 더 안정적)
        COVERAGE=$(get_coverage)

        if [ "$COVERAGE" -ge 80 ] 2>/dev/null; then
            echo -e "  ${GREEN}✅ PASS${NC} - Coverage: ${COVERAGE}% (≥80%)"
        elif [ "$COVERAGE" -ge 60 ] 2>/dev/null; then
            echo -e "  ${YELLOW}⚠️  WARN${NC} - Coverage: ${COVERAGE}% (target: ≥80%)"
        else
            echo -e "  ${RED}❌ FAIL${NC} - Coverage: ${COVERAGE}% (target: ≥80%)"
            FAILED=1
        fi
    else
        echo -e "  ${YELLOW}⚠️  SKIP${NC} - Coverage report not generated"
    fi
else
    echo -e "  ${YELLOW}⚠️  SKIP${NC} - JaCoCo not configured"
fi

# ============================================================
# 결과 요약
# ============================================================
echo ""
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}${BOLD}  ✅ All verification levels PASSED${NC}"
else
    echo -e "${RED}${BOLD}  ❌ Verification FAILED${NC}"
fi

echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 리포트 위치 안내
echo -e "${BOLD}Reports:${NC}"
echo "  - Test:     target/surefire-reports/"
echo "  - Coverage: target/site/jacoco/index.html"
echo ""

exit $FAILED
