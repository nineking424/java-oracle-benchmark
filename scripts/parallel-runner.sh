#!/bin/bash
# ============================================================
# Parallel Agent Runner for Multi-Agent Workflow
# 병렬 Agent 실행 및 Lock 관리
# ============================================================

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(dirname "$SCRIPT_DIR")
WORKFLOW_DIR="$PROJECT_ROOT/.workflow"
WORKTREE_BASE="$PROJECT_ROOT/.worktrees"
AGENTS_DIR="$PROJECT_ROOT/.agents"
STATE_FILE="$WORKFLOW_DIR/state.json"
LOCK_DIR="$WORKFLOW_DIR/locks"
SYNC_DIR="$WORKFLOW_DIR/sync"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

# Agent 목록
AGENTS=("architect" "developer" "reviewer" "qa" "fixer")

# ============================================================
# 유틸리티 함수
# ============================================================

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

get_iso_date() {
    if date -Iseconds &>/dev/null 2>&1; then
        date -Iseconds
    else
        date -u +"%Y-%m-%dT%H:%M:%SZ"
    fi
}

# 디렉토리 초기화
init_dirs() {
    mkdir -p "$LOCK_DIR"
    mkdir -p "$SYNC_DIR"
}

# ============================================================
# Lock 관리
# ============================================================

# Lock 획득
acquire_lock() {
    local agent=$1
    local lock_file="$LOCK_DIR/$agent.lock"

    init_dirs

    # 기존 Lock 확인
    if [ -f "$lock_file" ]; then
        local pid=$(cat "$lock_file" 2>/dev/null)
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            return 1  # 이미 실행 중
        fi
        # Stale lock 제거
        rm -f "$lock_file"
    fi

    # Lock 획득
    echo $$ > "$lock_file"
    return 0
}

# Lock 해제
release_lock() {
    local agent=$1
    local lock_file="$LOCK_DIR/$agent.lock"
    rm -f "$lock_file"
}

# 모든 Lock 상태 확인
check_locks() {
    init_dirs

    echo -e "${BOLD}Agent Lock Status:${NC}"
    echo ""

    for agent in "${AGENTS[@]}"; do
        local lock_file="$LOCK_DIR/$agent.lock"
        local status="${GREEN}available${NC}"

        if [ -f "$lock_file" ]; then
            local pid=$(cat "$lock_file" 2>/dev/null)
            if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                status="${RED}locked${NC} (PID: $pid)"
            else
                status="${YELLOW}stale${NC}"
            fi
        fi

        printf "  %-12s: %b\n" "$agent" "$status"
    done
}

# Stale Lock 정리
clean_stale_locks() {
    init_dirs

    log_info "Cleaning stale locks..."

    for agent in "${AGENTS[@]}"; do
        local lock_file="$LOCK_DIR/$agent.lock"
        if [ -f "$lock_file" ]; then
            local pid=$(cat "$lock_file" 2>/dev/null)
            if [ -z "$pid" ] || ! kill -0 "$pid" 2>/dev/null; then
                rm -f "$lock_file"
                log_info "Removed stale lock: $agent"
            fi
        fi
    done

    log_success "Stale locks cleaned"
}

# ============================================================
# Agent 실행
# ============================================================

# Agent 프롬프트 생성
generate_agent_prompt() {
    local agent=$1
    local task=$2
    local agent_file="$AGENTS_DIR/$agent.md"

    if [ ! -f "$agent_file" ]; then
        log_error "Agent file not found: $agent_file"
        return 1
    fi

    cat <<EOF
당신은 Multi-Agent 개발 워크플로우의 $agent Agent입니다.

아래의 역할 정의를 따라 작업을 수행하세요:

---
$(cat "$agent_file")
---

# 프로젝트 컨벤션 (CLAUDE.md)
$(cat "$PROJECT_ROOT/CLAUDE.md" 2>/dev/null || echo "CLAUDE.md not found")

---

# Task
$task

---

위 정보를 바탕으로 역할을 수행하고, 결과를 명확히 보고하세요.
작업 완료 후 반드시 git add && git commit을 실행하세요.
EOF
}

# 단일 Agent 백그라운드 실행
run_agent_background() {
    local agent=$1
    local task=$2
    local worktree_path="$WORKTREE_BASE/$agent"

    # Lock 획득
    if ! acquire_lock "$agent"; then
        log_error "$agent is already running (locked)"
        return 1
    fi

    # Worktree 확인
    if [ ! -d "$worktree_path" ]; then
        log_error "Worktree not found: $agent"
        log_info "Run: ./scripts/worktree-manager.sh init"
        release_lock "$agent"
        return 1
    fi

    log_info "Starting $agent in background..."

    # 백그라운드 실행
    (
        cd "$worktree_path"

        # 프롬프트 생성
        local prompt=$(generate_agent_prompt "$agent" "$task")

        # Claude Code 실행
        if command -v claude &>/dev/null; then
            echo "$prompt" | claude --print > ".agent-output.md" 2>&1
        else
            log_warn "Claude CLI not found. Saving prompt to file."
            echo "$prompt" > ".agent-prompt.md"
        fi

        # 결과 커밋
        git add -A 2>/dev/null || true
        git commit -m "[AGENT:$agent] Task completed

$task

Timestamp: $(get_iso_date)" 2>/dev/null || true

        # Lock 해제
        release_lock "$agent"

    ) &

    local pid=$!
    echo "$pid" > "$LOCK_DIR/$agent.lock"
    log_success "Started $agent (PID: $pid)"

    echo $pid
}

