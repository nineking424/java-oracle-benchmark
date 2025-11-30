#!/bin/bash
# ============================================================
# Parallel Claude Code Session Starter
# 여러 터미널/tmux 창에서 Claude Code 세션 시작
# ============================================================

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(dirname "$SCRIPT_DIR")
WORKTREE_BASE="$PROJECT_ROOT/.worktrees"

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

# Worktree 존재 확인
check_worktrees() {
    local missing=()

    for agent in "${AGENTS[@]}"; do
        if [ ! -d "$WORKTREE_BASE/$agent" ]; then
            missing+=("$agent")
        fi
    done

    if [ ${#missing[@]} -gt 0 ]; then
        log_error "Missing worktrees: ${missing[*]}"
        echo ""
        echo "Initialize worktrees first:"
        echo "  ./scripts/worktree-manager.sh init"
        exit 1
    fi
}

# ============================================================
# tmux 기반 병렬 세션
# ============================================================

start_tmux() {
    local session_name="${1:-workflow}"
    local agents_to_start=("${@:2}")

    # 기본값: 모든 agent
    if [ ${#agents_to_start[@]} -eq 0 ]; then
        agents_to_start=("${AGENTS[@]}")
    fi

    # tmux 설치 확인
    if ! command -v tmux &>/dev/null; then
        log_error "tmux is not installed"
        echo "Install with: brew install tmux"
        exit 1
    fi

    check_worktrees

    # 기존 세션 확인
    if tmux has-session -t "$session_name" 2>/dev/null; then
        log_warn "Session '$session_name' already exists"
        read -p "Kill existing session? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            tmux kill-session -t "$session_name"
        else
            echo "Attach with: tmux attach -t $session_name"
            exit 0
        fi
    fi

    log_info "Creating tmux session: $session_name"

    # 메인 창 (Orchestrator)
    tmux new-session -d -s "$session_name" -n "main" -c "$PROJECT_ROOT"
    tmux send-keys -t "$session_name:main" "echo '🎯 Orchestrator - Main Repository'" Enter
    tmux send-keys -t "$session_name:main" "echo 'Run: claude to start'" Enter

    # Agent 창 생성
    for agent in "${agents_to_start[@]}"; do
        local worktree_path="$WORKTREE_BASE/$agent"

        if [ -d "$worktree_path" ]; then
            tmux new-window -t "$session_name" -n "$agent" -c "$worktree_path"
            tmux send-keys -t "$session_name:$agent" "echo '🤖 $agent Agent Worktree'" Enter
            tmux send-keys -t "$session_name:$agent" "echo 'Branch: workflow/$agent'" Enter
            tmux send-keys -t "$session_name:$agent" "echo 'Run: claude to start'" Enter
            log_info "Created window: $agent"
        fi
    done

    # 상태 모니터 창
    tmux new-window -t "$session_name" -n "monitor" -c "$PROJECT_ROOT"
    tmux send-keys -t "$session_name:monitor" "./scripts/parallel-runner.sh watch" Enter

    # 메인 창으로 이동
    tmux select-window -t "$session_name:main"

    log_success "tmux session created: $session_name"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Windows:"
    echo "    - main      : Orchestrator (main repository)"
    for agent in "${agents_to_start[@]}"; do
        echo "    - $agent"
    done
    echo "    - monitor   : Real-time status monitor"
    echo ""
    echo "  Attach with:"
    echo "    tmux attach -t $session_name"
    echo ""
    echo "  Navigation:"
    echo "    Ctrl+b n    : Next window"
    echo "    Ctrl+b p    : Previous window"
    echo "    Ctrl+b 0-9  : Go to window number"
    echo "    Ctrl+b d    : Detach session"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # 자동 attach
    read -p "Attach now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        tmux attach -t "$session_name"
    fi
}

# tmux 세션 종료
stop_tmux() {
    local session_name="${1:-workflow}"

    if tmux has-session -t "$session_name" 2>/dev/null; then
        tmux kill-session -t "$session_name"
        log_success "Killed tmux session: $session_name"
    else
        log_info "Session '$session_name' not found"
    fi
}

# ============================================================
# macOS Terminal.app 기반 병렬 세션
# ============================================================

start_terminal_app() {
    local agents_to_start=("$@")

    # 기본값: 모든 agent
    if [ ${#agents_to_start[@]} -eq 0 ]; then
        agents_to_start=("${AGENTS[@]}")
    fi

    # macOS 확인
    if [[ "$(uname)" != "Darwin" ]]; then
        log_error "This option is only available on macOS"
        exit 1
    fi

    check_worktrees

    log_info "Opening Terminal.app windows..."

    # 메인 저장소
    osascript <<EOF
tell application "Terminal"
    activate
    do script "cd '$PROJECT_ROOT' && echo '🎯 Orchestrator - Main Repository' && echo 'Run: claude'"
end tell
EOF

    # Agent 터미널
    for agent in "${agents_to_start[@]}"; do
        local worktree_path="$WORKTREE_BASE/$agent"

        if [ -d "$worktree_path" ]; then
            osascript <<EOF
tell application "Terminal"
    do script "cd '$worktree_path' && echo '🤖 $agent Agent Worktree' && echo 'Branch: workflow/$agent' && echo 'Run: claude'"
end tell
EOF
            log_info "Opened terminal: $agent"
            sleep 0.5  # 창이 너무 빨리 열리지 않도록
        fi
    done

    log_success "Opened ${#agents_to_start[@]} Terminal windows"
}

# ============================================================
# iTerm2 기반 병렬 세션
# ============================================================

start_iterm() {
    local agents_to_start=("$@")

    # 기본값: 모든 agent
    if [ ${#agents_to_start[@]} -eq 0 ]; then
        agents_to_start=("${AGENTS[@]}")
    fi

    # macOS 확인
    if [[ "$(uname)" != "Darwin" ]]; then
        log_error "This option is only available on macOS"
        exit 1
    fi

    # iTerm2 확인
    if ! osascript -e 'tell application "System Events" to get name of processes' 2>/dev/null | grep -q "iTerm"; then
        if [ ! -d "/Applications/iTerm.app" ]; then
            log_warn "iTerm2 not found, using Terminal.app instead"
            start_terminal_app "${agents_to_start[@]}"
            return
        fi
    fi

    check_worktrees

    log_info "Opening iTerm2 tabs..."

    # iTerm2 스크립트
    osascript <<EOF
tell application "iTerm"
    activate

    -- 새 창 생성
    set newWindow to (create window with default profile)

    tell current session of newWindow
        write text "cd '$PROJECT_ROOT' && echo '🎯 Orchestrator - Main Repository'"
    end tell

EOF

    # Agent 탭 생성
    for agent in "${agents_to_start[@]}"; do
        local worktree_path="$WORKTREE_BASE/$agent"

        if [ -d "$worktree_path" ]; then
            osascript <<EOF
tell application "iTerm"
    tell current window
        set newTab to (create tab with default profile)
        tell current session of newTab
            write text "cd '$worktree_path' && echo '🤖 $agent Agent Worktree'"
        end tell
    end tell
end tell
EOF
            log_info "Created tab: $agent"
        fi
    done

    log_success "Opened iTerm2 with ${#agents_to_start[@]} tabs"
}

# ============================================================
# VSCode 터미널 가이드
# ============================================================

show_vscode_guide() {
    check_worktrees

    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  VSCode Multi-Terminal Setup Guide${NC}"
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "Open multiple integrated terminals in VSCode:"
    echo ""
    echo "1. Split Terminal: Ctrl+Shift+5 (or Cmd+Shift+5 on Mac)"
    echo ""
    echo "2. Run these commands in separate terminals:"
    echo ""
    echo -e "   ${BOLD}Terminal 1 (Orchestrator):${NC}"
    echo "   cd $PROJECT_ROOT"
    echo "   claude"
    echo ""

    for agent in "${AGENTS[@]}"; do
        local worktree_path="$WORKTREE_BASE/$agent"
        echo -e "   ${BOLD}Terminal ($agent):${NC}"
        echo "   cd $worktree_path"
        echo "   claude"
        echo ""
    done

    echo "3. Or use the terminal layout file:"
    echo "   .vscode/terminals.json (if available)"
    echo ""
}

# ============================================================
# 빠른 시작 (추천 방법 자동 선택)
# ============================================================

quick_start() {
    check_worktrees

    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  Quick Start - Parallel Claude Code Sessions${NC}"
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # 환경 감지
    local method="tmux"

    if command -v tmux &>/dev/null; then
        method="tmux"
    elif [[ "$(uname)" == "Darwin" ]]; then
        if [ -d "/Applications/iTerm.app" ]; then
            method="iterm"
        else
            method="terminal"
        fi
    fi

    echo "Detected environment: $method"
    echo ""
    echo "Available methods:"
    echo "  1) tmux      - Recommended for terminal users"
    echo "  2) terminal  - macOS Terminal.app"
    echo "  3) iterm     - macOS iTerm2"
    echo "  4) vscode    - Show VSCode guide"
    echo "  5) manual    - Show manual commands"
    echo ""

    read -p "Select method (1-5) [default: 1]: " choice
    choice=${choice:-1}

    case "$choice" in
        1) start_tmux "workflow" ;;
        2) start_terminal_app ;;
        3) start_iterm ;;
        4) show_vscode_guide ;;
        5) show_manual_commands ;;
        *) log_error "Invalid choice"; exit 1 ;;
    esac
}

# 수동 명령어 안내
show_manual_commands() {
    check_worktrees

    echo -e "${BOLD}Manual Commands for Each Agent:${NC}"
    echo ""
    echo "Open separate terminal windows and run:"
    echo ""
    echo -e "${BOLD}Orchestrator (Main):${NC}"
    echo "  cd $PROJECT_ROOT && claude"
    echo ""

    for agent in "${AGENTS[@]}"; do
        echo -e "${BOLD}$agent Agent:${NC}"
        echo "  cd $WORKTREE_BASE/$agent && claude"
        echo ""
    done
}

# ============================================================
# 도움말
# ============================================================

show_help() {
    echo -e "${BOLD}Parallel Claude Code Session Starter${NC}"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  quick                   Auto-detect and start (recommended)"
    echo "  tmux [session] [agents] Start tmux session"
    echo "  stop-tmux [session]     Stop tmux session"
    echo "  terminal [agents]       Open macOS Terminal.app windows"
    echo "  iterm [agents]          Open iTerm2 tabs"
    echo "  vscode                  Show VSCode setup guide"
    echo "  manual                  Show manual commands"
    echo "  help                    Show this help"
    echo ""
    echo "Available agents: ${AGENTS[*]}"
    echo ""
    echo "Examples:"
    echo "  $0 quick                        # Auto-detect best method"
    echo "  $0 tmux workflow                # Start tmux with all agents"
    echo "  $0 tmux dev developer reviewer  # Start with specific agents"
    echo "  $0 terminal developer qa        # Open Terminal.app for specific agents"
}

# ============================================================
# 메인
# ============================================================

case "${1:-quick}" in
    quick|start)
        quick_start
        ;;
    tmux)
        shift
        start_tmux "$@"
        ;;
    stop-tmux|stop)
        stop_tmux "${2:-workflow}"
        ;;
    terminal|term)
        shift
        start_terminal_app "$@"
        ;;
    iterm)
        shift
        start_iterm "$@"
        ;;
    vscode|code)
        show_vscode_guide
        ;;
    manual)
        show_manual_commands
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
