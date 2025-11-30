#!/bin/bash
# ============================================================
# Multi-Agent Development Workflow
# PRD 기반 + 상태 지속성 지원
# ============================================================

set -e

# 전역 에러 핸들링
trap 'log_error "Script interrupted"; exit 1' INT TERM

SCRIPT_DIR=$(dirname $(realpath $0))
PROJECT_ROOT=$(dirname $SCRIPT_DIR)
WORKFLOW_DIR="$PROJECT_ROOT/.workflow"
AGENTS_DIR="$PROJECT_ROOT/.agents"
STATE_FILE="$WORKFLOW_DIR/state.json"
PRD_FILE="$PROJECT_ROOT/PRD.txt"
LOG_DIR="$WORKFLOW_DIR/logs"
LOG_FILE="$LOG_DIR/workflow-$(date +%Y-%m-%d).log"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# ============================================================
# 유틸리티 함수
# ============================================================

log() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "[$timestamp] [$level] $message" | tee -a "$LOG_FILE"
}

# 스피너 표시 (백그라운드 프로세스용)
show_spinner() {
    local pid=$1
    local message=${2:-"Processing"}
    local spin='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    local start_time=$(date +%s)
    local i=0

    # 커서 숨김
    tput civis 2>/dev/null || true

    while kill -0 "$pid" 2>/dev/null; do
        local elapsed=$(($(date +%s) - start_time))
        local mins=$((elapsed / 60))
        local secs=$((elapsed % 60))
        local spin_char="${spin:$((i % 10)):1}"
        printf "\r  ${CYAN}%s${NC} %s ${YELLOW}[%02d:%02d]${NC}  " "$spin_char" "$message" "$mins" "$secs"
        i=$((i + 1))
        sleep 0.1
    done

    # 커서 복원 및 라인 클리어
    tput cnorm 2>/dev/null || true
    printf "\r\033[K"
}

# 경과 시간과 함께 명령 실행
run_with_spinner() {
    local message=$1
    shift
    local log_file=${1:-""}
    shift
    local cmd="$*"

    local start_time=$(date +%s)

    # 백그라운드로 실행
    if [ -n "$log_file" ]; then
        eval "$cmd" > "$log_file" 2>&1 &
    else
        eval "$cmd" > /dev/null 2>&1 &
    fi
    local pid=$!

    # 스피너 표시
    show_spinner $pid "$message"

    # 결과 대기
    wait $pid
    local exit_code=$?
    local elapsed=$(($(date +%s) - start_time))

    if [ $exit_code -eq 0 ]; then
        echo -e "  ${GREEN}✔${NC} $message ${CYAN}(${elapsed}s)${NC}"
    else
        echo -e "  ${RED}✘${NC} $message ${CYAN}(${elapsed}s)${NC}"
    fi

    return $exit_code
}

log_info() { log "${BLUE}INFO${NC}" "$1"; }
log_warn() { log "${YELLOW}WARN${NC}" "$1"; }
log_error() { log "${RED}ERROR${NC}" "$1"; }
log_success() { log "${GREEN}SUCCESS${NC}" "$1"; }

print_banner() {
    echo -e "${CYAN}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Multi-Agent Development Workflow"
    echo "  PRD-Driven | Stateful | Quality-Gated"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${NC}"
}