# 여러 Agent 병렬 실행
run_parallel() {
    local agents_to_run=("$@")
    local pids=()

    if [ ${#agents_to_run[@]} -eq 0 ]; then
        log_error "No agents specified"
        return 1
    fi

    log_info "Starting parallel execution for: ${agents_to_run[*]}"
    echo ""

    for agent in "${agents_to_run[@]}"; do
        local task="Execute your role as $agent agent"
        local pid=$(run_agent_background "$agent" "$task" 2>/dev/null)
        if [ -n "$pid" ]; then
            pids+=("$pid:$agent")
        fi
    done

    # 완료 대기
    log_info "Waiting for agents to complete..."
    echo ""

    for entry in "${pids[@]}"; do
        local pid=$(echo "$entry" | cut -d: -f1)
        local agent=$(echo "$entry" | cut -d: -f2)

        if wait "$pid" 2>/dev/null; then
            log_success "$agent completed successfully"
        else
            log_error "$agent failed or was interrupted"
        fi
    done

    echo ""
    log_success "Parallel execution completed"
}

# ============================================================
# 세션 모니터링
# ============================================================

# 실행 중인 Agent 모니터링
monitor() {
    init_dirs

    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  Parallel Agent Monitor${NC}"
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    printf "  ${BOLD}%-12s  %-12s  %-20s  %-10s${NC}\n" "Agent" "Status" "Branch" "Commits"
    echo "  ─────────────────────────────────────────────────────────────"

    for agent in "${AGENTS[@]}"; do
        local lock_file="$LOCK_DIR/$agent.lock"
        local worktree_path="$WORKTREE_BASE/$agent"
        local status="${GREEN}idle${NC}"
        local branch="N/A"
        local commits="0"

        # Lock 상태
        if [ -f "$lock_file" ]; then
            local pid=$(cat "$lock_file" 2>/dev/null)
            if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                status="${YELLOW}running${NC}"
            else
                status="${RED}stale${NC}"
            fi
        fi

        # Worktree 정보
        if [ -d "$worktree_path" ]; then
            branch=$(cd "$worktree_path" && git branch --show-current 2>/dev/null || echo "N/A")
            commits=$(cd "$worktree_path" && git rev-list main..HEAD --count 2>/dev/null || echo "0")
        fi

        printf "  %-12s  %b  %-20s  %-10s\n" \
            "$agent" "$status" "$branch" "$commits"
    done

    echo ""

    # 머지 큐 상태
    if [ -f "$SYNC_DIR/merge-queue.json" ]; then
        echo -e "${BOLD}Merge Queue:${NC}"
        jq -r '.queue[] | "  - \(.agent): \(.status) (\(.commits) commits)"' \
            "$SYNC_DIR/merge-queue.json" 2>/dev/null || echo "  (empty)"
    fi
}

# 실시간 모니터링 (watch 모드)
watch_monitor() {
    while true; do
        clear
        monitor
        echo ""
        echo -e "${CYAN}Refreshing every 5 seconds... (Ctrl+C to stop)${NC}"
        sleep 5
    done
}

# ============================================================
# 머지 큐 관리
# ============================================================

# 머지 큐에 추가
add_to_merge_queue() {
    local agent=$1
    local priority=${2:-5}

    init_dirs

    local queue_file="$SYNC_DIR/merge-queue.json"

    # 큐 파일 초기화
    if [ ! -f "$queue_file" ]; then
        echo '{"queue":[],"last_merge":null,"conflicts":[]}' > "$queue_file"
    fi

    local worktree_path="$WORKTREE_BASE/$agent"
    local commits=$(cd "$worktree_path" 2>/dev/null && git rev-list main..HEAD --count 2>/dev/null || echo "0")

    # 큐에 추가
    local tmp=$(mktemp)
    jq --arg agent "$agent" \
       --arg branch "workflow/$agent" \
       --argjson priority "$priority" \
       --arg commits "$commits" \
       --arg time "$(get_iso_date)" \
       '.queue += [{
           "agent": $agent,
           "branch": $branch,
           "priority": $priority,
           "status": "pending",
           "commits": ($commits | tonumber),
           "requested_at": $time
       }] | .queue |= sort_by(.priority)' \
       "$queue_file" > "$tmp" && mv "$tmp" "$queue_file"

    log_success "Added $agent to merge queue (priority: $priority, commits: $commits)"
}

