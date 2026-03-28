# Claude Code 多 LLM 切換工具

本目錄提供兩個 Shell 工具，讓你在 Claude Code 中輕鬆切換不同的 LLM 提供商，或在本地端運行自己的語言模型。

---

## 檔案說明

| 檔案 | 說明 |
|------|------|
| `claudeCodeModels.sh` | 提供 shell 函式，用於切換 Claude Code 所使用的 LLM |
| `llamaserver.sh` | 管理本地 llama-server 的啟動、停止與狀態查詢 |

---

## 安裝：將 claudeCodeModels.sh 加入 Shell 設定檔

`claudeCodeModels.sh` 必須透過 `source` 載入才能使用，建議加入 `~/.zshrc` 或 `~/.bashrc`，讓每次開啟終端機都自動生效。

### Zsh 使用者（macOS 預設）

```bash
echo 'source /path/to/ClaudeCode/shell/claudeCodeModels.sh' >> ~/.zshrc
source ~/.zshrc
```

### Bash 使用者

```bash
echo 'source /path/to/ClaudeCode/shell/claudeCodeModels.sh' >> ~/.bashrc
source ~/.bashrc
```

> 請將 `/path/to/ClaudeCode/shell/` 替換為你實際的目錄路徑，例如 `~/gitrepo/ClaudeCode/shell/`。

---

## 使用方式

載入後，直接在終端機輸入指令即可啟動 Claude Code，並自動套用對應的 LLM 設定。

### 切換至不同 LLM

```bash
qwen        # 使用 Alibaba Qwen3.5-plus
minimax     # 使用 MiniMax-M2.5
kimi        # 使用 Kimi-K2.5
llama       # 使用本地 llama-server（需先啟動，預設連線 localhost:7080）
```

### 還原為 Anthropic 預設

```bash
cc_reset
```

執行後會清除所有 LLM 覆蓋設定，下次執行 `claude` 時會回到原本的 Anthropic 模型。

### 測試 LLM 連線（不啟動 Claude Code）

在指令後加上 `test` 參數，會對當前設定的 LLM 執行四項連線測試，不會開啟 Claude Code：

```bash
qwen test
llama test
```

四項測試內容：

| 測試 | 說明 |
|------|------|
| `thinking` | 測試推理能力（OpenAI 相容介面） |
| `tool_call` | 測試工具呼叫（Function Calling） |
| `anthropic_messages` | 測試 Anthropic Messages API 相容性 |
| `multimodal` | 測試多模態（圖片 + 文字）能力 |

---

## 使用本地 llama-server

若要使用 `llama` 指令，需先啟動本地 llama-server。

### 安裝 llama-server

請參考 [llama.cpp](https://github.com/ggerganov/llama.cpp) 官方說明安裝 llama-server，並確保執行檔可在 `PATH` 中找到。

### 準備模型檔案

將 GGUF 格式的模型檔案放置於模型目錄（預設為 `~/llm/gguf/`），並依照 `llamaserver.sh` 中的 `MODEL_MAP` 設定對應的路徑。

### 啟動與管理 llama-server

```bash
./llamaserver.sh start              # 啟動預設模型（qwen3）
./llamaserver.sh start gptoss       # 啟動指定模型
./llamaserver.sh stop               # 停止運行中的 server
./llamaserver.sh restart [model]    # 重啟，可同時切換模型
./llamaserver.sh status             # 查看運行狀態與 PID
./llamaserver.sh list               # 列出所有可用模型
./llamaserver.sh logs               # 即時查看今日 log
```

啟動成功後，執行 `llama` 即可讓 Claude Code 連線至本地 server。

---

## 環境變數

所有設定皆可透過環境變數覆蓋，不需修改腳本：

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `LLAMA_PORT` | `7080` | llama-server 監聽埠號（`claudeCodeModels.sh` 與 `llamaserver.sh` 共用） |
| `LLAMA_SERVER_BIN` | `llama-server` | llama-server 執行檔路徑 |
| `LLAMA_MODEL_DIR` | `~/llm/gguf` | GGUF 模型檔案目錄 |
| `LLAMA_MODEL_LOG_DIR` | `~/llm/logs` | Log 輸出目錄 |
| `LLAMA_HOST` | `127.0.0.1` | llama-server 監聽位址 |

範例：使用不同埠號

```bash
export LLAMA_PORT=8080
llama
```

---

## 新增自訂模型

編輯 `llamaserver.sh` 中的 `MODEL_MAP`，加入新模型名稱與對應的 GGUF 檔案路徑：

```bash
declare -A MODEL_MAP=(
  [qwen3]="/qwen/Qwen_Qwen3.5-35B-A3B-Q6_K_L.gguf"
  [gptoss]="/gpt/oss-20b/gpt-oss-20b-mxfp4.gguf"
  [mistral]="/mistral/mistral-7b-v0.1.Q4_K_M.gguf"   # 新增範例
)
```

若該模型支援視覺（多模態），同時在 `MMPROJ_MAP` 加入對應的投影檔：

```bash
declare -A MMPROJ_MAP=(
  [qwen3]="/qwen/mmproj-Qwen_Qwen3.5-35B-A3B-bf16.gguf"
  [mistral]="/mistral/mmproj-mistral-7b.gguf"   # 新增範例
)
```

---

## 運作原理

每個切換函式（`qwen`、`llama` 等）會設定以下四個環境變數，讓 Claude Code 連線至指定的 LLM：

```
ANTHROPIC_BASE_URL        → Anthropic Messages API 端點
OPENAI_API_BASE_URL → OpenAI 相容 API 端點
ANTHROPIC_AUTH_TOKEN      → 認證 Token
ANTHROPIC_MODEL           → 模型名稱
```

設定完成後呼叫 `claude --dangerously-skip-permissions`，並啟用 `ENABLE_LSP_TOOLS=1`。

`cc_reset` 則清除上述所有環境變數，讓 Claude Code 回到 Anthropic 原生設定。