print_phase() {
    local phase=$1
    local icon=$2
    echo ""
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  $icon Phase: $phase${NC}"
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# JSON 파싱 (jq 사용)
json_get() {
    local file=$1
    local path=$2
    jq -r "$path" "$file" 2>/dev/null || echo "null"
}

json_set() {
    local file=$1
    local path=$2
    local value=$3
    local tmp=$(mktemp)
    jq "$path = $value" "$file" > "$tmp" && mv "$tmp" "$file"
}

# PRD 해시 계산 (변경 감지용) - Cross-platform
get_prd_hash() {
    if [ -f "$PRD_FILE" ]; then
        if command -v md5sum &> /dev/null; then
            md5sum "$PRD_FILE" | cut -d' ' -f1
        else
            md5 -q "$PRD_FILE"  # macOS/BSD
        fi
    else
        echo "no-prd"
    fi
}

# Cross-platform ISO 날짜 함수
get_iso_date() {
    if date -Iseconds &>/dev/null 2>&1; then
        date -Iseconds
    else
        date -u +"%Y-%m-%dT%H:%M:%SZ"  # macOS/BSD
    fi
}

# ============================================================
# 상태 관리
# ============================================================

init_directories() {
    mkdir -p "$WORKFLOW_DIR"/{checkpoints,artifacts,logs}
    mkdir -p "$PROJECT_ROOT"/src/{main,test}/{java,resources}
}

init_state() {
    log_info "Initializing workflow state..."
    
    init_directories
    
    if [ ! -f "$STATE_FILE" ]; then
        cat > "$STATE_FILE" << EOF
{
  "version": "1.0",
  "prd_hash": "$(get_prd_hash)",
  "created_at": "$(get_iso_date)",
  "updated_at": "$(get_iso_date)",
  "workflow": {
    "status": "NOT_STARTED",
    "current_phase": "INIT",
    "current_step": 0,
    "total_steps": 0
  },
  "phases": {
    "INIT": {"status": "PENDING"},
    "DESIGN": {"status": "PENDING"},
    "PLAN": {"status": "PENDING"},
    "IMPLEMENT": {"status": "PENDING"},
    "REVIEW": {"status": "PENDING"},
    "QA": {"status": "PENDING"},
    "COMPLETE": {"status": "PENDING"}
  },
  "quality_gates": {
    "compile": {"last_run": null, "result": null},
    "test": {"last_run": null, "result": null, "count": 0},
    "coverage": {"last_run": null, "result": null, "percentage": 0}
  },
  "resume_context": {
    "last_agent": null,
    "last_action": null,
    "pending_task": null,
    "relevant_files": []
  }
}
EOF
        log_info "Created new state file"
    fi
}

save_checkpoint() {
    local phase=$1
    local step=$2
    local context=${3:-"{}"}
    
    local phase_lower=$(echo "$phase" | tr '[:upper:]' '[:lower:]')
    local checkpoint_file="$WORKFLOW_DIR/checkpoints/$(printf '%02d' $step)-${phase_lower}.json"
    
    cat > "$checkpoint_file" << EOF
{
  "phase": "$phase",
  "step": $step,
  "timestamp": "$(get_iso_date)",
  "context": $context
}
EOF
    
    log_info "Checkpoint saved: $(basename $checkpoint_file)"
}

# Git 자동 커밋 함수
auto_commit() {
    local phase="$1"
    local step="$2"
    local message="$3"

    # 변경사항 확인
    if [ -z "$(git status --porcelain 2>/dev/null)" ]; then
        log_info "No changes to commit"
        return 0
    fi

    # 스테이징 및 커밋
    git add -A
    git commit -m "[$phase] $message

Step: $step
Generated with Claude Code

Co-Authored-By: Claude <noreply@anthropic.com>" >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        log_success "Committed: [$phase] $message"
    else
        log_warn "Commit failed (possibly nothing to commit)"
    fi
}

update_state() {
    local phase=$1
    local status=$2
    local step=${3:-0}
    
    local tmp=$(mktemp)
    jq --arg phase "$phase" \
       --arg status "$status" \
       --argjson step "$step" \
       --arg time "$(get_iso_date)" \
       '.workflow.current_phase = $phase |
        .workflow.status = $status |
        .workflow.current_step = $step |
        .updated_at = $time |
        .phases[$phase].status = $status |
        if $status == "IN_PROGRESS" then .phases[$phase].started_at = $time else . end |
        if $status == "COMPLETED" then .phases[$phase].completed_at = $time else . end' \
       "$STATE_FILE" > "$tmp" && mv "$tmp" "$STATE_FILE"
}

update_resume_context() {
    local agent=$1
    local action=$2
    local pending=${3:-""}
    
    local tmp=$(mktemp)
    jq --arg agent "$agent" \
       --arg action "$action" \
       --arg pending "$pending" \
       '.resume_context.last_agent = $agent |
        .resume_context.last_action = $action |
        .resume_context.pending_task = $pending' \
       "$STATE_FILE" > "$tmp" && mv "$tmp" "$STATE_FILE"
}

# ============================================================
# Agent 실행
# ============================================================

run_agent() {
    local agent=$1
    local task=$2
    local output_file=${3:-""}
    
    echo ""
    echo -e "${BOLD}🤖 Running Agent: ${CYAN}$agent${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    update_resume_context "$agent" "$task" "Executing..."
    
    # Agent 프롬프트 파일 확인
    local agent_file="$AGENTS_DIR/$agent.md"
    if [ ! -f "$agent_file" ]; then
        log_error "Agent file not found: $agent_file"
        return 1
    fi
    
    # 프롬프트 구성
    local prompt="당신은 Multi-Agent 개발 워크플로우의 $agent Agent입니다.

아래의 역할 정의를 따라 작업을 수행하세요:

---
$(cat "$agent_file")
---

# PRD (Product Requirements Document)
$(cat "$PRD_FILE")

---

# 프로젝트 컨벤션 (CLAUDE.md)
$(cat "$PROJECT_ROOT/CLAUDE.md")

---

# Current Workflow State
$(cat "$STATE_FILE")

---

# Task
$task

---

위 정보를 바탕으로 역할을 수행하고, 결과를 명확히 보고하세요."

    # Claude 실행 (claude CLI 사용)
    if command -v claude &> /dev/null; then
        local tmp_output=$(mktemp)
        local start_time=$(date +%s)

        # 백그라운드로 Claude 실행 (도구 사용 허용)
        echo "$prompt" | claude --dangerously-skip-permissions -p > "$tmp_output" 2>&1 &
        local pid=$!

        # 스피너 표시
        show_spinner $pid "Waiting for Claude ($agent)"

        # 결과 대기
        wait $pid || true
        local elapsed=$(($(date +%s) - start_time))

        # 결과 처리
        if [ -n "$output_file" ]; then
            mv "$tmp_output" "$output_file"
            echo -e "  ${GREEN}✔${NC} Output saved: $(basename $output_file) ${CYAN}(${elapsed}s)${NC}"
        else
            cat "$tmp_output"
            rm -f "$tmp_output"
            echo -e "  ${GREEN}✔${NC} Claude response received ${CYAN}(${elapsed}s)${NC}"
        fi
    else
        log_warn "Claude CLI not found. Please install Claude Code."
        log_info "Prompt saved to: /tmp/agent-prompt-$agent.txt"
        echo "$prompt" > "/tmp/agent-prompt-$agent.txt"

        # 대안: 프롬프트를 파일로 저장하고 수동 실행 안내
        echo ""
        echo -e "${YELLOW}Claude CLI가 설치되지 않았습니다.${NC}"
        echo "다음 명령으로 Claude Code에서 직접 실행하세요:"
        echo ""
        echo "  cat /tmp/agent-prompt-$agent.txt | claude"
        echo ""
        read -p "작업 완료 후 Enter를 눌러 계속하세요..."
    fi

    log_success "Agent $agent completed"
}

# ============================================================
# 품질 게이트
# ============================================================

run_quality_gate() {
    local gate=$1
    echo -e "\n🔍 Quality Gate: ${BOLD}$gate${NC}"

    local result="FAIL"
    local details=""
    local log_file="/tmp/mvn-${gate}.log"

    # Maven pom.xml 확인
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        log_warn "pom.xml not found. Skipping $gate gate."
        return 0
    fi

    cd "$PROJECT_ROOT"

    local start_time=$(date +%s)

    case $gate in
        "compile")
            # 백그라운드로 Maven 실행
            mvn compile test-compile > "$log_file" 2>&1 &
            local pid=$!

            # 스피너 표시
            show_spinner $pid "Compiling sources"

            if wait $pid; then
                result="PASS"
            else
                # 실패 시 에러 로그 출력
                echo -e "\n${RED}Compile errors:${NC}"
                grep -A 5 "\[ERROR\]" "$log_file" | head -30
            fi
            ;;
        "test")
            # 백그라운드로 테스트 실행
            mvn test > "$log_file" 2>&1 &
            local pid=$!

            # 스피너 표시
            show_spinner $pid "Executing tests"

            if wait $pid; then
                result="PASS"
                # 테스트 결과 추출 (다양한 패턴 지원)
                details=$(grep -E "Tests run:|tests" "$log_file" | grep -oE '[0-9]+ tests?' | head -1 || echo "")
                if [ -z "$details" ]; then
                    details=$(grep "Tests run:" "$log_file" | head -1 | sed 's/.*Tests run: //' | cut -d',' -f1 || echo "")
                    [ -n "$details" ] && details="$details tests"
                fi
            else
                # 실패 시 에러 로그 출력
                echo -e "\n${RED}Test failures:${NC}"
                grep -B 2 -A 10 "FAILURE\|ERROR" "$log_file" | head -40
            fi
            ;;
        "coverage")
            # 백그라운드로 커버리지 리포트 생성
            mvn jacoco:report > "$log_file" 2>&1 &
            local pid=$!

            # 스피너 표시
            show_spinner $pid "Generating coverage report"

            if wait $pid; then
                local report_file="$PROJECT_ROOT/target/site/jacoco/index.html"
                if [ -f "$report_file" ]; then
                    # 커버리지 추출 (다양한 패턴 지원)
                    local coverage=$(grep -oE 'Total[^%]*[0-9]+' "$report_file" 2>/dev/null | grep -oE '[0-9]+$' | head -1 || echo "0")
                    if [ -z "$coverage" ] || [ "$coverage" = "0" ]; then
                        # 대안 패턴
                        coverage=$(grep -oE '[0-9]+%' "$report_file" 2>/dev/null | head -1 | tr -d '%' || echo "0")
                    fi
                    details="${coverage}%"
                    if [ "$coverage" -ge 80 ] 2>/dev/null; then
                        result="PASS"
                    fi
                fi
            fi
            ;;
    esac

    local elapsed=$(($(date +%s) - start_time))

    # 상태 업데이트
    local tmp=$(mktemp)
    jq --arg gate "$gate" \
       --arg result "$result" \
       --arg time "$(get_iso_date)" \
       --arg details "$details" \
       '.quality_gates[$gate] = {
          "last_run": $time,
          "result": $result,
          "details": $details
        }' "$STATE_FILE" > "$tmp" && mv "$tmp" "$STATE_FILE"

    if [ "$result" = "PASS" ]; then
        echo -e "  ${GREEN}✅ PASS${NC} $details ${CYAN}(${elapsed}s)${NC}"
        return 0
    else
        echo -e "  ${RED}❌ FAIL${NC} $details ${CYAN}(${elapsed}s)${NC}"
        return 1
    fi
}

