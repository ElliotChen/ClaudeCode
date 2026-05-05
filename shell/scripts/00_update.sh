#!/usr/bin/env bash
# update.sh
# 一輪批次更新：套 patch -> commit -> 跑文件 skill -> 同步到 note。
# 本輪所有模組共用同一個 release/<version>。
#
# 用法：
#   ./scripts/update.sh <version>
# 範例：
#   ./scripts/update.sh 1.6.10
#
# 流程：
#   1. 掃描 patch/ 下所有模組目錄
#   2. 由 modules.conf 查每個模組的 category
#   3. 列出本輪要處理的模組，請使用者確認
#   4. 依序套 patch、commit、跑 skill、同步 docs
#   5. 詢問是否清除 patch/ 內已處理的模組
#
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
source "$DIR/lib.sh"

JOBS=1
while [[ "${1:-}" == -* ]]; do
    case "$1" in
        -j|--jobs)  JOBS="${2:?--jobs 需要參數}"; shift 2 ;;
        -j*)        JOBS="${1#-j}"; shift ;;
        --jobs=*)   JOBS="${1#--jobs=}"; shift ;;
        *) echo "ERROR: 未知選項 $1" >&2; exit 1 ;;
    esac
done

VERSION="${1:?version required, e.g. 1.6.10}"

mods=()
while IFS= read -r m; do mods+=("$m"); done < <(list_patch_modules)

if [[ ${#mods[@]} -eq 0 ]]; then
    echo "patch/ 內沒有任何模組目錄，結束"
    exit 0
fi

echo "============================================================"
echo "本輪更新 (release/$VERSION)"
echo "============================================================"

# 預檢：所有模組都要能在 modules.conf 查到 category
missing=()
for m in "${mods[@]}"; do
    echo "[DEBUG] -- 00_1 find mod [$m]"
    echo "[DEBUG] -- 00_2 and check category is $(lookup_category "$m")"
    if ! cat="$(lookup_category "$m" 2>/dev/null)"; then
        missing+=("$m")
    else
        printf "  %-8s %s\n" "$cat" "$m"
    fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
    echo
    echo "ERROR: 下列模組未在 $MODULES_CONF 註冊：" >&2
    for m in "${missing[@]}"; do echo "  - $m" >&2; done
    echo "請先補進 modules.conf 再重跑。" >&2
    exit 1
fi

echo
read -r -p "繼續執行？[y/N] " ans
[[ "$ans" =~ ^[Yy]$ ]] || { echo "已取消"; exit 0; }

echo
echo "------------------------------------------------------------"
echo "[1/3] 套用 patch 並 commit"
echo "------------------------------------------------------------"
"$DIR/01_apply-patch.sh" "$VERSION"

echo
echo "------------------------------------------------------------"
echo "[2/3] 產生文件（呼叫 Claude Code skill）"
echo "------------------------------------------------------------"
"$DIR/02_generate-docs.sh" -j "$JOBS" "$VERSION"

echo
echo "------------------------------------------------------------"
echo "[3/3] 同步文件到 note"
echo "------------------------------------------------------------"
"$DIR/03_sync-docs.sh" --patch

echo
echo "==> 本輪完成：release/" + ${VERSION} + "，共 ${#mods[@]} 個模組"

echo
read -r -p "清除 patch/ 內這 ${#mods[@]} 個已處理的模組？[y/N] " ans
if [[ "$ans" =~ ^[Yy]$ ]]; then
    for m in "${mods[@]}"; do
        rm -rf "$ROOT/patch/$m"
    done
    echo "已清除"
fi
