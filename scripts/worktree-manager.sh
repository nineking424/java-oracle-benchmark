#!/bin/bash
# ============================================================
# Git Worktree Manager for Multi-Agent Workflow
# Agent별 독립 작업 디렉토리 관리
# ============================================================

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(dirname "$SCRIPT_DIR")
WORKFLOW_DIR="$PROJECT_ROOT/.workflow"
WORKTREE_BASE="$PROJECT_ROOT/.worktrees"
STATE_FILE="$WORKFLOW_DIR/state.json"

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

# Cross-platform ISO 날짜
get_iso_date() {
    if date -Iseconds &>/dev/null 2>&1; then
        date -Iseconds
    else
        date -u +"%Y-%m-%dT%H:%M:%SZ"
    fi
}

# Git 저장소 확인
check_git_repo() {
    if ! git rev-parse --git-dir &>/dev/null; then
        log_error "Not a git repository. Please initialize git first."
        echo "  Run: git init && git add . && git commit -m 'Initial commit'"
        exit 1
    fi
}

# ============================================================
# Worktree 관리 함수
# ============================================================

# 모든 Agent worktree 초기화
init_worktrees() {
    check_git_repo

    log_info "Initializing worktrees for all agents..."
    mkdir -p "$WORKTREE_BASE"

    # 현재 브랜치 저장
    local current_branch=$(git branch --show-current 2>/dev/null || echo "main")

    # main 브랜치 없으면 생성
    if ! git rev-parse --verify main &>/dev/null; then
        if git rev-parse --verify master &>/dev/null; then
            git branch main master
        else
            log_warn "No main or master branch found. Creating main branch."
            git checkout -b main 2>/dev/null || true
        fi
    fi

    for agent in "${AGENTS[@]}"; do
        local worktree_path="$WORKTREE_BASE/$agent"
        local branch="workflow/$agent"

        # 브랜치 없으면 생성
        if ! git rev-parse --verify "$branch" &>/dev/null; then
            git branch "$branch" main 2>/dev/null || git branch "$branch" HEAD
            log_info "Created branch: $branch"
        fi

        # Worktree 없으면 생성
        if [ ! -d "$worktree_path" ]; then
            git worktree add "$worktree_path" "$branch" 2>/dev/null || {
                log_warn "Worktree for $agent already exists or branch is checked out elsewhere"
                continue
            }
            log_success "Created worktree: $agent -> $branch"
        else
            log_info "Worktree already exists: $agent"
        fi
    done

    # 원래 브랜치로 복귀
    git checkout "$current_branch" 2>/dev/null || true

    log_success "All worktrees initialized!"
    echo ""
    list_worktrees
}

# 특정 Agent worktree 생성 (리프레시)
create_worktree() {
    local agent=$1
    local base_branch=${2:-"main"}

    check_git_repo

    if [[ ! " ${AGENTS[*]} " =~ " ${agent} " ]]; then
        log_error "Unknown agent: $agent"
        echo "Available agents: ${AGENTS[*]}"
        exit 1
    fi

    local worktree_path="$WORKTREE_BASE/$agent"
    local branch="workflow/$agent"

    log_info "Creating fresh worktree for $agent from $base_branch..."

    # 기존 worktree 제거
    if [ -d "$worktree_path" ]; then
        git worktree remove "$worktree_path" --force 2>/dev/null || {
            rm -rf "$worktree_path"
            git worktree prune
        }
        log_info "Removed existing worktree"
    fi

    # 기존 브랜치 제거 후 재생성
    git branch -D "$branch" 2>/dev/null || true
    git branch "$branch" "$base_branch"
    git worktree add "$worktree_path" "$branch"

    log_success "Created fresh worktree: $agent from $base_branch"
}

# 모든 worktree 제거
cleanup_worktrees() {
    check_git_repo

    log_warn "This will remove ALL worktrees!"
    read -p "Are you sure? (y/n) " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Cleanup cancelled"
        return 0
    fi

    log_info "Cleaning up all worktrees..."

    # Worktree 제거
    for agent in "${AGENTS[@]}"; do
        local worktree_path="$WORKTREE_BASE/$agent"
        if [ -d "$worktree_path" ]; then
            git worktree remove "$worktree_path" --force 2>/dev/null || {
                rm -rf "$worktree_path"
            }
            log_info "Removed worktree: $agent"
        fi
    done

    # Worktree 디렉토리 정리
    git worktree prune
    rm -rf "$WORKTREE_BASE"

    # 브랜치 제거 (선택적)
    read -p "Also remove workflow/* branches? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        for agent in "${AGENTS[@]}"; do
            git branch -D "workflow/$agent" 2>/dev/null || true
        done
        log_info "Removed workflow branches"
    fi

    log_success "All worktrees cleaned up!"
}

# Worktree 상태 확인
list_worktrees() {
    check_git_repo

    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  Active Worktrees${NC}"
    echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # Git worktree 정보 출력
    git worktree list 2>/dev/null | while read -r line; do
        local path=$(echo "$line" | awk '{print $1}')
        local commit=$(echo "$line" | awk '{print $2}')
        local branch=$(echo "$line" | awk '{print $3}' | tr -d '[]')

        if [[ "$path" == *".worktrees"* ]]; then
            local agent=$(basename "$path")
            local commits_ahead=$(cd "$path" 2>/dev/null && git rev-list main..HEAD --count 2>/dev/null || echo "?")
            local status="idle"
            local lock_file="$WORKFLOW_DIR/locks/$agent.lock"

            if [ -f "$lock_file" ]; then
                local pid=$(cat "$lock_file" 2>/dev/null)
                if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                    status="${GREEN}running${NC}"
                else
                    status="${YELLOW}stale${NC}"
                fi
            fi

            printf "  ${BOLD}%-12s${NC} branch: %-20s commits: %-3s status: %b\n" \
                "$agent" "$branch" "$commits_ahead" "$status"
        else
            printf "  ${BOLD}%-12s${NC} (main repository)\n" "main"
        fi
    done

    echo ""
}