# ============================================================
# 워크플로우 실행
# ============================================================

check_prd() {
    if [ ! -f "$PRD_FILE" ]; then
        log_error "PRD.txt not found!"
        echo ""
        echo "PRD.txt 파일을 프로젝트 루트에 생성하세요."
        echo "위치: $PRD_FILE"
        exit 1
    fi
}

check_resume() {
    if [ ! -f "$STATE_FILE" ]; then
        return 1  # 새로 시작
    fi
    
    local status=$(json_get "$STATE_FILE" '.workflow.status')
    
    if [ "$status" = "IN_PROGRESS" ] || [ "$status" = "PAUSED" ]; then
        return 0  # 재개 가능
    fi
    
    return 1  # 새로 시작
}

check_prd_changed() {
    local saved_hash=$(json_get "$STATE_FILE" '.prd_hash')
    local current_hash=$(get_prd_hash)
    
    if [ "$saved_hash" != "$current_hash" ] && [ "$saved_hash" != "null" ]; then
        return 0  # 변경됨
    fi
    return 1  # 변경 안됨
}

get_resume_point() {
    local phase=$(json_get "$STATE_FILE" '.workflow.current_phase')
    local step=$(json_get "$STATE_FILE" '.workflow.current_step')
    echo "$phase:$step"
}

