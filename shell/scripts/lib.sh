#!/usr/bin/env bash
# lib.sh — 共用函式（被其他 *.sh source 進來使用）

# ROOT = workspace 根（scripts/ 的上一層）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GIT_ROOT="$HOME/gitrepo/aors2"
MODULES_CONF="${MODULES_CONF:-$ROOT/scripts/modules.conf}"

if [ -f ~/shell/claudeCodeModels.sh ]; then source ~/shell/claudeCodeModels.sh; fi

# 從 modules.conf 查 module → category。
# 用法：cat="$(lookup_category biz-management)"
lookup_category() {
    local mod="$1"
    [[ -f "$MODULES_CONF" ]] || {
        echo "ERROR: 找不到設定檔 $MODULES_CONF" >&2
        return 1
    }
    local line
    line="$(
        grep -v '^[[:space:]]*#' "$MODULES_CONF" \
            | grep -v '^[[:space:]]*$' \
            | grep -E "^[[:space:]]*${mod}[[:space:]]*:" \
            | head -n 1 \
            || true
    )"
    if [[ -z "$line" ]]; then
        echo "ERROR: 模組 '$mod' 未在 $MODULES_CONF 註冊" >&2
        return 1
    fi
    local cat
    cat="$(echo "$line" | cut -d: -f2 | tr -d '[:space:]')"
    case "$cat" in
        infra|ams|dms|web) echo "$cat" ;;
        *) echo "ERROR: 模組 '$mod' 的 category '$cat' 不合法（須為 infra/ams/dms/web）" >&2; return 1 ;;
    esac
}

# 列出 patch/ 下所有模組目錄名稱（一行一個）。
list_patch_modules() {
    local patch_root="$ROOT/patch"
    ##echo "[DEBUG] ------ $patch_root"
    [[ -d "$patch_root" ]] || return 0
    shopt -s nullglob
    local d
    for d in "$patch_root"/*/; do
        [[ -d "$d" ]] || continue
        basename "$d"
    done
    shopt -u nullglob
}