# 머지 큐 처리
process_merge_queue() {
    local queue_file="$SYNC_DIR/merge-queue.json"

    if [ ! -f "$queue_file" ]; then
        log_info "Merge queue is empty"
        return 0
    fi

    local pending=$(jq -r '.queue | map(select(.status == "pending")) | length' "$queue_file")

    if [ "$pending" -eq 0 ]; then
        log_info "No pending merges in queue"
        return 0
    fi

    log_info "Processing $pending pending merges..."

    # 첫 번째 pending 항목 처리
    local agent=$(jq -r '.queue | map(select(.status == "pending")) | .[0].agent' "$queue_file")

    if [ -n "$agent" ] && [ "$agent" != "null" ]; then
        # 상태 업데이트
        local tmp=$(mktemp)
        jq --arg agent "$agent" \
           '.queue |= map(if .agent == $agent and .status == "pending" then .status = "merging" else . end)' \
           "$queue_file" > "$tmp" && mv "$tmp" "$queue_file"

        # 머지 실행
        "$SCRIPT_DIR/worktree-manager.sh" sync-to-main "$agent"

        # 완료 처리
        tmp=$(mktemp)
        jq --arg agent "$agent" \
           --arg time "$(get_iso_date)" \
           '.queue |= map(select(.agent != $agent or .status != "merging")) |
            .last_merge = $time' \
           "$queue_file" > "$tmp" && mv "$tmp" "$queue_file"

        log_success "Merged $agent"
    fi
}

# 머지 큐 상태 표시
show_merge_queue() {
    local queue_file="$SYNC_DIR/merge-queue.json"

    echo -e "${BOLD}Merge Queue Status:${NC}"
    echo ""

    if [ ! -f "$queue_file" ]; then
        echo "  (queue not initialized)"
        return 0
    fi

    echo -e "  ${BOLD}Last merge:${NC} $(jq -r '.last_merge // "never"' "$queue_file")"
    echo ""

    local count=$(jq -r '.queue | length' "$queue_file")
    if [ "$count" -eq 0 ]; then
        echo "  (queue is empty)"
        return 0
    fi

    printf "  ${BOLD}%-12s  %-10s  %-8s  %-20s${NC}\n" "Agent" "Status" "Commits" "Requested"
    echo "  ─────────────────────────────────────────────────────"

    jq -r '.queue[] | "\(.agent)|\(.status)|\(.commits)|\(.requested_at)"' "$queue_file" | while IFS='|' read -r agent status commits requested; do
        local status_colored="$status"
        case "$status" in
            pending) status_colored="${YELLOW}pending${NC}" ;;
            merging) status_colored="${BLUE}merging${NC}" ;;
            completed) status_colored="${GREEN}completed${NC}" ;;
            failed) status_colored="${RED}failed${NC}" ;;
        esac
        printf "  %-12s  %b  %-8s  %-20s\n" "$agent" "$status_colored" "$commits" "${requested:0:19}"
    done
}

# ============================================================
# 도움말
# ============================================================

show_help() {
    echo -e "${BOLD}Parallel Agent Runner${NC}"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  run <agents...>         Run specified agents in parallel"
    echo "  run-all                 Run all agents in parallel"
    echo "  monitor                 Show current agent status"
    echo "  watch                   Real-time monitoring (refreshes every 5s)"
    echo "  locks                   Show lock status"
    echo "  clean-locks             Remove stale locks"
    echo "  queue-add <agent> [pri] Add agent to merge queue"
    echo "  queue-process           Process pending merges"
    echo "  queue-status            Show merge queue"
    echo "  help                    Show this help"
    echo ""
    echo "Available agents: ${AGENTS[*]}"
    echo ""
    echo "Examples:"
    echo "  $0 run developer reviewer   # Run developer and reviewer in parallel"
    echo "  $0 run-all                  # Run all agents"
    echo "  $0 watch                    # Real-time monitoring"
    echo "  $0 queue-add developer 1    # Add developer to queue with priority 1"
}

# ============================================================
# 메인
# ============================================================

case "${1:-help}" in
    run)
        shift
        run_parallel "$@"
        ;;
    run-all)
        run_parallel "${AGENTS[@]}"
        ;;
    monitor|status)
        monitor
        ;;
    watch)
        watch_monitor
        ;;
    locks)
        check_locks
        ;;
    clean-locks)
        clean_stale_locks
        ;;
    queue-add)
        if [ -z "$2" ]; then
            log_error "Agent name required"
            exit 1
        fi
        add_to_merge_queue "$2" "${3:-5}"
        ;;
    queue-process)
        process_merge_queue
        ;;
    queue-status|queue)
        show_merge_queue
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
