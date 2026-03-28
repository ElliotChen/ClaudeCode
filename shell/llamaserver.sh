#!/opt/homebrew/bin/bash
#
# llama-server.sh — llama-server 管理工具
# 同一時間僅允許一個 model 運行
#
set -euo pipefail

## Default variables
TODAY=$(date +'%Y%m%d')

# ─────────────────────────────────────────────
# 設定區：依實際環境修改
# ─────────────────────────────────────────────
LLAMA_SERVER_BIN="${LLAMA_SERVER_BIN:-llama-server}"
MODEL_DIR="${LLAMA_MODEL_DIR:-$HOME/llm/gguf}"
MODEL_LOG_DIR="${LLAMA_MODEL_LOG_DIR:-$HOME/llm/logs}"
DEFAULT_MODEL="qwen3"
HOST="${LLAMA_HOST:-127.0.0.1}"
PORT="${LLAMA_PORT:-7080}"

# ─────────────────────────────────────────────
# Model 對照表：名稱 → 實際檔案 (GGUF)
# 請依自己下載的檔案調整
# ─────────────────────────────────────────────
declare -A MODEL_MAP=(
  [qwen3]="/qwen/Qwen_Qwen3.5-35B-A3B-Q6_K_L.gguf"
  [qwen3_9b]="/qwen/jackrong/9b/Qwen3.5-9B.Q8_0.gguf"
  [gptoss]="/gpt/oss-20b/gpt-oss-20b-mxfp4.gguf"
)

# ─────────────────────────────────────────────
# 多模態Model 對照表：名稱 → 實際檔案 (GGUF)
# 請依自己下載的檔案調整
# ─────────────────────────────────────────────
declare -A MMPROJ_MAP=(
  [qwen3]="/qwen/mmproj-Qwen_Qwen3.5-35B-A3B-bf16.gguf"
  [qwen3_9b]="/qwen/jackrong/9b/mmproj-BF16.gguf"
)

# ─────────────────────────────────────────────
# 顏色
# ─────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ─────────────────────────────────────────────
# 函式
# ─────────────────────────────────────────────

_log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
_log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
_log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

_get_pid() {
  pgrep -f "${LLAMA_SERVER_BIN}" 2>/dev/null || true
}

_list_models() {
  echo -e "${CYAN}可用 model 清單：${NC}"
  for key in "${!MODEL_MAP[@]}"; do
    local mark=""
    [[ "$key" == "$DEFAULT_MODEL" ]] && mark=" (預設)"
    echo "  • ${key}${mark}  →  ${MODEL_MAP[$key]}"
  done
}

_resolve_model_path() {
  local name="$1"
  local file="${MODEL_MAP[$name]:-}"
  if [[ -z "$file" ]]; then
    _log_error "未知的 model: ${name}"
    _list_models
    exit 1
  fi
  local path="${MODEL_DIR}${file}"
  if [[ ! -f "$path" ]]; then
    _log_error "模型檔案不存在: ${path}"
    _log_warn "請確認 LLAMA_MODEL_DIR 及檔名是否正確"
    exit 1
  fi
  echo "$path"
}

_resolve_mmproj_path() {
  local name="$1"
  local file="${MMPROJ_MAP[$name]:-}"
  local path=""
  if [[ -n "$file" ]]; then
  	path="${MODEL_DIR}${file}"
  fi
  echo "$path"
}

do_start() {
  local model_name="${1:-$DEFAULT_MODEL}"
  local pid
  pid=$(_get_pid)

  if [[ -n "$pid" ]]; then
    _log_error "llama-server 已在運行中 (PID: ${pid})"
    _log_warn "請先執行 stop 再啟動新的 model"
    exit 1
  fi

  local model_path
  model_path=$(_resolve_model_path "$model_name")

  local mmproj_path
  mmproj_path=$(_resolve_mmproj_path "$model_name")

  _log_info "啟動 llama-server ..."
  _log_info "Model : ${model_name} (${model_path}) (${mmproj_path})"
  _log_info "Listen: ${HOST}:${PORT}"

  local args=(
  	--model "$model_path"
  	--host "$HOST"
  	--port "$PORT"
  	--log-file "${MODEL_LOG_DIR}/${TODAY}.log"
  )

  if [[ -n "$mmproj_path" ]]; then
  	args+=(--mmproj "$mmproj_path")
  fi

  nohup "${LLAMA_SERVER_BIN}" "${args[@]}" > /dev/null 2>&1 &
 

  sleep 1
  pid=$(_get_pid)
  if [[ -n "$pid" ]]; then
    _log_info "啟動成功 (PID: ${pid})"
    _log_info "Log: ${MODEL_LOG_DIR}/${TODAY}.log"
  else
    _log_error "啟動失敗，請檢查 ${MODEL_LOG_DIR}/${TODAY}.log"
    exit 1
  fi
}