# ============================================================
# 동기화 함수
# ============================================================

# Worktree에서 메인으로 변경사항 머지
sync_to_main() {
    local agent=$1
    local worktree_path="$WORKTREE_BASE/$agent"
    local branch="workflow/$agent"

    check_git_repo

    if [ ! -d "$worktree_path" ]; then
        log_error "Worktree not found: $agent"
        return 1
    fi

    # Worktree에서 커밋 확인
    local commits=$(cd "$worktree_path" && git rev-list main.."$branch" --count 2>/dev/null || echo "0")

    if [ "$commits" -gt 0 ]; then
        log_info "Syncing $commits commits from $agent to main..."

        cd "$PROJECT_ROOT"

        # 머지 충돌 체크
        if ! git merge --no-commit --no-ff "$branch" 2>/dev/null; then
            git merge --abort 2>/dev/null || true
            log_error "Merge conflicts detected! Please resolve manually."
            return 1
        fi
        git merge --abort 2>/dev/null || true

        # 실제 머지 실행
        git merge "$branch" --no-ff -m "[SYNC] Merge $agent work

Agent: $agent
Commits: $commits
Timestamp: $(get_iso_date)"

        log_success "Synced $commits commits from $agent to main"
    else
        log_info "No new commits to sync from $agent"
    fi

    cd "$PROJECT_ROOT"
}

# 메인에서 Worktree로 업데이트 (리베이스)
sync_from_main() {
    local agent=$1
    local worktree_path="$WORKTREE_BASE/$agent"

    check_git_repo

    if [ ! -d "$worktree_path" ]; then
        log_error "Worktree not found: $agent"
        return 1
    fi

    log_info "Syncing main -> $agent..."

    cd "$worktree_path"

    # 현재 작업 저장
    local stashed=false
    if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
        git stash push -m "auto-stash before sync $(date +%Y%m%d%H%M%S)" 2>/dev/null && stashed=true
        log_info "Stashed uncommitted changes"
    fi

    # 메인에서 리베이스
    if git rebase main; then
        log_success "Rebased on main successfully"
    else
        log_error "Rebase failed! Aborting..."
        git rebase --abort 2>/dev/null || true
        # Stash 복원
        if [ "$stashed" = true ]; then
            git stash pop 2>/dev/null || true
        fi
        cd "$PROJECT_ROOT"
        return 1
    fi

    # 저장된 작업 복원
    if [ "$stashed" = true ]; then
        git stash pop 2>/dev/null || {
            log_warn "Could not restore stashed changes. Check: git stash list"
        }
    fi

    cd "$PROJECT_ROOT"
    log_success "Synced main -> $agent"
}

# 모든 Worktree 동기화 (to main)
sync_all_to_main() {
    check_git_repo

    log_info "Syncing all worktrees to main..."

    for agent in "${AGENTS[@]}"; do
        if [ -d "$WORKTREE_BASE/$agent" ]; then
            sync_to_main "$agent" || {
                log_warn "Failed to sync $agent, continuing..."
            }
        fi
    done

    log_success "All worktrees synced to main!"
}

# 모든 Worktree 업데이트 (from main)
sync_all_from_main() {
    check_git_repo

    log_info "Updating all worktrees from main..."

    for agent in "${AGENTS[@]}"; do
        if [ -d "$WORKTREE_BASE/$agent" ]; then
            sync_from_main "$agent" || {
                log_warn "Failed to update $agent, continuing..."
            }
        fi
    done

    log_success "All worktrees updated from main!"
}

# ============================================================
# 도움말
# ============================================================

show_help() {
    echo -e "${BOLD}Git Worktree Manager for Multi-Agent Workflow${NC}"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  init                    Initialize worktrees for all agents"
    echo "  create <agent> [branch] Create/refresh worktree for specific agent"
    echo "  cleanup                 Remove all worktrees"
    echo "  list                    List all worktrees and their status"
    echo "  sync-to-main <agent>    Merge agent's work to main"
    echo "  sync-from-main <agent>  Update agent's worktree from main"
    echo "  sync-all                Sync all worktrees to main"
    echo "  update-all              Update all worktrees from main"
    echo "  help                    Show this help"
    echo ""
    echo "Available agents: ${AGENTS[*]}"
    echo ""
    echo "Examples:"
    echo "  $0 init                     # Initialize all worktrees"
    echo "  $0 create developer main    # Refresh developer worktree from main"
    echo "  $0 sync-to-main developer   # Merge developer's work to main"
    echo "  $0 list                     # Show worktree status"
}

# ============================================================
# 메인
# ============================================================

case "${1:-help}" in
    init)
        init_worktrees
        ;;
    create)
        if [ -z "$2" ]; then
            log_error "Agent name required"
            echo "Usage: $0 create <agent> [base_branch]"
            exit 1
        fi
        create_worktree "$2" "${3:-main}"
        ;;
    cleanup)
        cleanup_worktrees
        ;;
    list|status)
        list_worktrees
        ;;
    sync-to-main)
        if [ -z "$2" ]; then
            log_error "Agent name required"
            exit 1
        fi
        sync_to_main "$2"
        ;;
    sync-from-main)
        if [ -z "$2" ]; then
            log_error "Agent name required"
            exit 1
        fi
        sync_from_main "$2"
        ;;
    sync-all)
        sync_all_to_main
        ;;
    update-all)
        sync_all_from_main
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