should_run_phase() {
    local phase=$1
    local start_phase=$2
    
    local phase_order="INIT DESIGN PLAN IMPLEMENT REVIEW QA COMPLETE"
    local start_idx=$(echo "$phase_order" | tr ' ' '\n' | grep -n "^$start_phase$" | cut -d: -f1)
    local current_idx=$(echo "$phase_order" | tr ' ' '\n' | grep -n "^$phase$" | cut -d: -f1)
    
    [ "$current_idx" -ge "$start_idx" ]
}

run_workflow() {
    local mode=${1:-"auto"}  # auto, new, resume
    
    print_banner
    log_info "Starting Multi-Agent Workflow (mode: $mode)"
    
    # PRD 확인
    check_prd
    
    # 초기화
    init_state
    
    # 재개 여부 확인
    local start_phase="INIT"
    local start_step=0
    
    if [ "$mode" = "auto" ] && check_resume; then
        local resume_point=$(get_resume_point)
        start_phase=$(echo "$resume_point" | cut -d: -f1)
        start_step=$(echo "$resume_point" | cut -d: -f2)
        
        echo ""
        echo -e "${YELLOW}📍 이전 진행 상태 발견${NC}"
        echo "   Phase: $start_phase, Step: $start_step"
        
        local last_agent=$(json_get "$STATE_FILE" '.resume_context.last_agent')
        local pending=$(json_get "$STATE_FILE" '.resume_context.pending_task')
        echo "   Last Agent: $last_agent"
        echo "   Pending: $pending"
        echo ""
        read -p "이어서 진행하시겠습니까? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            start_phase="INIT"
            start_step=0
        fi
        
    elif [ "$mode" = "new" ]; then
        log_info "Starting fresh workflow..."
        rm -f "$STATE_FILE"
        rm -rf "$WORKFLOW_DIR/checkpoints"/*
        rm -rf "$WORKFLOW_DIR/artifacts"/*
        init_state
    fi
    
    # PRD 변경 감지
    if check_prd_changed; then
        echo ""
        echo -e "${YELLOW}⚠️  PRD.txt가 이전 실행 이후 변경되었습니다!${NC}"
        read -p "계속 진행하시겠습니까? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
        json_set "$STATE_FILE" '.prd_hash' "\"$(get_prd_hash)\""
    fi
    
    update_state "INIT" "IN_PROGRESS" 0
    
    # ============================================================
    # Phase 1: DESIGN
    # ============================================================
    if should_run_phase "DESIGN" "$start_phase"; then
        print_phase "DESIGN" "📐"
        update_state "DESIGN" "IN_PROGRESS" 1
        
        run_agent "architect" \
            "PRD.txt를 분석하고 상세 아키텍처를 설계하세요.
            결과를 architecture.md 형식으로 출력하세요." \
            "$WORKFLOW_DIR/artifacts/architecture.md"
        
        save_checkpoint "DESIGN" 1 '{"artifact": "architecture.md"}'
        update_state "DESIGN" "COMPLETED" 1
        auto_commit "DESIGN" 1 "Architecture document created"
        log_success "DESIGN phase completed"
    fi
    
    # ============================================================
    # Phase 2: PLAN
    # ============================================================
    if should_run_phase "PLAN" "$start_phase"; then
        print_phase "PLAN" "📋"
        update_state "PLAN" "IN_PROGRESS" 2
        
        run_agent "orchestrator" \
            "아키텍처 문서를 기반으로 상세 구현 계획을 수립하세요.
            각 Step별로 구현 항목, 예상 파일, 검증 방법을 포함하세요.
            
            Architecture:
            $(cat "$WORKFLOW_DIR/artifacts/architecture.md" 2>/dev/null || echo 'Not available')" \
            "$WORKFLOW_DIR/artifacts/implementation-plan.md"
        
        # 총 단계 수 추출 (기본값 17)
        local total_steps=17
        if [ -f "$WORKFLOW_DIR/artifacts/implementation-plan.md" ]; then
            local extracted=$(grep -c "^### Step\|^## Step" "$WORKFLOW_DIR/artifacts/implementation-plan.md" 2>/dev/null || echo "0")
            if [ "$extracted" -gt 0 ]; then
                total_steps=$extracted
            fi
        fi
        
        local tmp=$(mktemp)
        jq --argjson total "$total_steps" '.workflow.total_steps = $total' "$STATE_FILE" > "$tmp" && mv "$tmp" "$STATE_FILE"
        
        save_checkpoint "PLAN" 2 "{\"total_steps\": $total_steps}"
        update_state "PLAN" "COMPLETED" 2
        auto_commit "PLAN" 2 "Implementation plan created"
        log_success "PLAN phase completed (Total steps: $total_steps)"
    fi
    
    # ============================================================
    # Phase 3: IMPLEMENT
    # ============================================================
    if should_run_phase "IMPLEMENT" "$start_phase"; then
        print_phase "IMPLEMENT" "💻"
        update_state "IMPLEMENT" "IN_PROGRESS" 3
        
        local total_steps=$(json_get "$STATE_FILE" '.workflow.total_steps')
        local impl_start=1
        
        # 재개 시 시작 스텝 결정
        if [ "$start_phase" = "IMPLEMENT" ] && [ "$start_step" -gt 2 ]; then
            impl_start=$((start_step - 2))
        fi
        
        for ((step=impl_start; step<=total_steps; step++)); do
            echo ""
            echo -e "  ${BOLD}▶ Implementation Step $step/$total_steps${NC}"
            update_state "IMPLEMENT" "IN_PROGRESS" $((step + 2))
            
            # Developer Agent 실행
            run_agent "developer" \
                "구현 계획의 Step $step을 구현하세요.
                
                Implementation Plan:
                $(cat "$WORKFLOW_DIR/artifacts/implementation-plan.md" 2>/dev/null || echo 'Not available')
                
                Architecture:
                $(cat "$WORKFLOW_DIR/artifacts/architecture.md" 2>/dev/null || echo 'Not available')
                
                현재 Step: $step
                
                구현 후 반드시 컴파일 검증을 수행하세요."
            
            # 컴파일 게이트
            local retry=0
            while ! run_quality_gate "compile"; do
                ((retry++))
                if [ $retry -ge 3 ]; then
                    log_error "Compile failed after 3 attempts. Pausing workflow."
                    update_state "IMPLEMENT" "PAUSED" $((step + 2))
                    update_resume_context "developer" "compile failed at step $step" "Fix compilation errors"
                    echo ""
                    echo -e "${YELLOW}워크플로우가 일시 중지되었습니다.${NC}"
                    echo "컴파일 에러를 수정한 후 다시 시작하세요:"
                    echo "  ./scripts/workflow.sh resume"
                    exit 1
                fi
                
                log_warn "Compile failed, calling fixer (attempt $retry/3)"
                run_agent "fixer" "컴파일 에러를 분석하고 수정하세요."
            done
            
            # 테스트 게이트 (선택적)
            run_quality_gate "test" || {
                log_warn "Tests failed, calling fixer"
                run_agent "fixer" "테스트 실패를 분석하고 수정하세요."
                run_quality_gate "test" || true
            }
            
            # 체크포인트 저장
            save_checkpoint "IMPLEMENT" $((step + 2)) "{\"impl_step\": $step, \"status\": \"completed\"}"
            auto_commit "IMPLEMENT" $((step + 2)) "Step $step completed"
            log_success "Implementation step $step completed"
        done

        update_state "IMPLEMENT" "COMPLETED" $((total_steps + 2))
        auto_commit "IMPLEMENT" $((total_steps + 2)) "Implementation phase completed"
        log_success "IMPLEMENT phase completed"
    fi
    
    # ============================================================
    # Phase 4: REVIEW
    # ============================================================
    if should_run_phase "REVIEW" "$start_phase"; then
        print_phase "REVIEW" "🔍"
        update_state "REVIEW" "IN_PROGRESS"
        
        local review_pass=false
        local review_attempt=0
        
        while [ "$review_pass" = false ] && [ $review_attempt -lt 3 ]; do
            ((review_attempt++))
            
            run_agent "reviewer" \
                "구현된 코드를 리뷰하세요.
                아키텍처 문서와의 일치성, 코드 품질, 잠재적 이슈를 검토하세요.
                
                Architecture:
                $(cat "$WORKFLOW_DIR/artifacts/architecture.md" 2>/dev/null || echo 'Not available')" \
                "$WORKFLOW_DIR/artifacts/review-report.md"
            
            # 리뷰 결과 확인
            if [ -f "$WORKFLOW_DIR/artifacts/review-report.md" ]; then
                if grep -q "APPROVED" "$WORKFLOW_DIR/artifacts/review-report.md"; then
                    review_pass=true
                else
                    log_warn "Review requested changes (attempt $review_attempt/3)"
                    run_agent "fixer" \
                        "리뷰 지적 사항을 수정하세요.
                        
                        Review Report:
                        $(cat "$WORKFLOW_DIR/artifacts/review-report.md")"
                fi
            else
                review_pass=true  # 리포트 없으면 통과로 간주
            fi
        done
        
        if [ "$review_pass" = false ]; then
            log_error "Review failed after 3 attempts"
            update_state "REVIEW" "FAILED"
            exit 1
        fi
        
        update_state "REVIEW" "COMPLETED"
        auto_commit "REVIEW" 0 "Code review passed"
        log_success "REVIEW phase completed"
    fi

    # ============================================================
    # Phase 5: QA
    # ============================================================
    if should_run_phase "QA" "$start_phase"; then
        print_phase "QA" "🧪"
        update_state "QA" "IN_PROGRESS"
        
        run_agent "qa" \
            "테스트 커버리지를 분석하고 필요시 보강하세요.
            Edge case와 통합 테스트를 추가하세요." \
            "$WORKFLOW_DIR/artifacts/qa-report.md"
        
        # 최종 품질 게이트
        run_quality_gate "test"
        run_quality_gate "coverage" || {
            log_warn "Coverage below threshold, adding more tests"
            run_agent "qa" "커버리지가 부족합니다. 추가 테스트를 작성하세요."
            run_quality_gate "coverage" || true
        }
        
        update_state "QA" "COMPLETED"
        auto_commit "QA" 0 "Quality assurance completed"
        log_success "QA phase completed"
    fi

    # ============================================================
    # 완료
    # ============================================================
    print_phase "COMPLETE" "✅"
    update_state "COMPLETE" "SUCCESS"
    auto_commit "COMPLETE" 0 "Workflow completed successfully"
    
    echo ""
    echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}${BOLD}  ✅ Workflow Completed Successfully!${NC}"
    echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${BOLD}📊 산출물:${NC}"
    echo "  - Architecture: $WORKFLOW_DIR/artifacts/architecture.md"
    echo "  - Implementation Plan: $WORKFLOW_DIR/artifacts/implementation-plan.md"
    echo "  - Review Report: $WORKFLOW_DIR/artifacts/review-report.md"
    echo "  - QA Report: $WORKFLOW_DIR/artifacts/qa-report.md"
    echo "  - Logs: $LOG_FILE"
    echo ""
}

# ============================================================
# 상태 표시
# ============================================================

show_status() {
    if [ ! -f "$STATE_FILE" ]; then
        echo "워크플로우 상태가 없습니다."
        echo "시작하려면: ./workflow.sh start"
        exit 0
    fi
    
    echo ""
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  📊 Workflow Status${NC}"
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    local status=$(json_get "$STATE_FILE" '.workflow.status')
    local phase=$(json_get "$STATE_FILE" '.workflow.current_phase')
    local step=$(json_get "$STATE_FILE" '.workflow.current_step')
    local total=$(json_get "$STATE_FILE" '.workflow.total_steps')
    local updated=$(json_get "$STATE_FILE" '.updated_at')
    
    echo -e "  Status:       ${BOLD}$status${NC}"
    echo -e "  Phase:        $phase"
    echo -e "  Progress:     Step $step / $total"
    echo -e "  Last Updated: $updated"
    echo ""
    
    echo -e "${BOLD}  Phase Status:${NC}"
    for p in DESIGN PLAN IMPLEMENT REVIEW QA; do
        local p_status=$(json_get "$STATE_FILE" ".phases.$p.status")
        local icon="⬜"
        case $p_status in
            "COMPLETED") icon="✅" ;;
            "IN_PROGRESS") icon="🔄" ;;
            "FAILED"|"PAUSED") icon="❌" ;;
        esac
        printf "    %s %-10s: %s\n" "$icon" "$p" "$p_status"
    done
    
    echo ""
    echo -e "${BOLD}  Quality Gates:${NC}"
    for g in compile test coverage; do
        local g_result=$(json_get "$STATE_FILE" ".quality_gates.$g.result")
        local g_details=$(json_get "$STATE_FILE" ".quality_gates.$g.details")
        [ "$g_result" = "null" ] && g_result="NOT_RUN"
        [ "$g_details" = "null" ] && g_details=""
        printf "    - %-10s: %s %s\n" "$g" "$g_result" "$g_details"
    done
    
    if [ "$status" = "IN_PROGRESS" ] || [ "$status" = "PAUSED" ]; then
        echo ""
        echo -e "${BOLD}  Resume Context:${NC}"
        local last_agent=$(json_get "$STATE_FILE" '.resume_context.last_agent')
        local pending=$(json_get "$STATE_FILE" '.resume_context.pending_task')
        echo "    Last Agent: $last_agent"
        echo "    Pending: $pending"
        echo ""
        echo -e "  ${YELLOW}재개하려면: ./workflow.sh resume${NC}"
    fi
    
    echo ""
}

# ============================================================
# 도움말
# ============================================================

show_help() {
    echo ""
    echo -e "${BOLD}Usage:${NC} ./workflow.sh [command]"
    echo ""
    echo -e "${BOLD}Basic Commands:${NC}"
    echo "  start     워크플로우 시작 (자동으로 재개 또는 새로 시작)"
    echo "  new       새 워크플로우 강제 시작 (이전 상태 삭제)"
    echo "  resume    마지막 체크포인트에서 재개"
    echo "  status    현재 워크플로우 상태 표시"
    echo "  reset     워크플로우 상태 초기화"
    echo "  help      이 도움말 표시"
    echo ""
    echo -e "${BOLD}Parallel Commands:${NC}"
    echo "  parallel init      Git worktrees 초기화 (Agent별 작업 디렉토리)"
    echo "  parallel start     병렬 Claude Code 세션 시작"
    echo "  parallel status    병렬 실행 상태 확인"
    echo "  parallel watch     실시간 모니터링"
    echo "  parallel sync      모든 worktree를 main에 동기화"
    echo "  parallel cleanup   모든 worktree 제거"
    echo "  parallel list      Worktree 목록 조회"
    echo ""
    echo -e "${BOLD}Rollback Commands:${NC}"
    echo "  rollback list      롤백 가능 지점 목록"
    echo "  rollback to <tag>  특정 태그/커밋으로 롤백"
    echo "  rollback tag <name> 롤백 지점 태그 생성"
    echo ""
    echo -e "${BOLD}Examples:${NC}"
    echo "  ./workflow.sh start              # 자동으로 시작 또는 재개"
    echo "  ./workflow.sh parallel init      # Worktree 초기화"
    echo "  ./workflow.sh parallel start     # 병렬 세션 시작"
    echo "  ./workflow.sh rollback list      # 롤백 지점 확인"
    echo ""
    echo -e "${BOLD}Prerequisites:${NC}"
    echo "  - PRD.txt 파일이 프로젝트 루트에 존재해야 합니다"
    echo "  - Claude CLI (claude) 설치 권장"
    echo "  - jq 설치 필요 (JSON 파싱)"
    echo "  - Git 초기화 필요 (병렬 실행 시)"
    echo ""
}

# ============================================================
# 메인
# ============================================================

# jq 확인
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq가 설치되어 있지 않습니다.${NC}"
    echo "설치: sudo apt-get install jq"
    exit 1
fi

case "${1:-help}" in
    "start")
        run_workflow "auto"
        ;;
    "new")
        run_workflow "new"
        ;;
    "resume")
        run_workflow "resume"
        ;;
    "status")
        show_status
        ;;
    "reset")
        rm -rf "$WORKFLOW_DIR"
        log_info "Workflow state reset"
        echo "워크플로우 상태가 초기화되었습니다."
        ;;
    "parallel")
        # 병렬 실행 명령어
        shift
        case "${1:-help}" in
            "init")
                "$SCRIPT_DIR/worktree-manager.sh" init
                ;;
            "start")
                "$SCRIPT_DIR/start-parallel.sh" quick
                ;;
            "status"|"monitor")
                "$SCRIPT_DIR/parallel-runner.sh" monitor
                ;;
            "watch")
                "$SCRIPT_DIR/parallel-runner.sh" watch
                ;;
            "sync")
                "$SCRIPT_DIR/worktree-manager.sh" sync-all
                ;;
            "cleanup")
                "$SCRIPT_DIR/worktree-manager.sh" cleanup
                ;;
            "run")
                shift
                "$SCRIPT_DIR/parallel-runner.sh" run "$@"
                ;;
            "list")
                "$SCRIPT_DIR/worktree-manager.sh" list
                ;;
            *)
                echo "Parallel Workflow Commands:"
                echo ""
                echo "  parallel init      Initialize worktrees for all agents"
                echo "  parallel start     Start parallel Claude Code sessions"
                echo "  parallel status    Show parallel execution status"
                echo "  parallel watch     Real-time monitoring"
                echo "  parallel sync      Sync all worktrees to main"
                echo "  parallel cleanup   Remove all worktrees"
                echo "  parallel run <agents...>  Run specific agents"
                echo "  parallel list      List all worktrees"
                ;;
        esac
        ;;
    "rollback")
        # 롤백 명령어
        shift
        case "${1:-help}" in
            "list")
                echo -e "${BOLD}Available Rollback Points:${NC}"
                echo ""
                echo "  [Git Tags - Phase Completions]"
                git tag -l "phase/*" --sort=-creatordate 2>/dev/null | head -10 | while read tag; do
                    local tag_date=$(git log -1 --format=%ci "$tag" 2>/dev/null | cut -d' ' -f1)
                    echo "    $tag ($tag_date)"
                done || echo "    (no tags found)"
                echo ""
                echo "  [Recent Commits]"
                git log --oneline -10 2>/dev/null || echo "    (no commits)"
                ;;
            "to")
                if [ -z "$2" ]; then
                    log_error "Tag or commit required"
                    echo "Usage: workflow.sh rollback to <tag|commit>"
                    exit 1
                fi
                local target="$2"
                echo -e "${YELLOW}Rolling back to: $target${NC}"
                echo ""
                git log --oneline HEAD..."$target" 2>/dev/null | head -10
                echo ""
                read -p "Are you sure? (y/n) " -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    # 백업 태그 생성
                    git tag "backup/pre-rollback-$(date +%Y%m%d%H%M%S)" 2>/dev/null || true
                    # 롤백 실행
                    git reset --hard "$target"
                    # 모든 worktree도 롤백
                    if [ -d "$PROJECT_ROOT/.worktrees" ]; then
                        for agent in architect developer reviewer qa fixer; do
                            local wt="$PROJECT_ROOT/.worktrees/$agent"
                            if [ -d "$wt" ]; then
                                (cd "$wt" && git reset --hard "$target" 2>/dev/null) || true
                            fi
                        done
                    fi
                    log_success "Rolled back to: $target"
                else
                    echo "Cancelled"
                fi
                ;;
            "tag")
                if [ -z "$2" ]; then
                    log_error "Tag name required"
                    echo "Usage: workflow.sh rollback tag <name>"
                    exit 1
                fi
                local tag_name="phase/$2"
                git tag -a "$tag_name" -m "Phase checkpoint: $2
Timestamp: $(get_iso_date)"
                log_success "Created tag: $tag_name"
                ;;
            *)
                echo "Rollback Commands:"
                echo ""
                echo "  rollback list         List available rollback points"
                echo "  rollback to <target>  Rollback to specific tag or commit"
                echo "  rollback tag <name>   Create a rollback point tag"
                ;;
        esac
        ;;
    "help"|"--help"|"-h"|*)
        show_help
        ;;
esac
