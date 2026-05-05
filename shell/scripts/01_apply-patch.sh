#!/usr/bin/env bash
# apply-patch.sh
# 將 patch/ 下的模組套用到 gitrepo/<category>/<module>/，並以 release/<version> 提交。
# category 由 modules.conf 查表決定。
#
# 用法：
#   ./scripts/apply-patch.sh <version>            # 套用 patch/ 下所有模組
#   ./scripts/apply-patch.sh <version> <module>   # 只套用單一模組
#
# 範例：
#   ./scripts/apply-patch.sh 1.6.10
#   ./scripts/apply-patch.sh 1.6.10 biz-management
#
# 保留規則：
#   - 永遠保留：.git/、docs/、.aorskeep
#   - 額外保留：模組根目錄 .aorskeep 中列出的 rsync pattern
#
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

VERSION="${1:?version required, e.g. 1.6.10}"
ONLY_MODULE="${2:-}"

apply_one() {
    local mod="$1" version="$2"
    local cat
    cat="$(lookup_category "$mod")" || return 1

    local patch_dir="$ROOT/patch/$mod"
    local repo_dir="$GIT_ROOT/$cat/$mod"

    echo "[DEBUG] ----- 01 $repo_dir"

    [[ -d "$patch_dir" ]] || { echo "  跳過 $mod（patch/$mod 不存在）"; return 0; }
    [[ -d "$repo_dir" ]] || { echo "ERROR: repo 不存在：$repo_dir" >&2; return 1; }
    [[ -d "$repo_dir/.git" ]] || { echo "ERROR: 不是 git repo：$repo_dir" >&2; return 1; }

    local excludes=(
        --exclude='.git/'
        --exclude='docs/'
        --exclude='.aorskeep'
    )
    local keepfile="$repo_dir/.aorskeep"
    [[ -f "$keepfile" ]] && excludes+=(--exclude-from="$keepfile")

    echo "==> [$cat/$mod] 套用 patch"
    rsync -a --delete "${excludes[@]}" "$patch_dir/" "$repo_dir/"

    (
        cd "$repo_dir"
        local branch
        branch="$(git rev-parse --abbrev-ref HEAD)"
        if [[ "$branch" != "main" && "$branch" != "master" ]]; then
            echo "  WARN: 目前不在 main/master，而是 $branch" >&2
        fi
        if git status --porcelain | grep -q .; then
            git add -A ':!docs/' ##排除docs
            git commit -m "$version" >/dev/null
            echo "  已提交：$version"
        else
            echo "  無變更，略過提交"
        fi
    )
}

if [[ -n "$ONLY_MODULE" ]]; then
    apply_one "$ONLY_MODULE" "$VERSION"
    exit 0
fi

mods=()
while IFS= read -r m; do mods+=("$m"); done < <(list_patch_modules)

if [[ ${#mods[@]} -eq 0 ]]; then
    echo "patch/ 內沒有任何模組目錄，結束"
    exit 0
fi

for m in "${mods[@]}"; do
    apply_one "$m" "$VERSION"
done
