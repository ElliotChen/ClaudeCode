#!/usr/bin/env bash
# sync-docs.sh
# 將 gitrepo/<cat>/<module>/docs/ 同步到 ~/Documents/note/aors2/<cat>/<module>/。
# 只覆蓋較新的檔案；不會刪除目的端內容。
#
# 用法：
#   ./scripts/sync-docs.sh                # 同步 modules.conf 內所有已建立 repo 的模組
#   ./scripts/sync-docs.sh <module>       # 只同步單一模組（category 由 modules.conf 查表）
#   ./scripts/sync-docs.sh --patch        # 只同步本輪 patch/ 內出現過的模組
#
# 環境變數：
#   AORS_NOTE_DIR    覆寫文件彙整目錄（預設 ~/Documents/note/aors2）
#
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

NOTE_DIR="${AORS_NOTE_DIR:-$HOME/Documents/note/aors2}"

sync_module() {
    local mod="$1"
    local cat
    cat="$(lookup_category "$mod")" || return 1

    local src="$GIT_ROOT/$cat/$mod/docs"
    local dst="$NOTE_DIR/$cat/$mod"

    [[ -d "$src" ]] || { return 0; }

    mkdir -p "$dst"
    # -a 保留屬性、--update 只覆蓋較新的檔案
    # 不使用 --delete，避免誤刪您在 note 內手動加的內容
    local out
    out="$(rsync -a --update --itemize-changes "$src/" "$dst/" | grep -v '^\.' || true)"
    if [[ -n "$out" ]]; then
        echo "==> 更新 $cat/$mod"
        echo "$out" | sed 's/^/    /'
    fi
}

list_all_registered_modules() {
    [[ -f "$MODULES_CONF" ]] || return 0
    grep -v '^[[:space:]]*#' "$MODULES_CONF" \
        | grep -v '^[[:space:]]*$' \
        | cut -d: -f1 \
        | tr -d '[:space:]'
}

ARG="${1:-}"

if [[ "$ARG" == "--patch" ]]; then
    while IFS= read -r m; do sync_module "$m"; done < <(list_patch_modules)
elif [[ -n "$ARG" ]]; then
    sync_module "$ARG"
else
    while IFS= read -r m; do
        [[ -n "$m" ]] || continue
        sync_module "$m"
    done < <(list_all_registered_modules)
fi

echo "==> 同步完成 -> $NOTE_DIR"
