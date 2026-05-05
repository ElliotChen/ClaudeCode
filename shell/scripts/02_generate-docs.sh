#!/usr/bin/env bash
# generate-docs.sh
# 對指定模組（或 patch/ 下所有模組）依序呼叫 Claude Code skill 產生文件。
# category 由 modules.conf 查表決定。
#
# 用法：
#   ./scripts/generate-docs.sh <version>            # 對 patch/ 下所有模組執行
#   ./scripts/generate-docs.sh <version> <module>   # 只跑單一模組
#
# Skill 清單以陣列方式維護（見下方 SKILLS），按順序執行。
#
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

# ── 解析選項 ──────────────────────────────────────────────
JOBS=1
while [[ "${1:-}" == -* ]]; do
    case "$1" in
        -j|--jobs)
            JOBS="${2:?--jobs 需要參數}"
            shift 2
            ;;
        -j*)        JOBS="${1#-j}"; shift ;;
        --jobs=*)   JOBS="${1#--jobs=}"; shift ;;
        -h|--help)
            sed -n '2,/^set -/p' "$0" | sed -E 's/^# ?//; /^set -/d'
            exit 0
            ;;
        *)
            echo "ERROR: 未知選項 $1" >&2
            exit 1
            ;;
    esac
done

VERSION="${1:?version required}"
ONLY_MODULE="${2:-}"

[[ "$JOBS" =~ ^[1-9][0-9]*$ ]] || { echo "ERROR: -j 須為正整數（got: $JOBS）" >&2; exit 1; }

# 要執行的 skill 清單（按順序）
SKILLS=(
    "commit-doc-generator"
)
#    "error-doc-generator"
#    "schedule-doc-generator"
#)

LOG_DIR="$ROOT/logs/generate-docs/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$LOG_DIR"

# ── 核心：對單一模組跑所有 skill ─────────────────────────
run_skills_for() {
    local mod="$1" version="$2"
    local cat
    cat="$(lookup_category "$mod")" || return 1
    local repo_dir="$GIT_ROOT/$cat/$mod"

    echo "[DEBUG] ----- 02 $repo_dir"
    [[ -d "$repo_dir" ]] || { echo "  跳過 $mod（repo 不存在）"; return 0; }

    cd "$repo_dir"
    local skill
    for skill in "${SKILLS[@]}"; do
        echo "[$mod] skill: $skill"
        qwen -p "/$skill 直接執行不要詢問確認，資料若有多筆，則以最新的一筆進行；version=$version"
    done
    echo "[$mod] done"

}

# 包裝給 xargs 子 shell 呼叫；負責 log redirect 與摘要輸出
process_one() {
    local mod="$1"
    # 在子 shell 還原 SKILLS 陣列（export -f 不會帶陣列）
    # shellcheck disable=SC2206
    SKILLS=( $SKILLS_STR )
    if run_skills_for "$mod" "$VERSION" >"$LOG_DIR/$mod.log" 2>&1; then
        echo "[ok]   $mod"
    else
        echo "[FAIL] $mod  (見 $LOG_DIR/$mod.log)"
        return 1
    fi
}

export ROOT MODULES_CONF VERSION LOG_DIR
export SKILLS_STR="${SKILLS[*]}"
export -f lookup_category run_skills_for process_one

if [[ -n "$ONLY_MODULE" ]]; then
    run_skills_for "$ONLY_MODULE" "$VERSION"
    exit 0
fi

# ── 收集模組清單 ─────────────────────────────────────────
mods=()
if [[ -n "$ONLY_MODULE" ]]; then
    mods=("$ONLY_MODULE")
else
    while IFS= read -r m; do mods+=("$m"); done < <(list_patch_modules)
fi

if [[ ${#mods[@]} -eq 0 ]]; then
    echo "沒有要處理的模組"
    exit 0
fi

echo "==> 將處理 ${#mods[@]} 個模組，併發 $JOBS"
echo "==> log 目錄：$LOG_DIR"
echo

# ── 執行 ──────────────────────────────────────────────────
echo "[DEBUG] -- 02 JOBS[${JOBS}]"
fail=0
if (( JOBS == 1 )); then
    # 序列模式：直接輸出 + tee 到 log
    for m in "${mods[@]}"; do
        if ! run_skills_for "$m" "$VERSION" 2>&1 | tee "$LOG_DIR/$m.log"; then
            fail=$((fail + 1))
        fi
    done
else
    # 併發模式：xargs -P，每個模組輸出寫到自己的 log
    set +e
    printf '%s\n' "${mods[@]}" \
        | xargs -n1 -P"$JOBS" -I{} bash -c 'process_one "$1"' _ {}
    rc=$?
    set -e
    [[ $rc -ne 0 ]] && fail=1
fi

echo
echo "==> log 目錄：$LOG_DIR"
if (( fail > 0 )); then
    echo "==> 完成，但有失敗的模組（請檢查上方 [FAIL] 標記與 log）"
    exit 1
fi
echo "==> 全部模組完成"
