#!/bin/bash
# 워크플로우 상태 확인 단축 스크립트
set -e
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
"$SCRIPT_DIR/workflow.sh" status