do_stop() {
  local pid
  pid=$(_get_pid)

  if [[ -z "$pid" ]]; then
    _log_warn "llama-server 未在運行"
    return 0
  fi

  _log_info "正在停止 llama-server (PID: ${pid}) ..."
  kill "$pid"

  # 等待結束，最多 10 秒
  local count=0
  while kill -0 "$pid" 2>/dev/null; do
    sleep 1
    count=$((count + 1))
    if [[ $count -ge 10 ]]; then
      _log_warn "graceful 超時，送 SIGKILL ..."
      kill -9 "$pid" 2>/dev/null || true
      break
    fi
  done

  _log_info "已停止"
}

do_restart() {
  local model_name="${1:-}"
  do_stop
  sleep 1
  do_start "$model_name"
}

do_status() {
  local pid
  pid=$(_get_pid)

  if [[ -n "$pid" ]]; then
    _log_info "llama-server 運行中 (PID: ${pid})"
    # 嘗試顯示正在使用的 model
    local cmdline
    cmdline=$(ps -p "$pid" -o args= 2>/dev/null || true)
    if [[ -n "$cmdline" ]]; then
      local model_file
      model_file=$(echo "$cmdline" | grep -o '\-\-model\s\S*' || true)
      [[ -n "$model_file" ]] && _log_info "Model : $model_file"
    fi
  else
    _log_warn "llama-server 未在運行"
  fi
}

do_logs() {
  if [[ -f ${MODEL_LOG_DIR}/${TODAY}.log ]]; then
    tail -f ${MODEL_LOG_DIR}/${TODAY}.log
  else
    _log_warn "尚無 log 檔案"
  fi
}

do_help() {
  cat <<EOF

${CYAN}llama-server.sh${NC} — llama-server 管理工具

${CYAN}用法：${NC}
  $(basename "$0") <command> [model]

${CYAN}指令：${NC}
  start [model]    啟動 llama-server，預設使用 ${DEFAULT_MODEL}
  stop             停止運行中的 llama-server (pgrep + kill)
  restart [model]  重啟 (stop → start)，可同時切換 model
  status           查看 llama-server 運行狀態
  list             列出所有可用的 model
  logs             即時查看 log (tail -f)
  help             顯示此說明

${CYAN}範例：${NC}
  $(basename "$0") start              # 啟動預設 model (${DEFAULT_MODEL})
  $(basename "$0") start mistral      # 啟動 mistral
  $(basename "$0") restart gptoss     # 切換到 gptoss
  $(basename "$0") stop               # 停止
  $(basename "$0") status             # 查看狀態

${CYAN}環境變數（可覆蓋預設值）：${NC}
  LLAMA_SERVER_BIN   llama-server 執行檔路徑  (預設: llama-server)
  LLAMA_MODEL_DIR    模型檔案目錄              (預設: \$HOME/models)
  LLAMA_HOST         監聽位址                  (預設: 127.0.0.1)
  LLAMA_PORT         監聽埠號                  (預設: 8080)
  LLAMA_CTX_SIZE     Context size              (預設: 4096)
  LLAMA_GPU_LAYERS   GPU offload layers        (預設: 99)

${CYAN}新增 model：${NC}
  編輯腳本中的 MODEL_MAP，加入 [名稱]="檔案名.gguf" 即可。

EOF
}

# ─────────────────────────────────────────────
# 主程式
# ─────────────────────────────────────────────
case "${1:-help}" in
  start)    do_start "${2:-}" ;;
  stop)     do_stop ;;
  restart)  do_restart "${2:-}" ;;
  status)   do_status ;;
  list)     _list_models ;;
  logs)     do_logs ;;
  help|-h|--help) do_help ;;
  *)
    _log_error "未知指令: $1"
    do_help
    exit 1
    ;;
esac