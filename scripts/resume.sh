#!/bin/bash
# 워크플로우 재개 단축 스크립트
set -e
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
"$SCRIPT_DIR/workflow.sh" resume
