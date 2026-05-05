# Claude Code 啟動流程說明

從啟動到能回應使用者之前，執行了以下步驟：

## 啟動流程

### 1. 環境初始化
- 讀取環境變數與系統配置（OS: Darwin 25.4.0, Shell: zsh）
- 設定工作目錄為 `/Users/elliot/IdeaProjects/qwtest`

### 2. Git 狀態載入
- 取得當前分支：`master`
- 主分支：`main`
- 載入異動檔案狀態（`pom.xml`、測試檔案等）
- 載入最近提交記錄

### 3. 技能系統載入
- 載入 `superpowers:using-superpowers` 技能（強制在回應前檢查適用技能）
- 載入 30+ 個可用技能清單（TDD、debugging、architecture-doc-generator 等）

### 4. 規則與指令載入
- 讀取 `.claude/CLAUDE.md` 使用者規則
- 載入 Maven 查詢規則（使用 `central.sonatype.com`）
- 載入日期相關規則（當前日期：2026-03-28）

### 5. 記憶體系統就緒
- 準備讀取 `/Users/elliot/.claude/projects/-Users-elliot-IdeaProjects-qwtest/memory/` 中的記憶檔案

### 6. 技能適用性檢查
- 根據 `using-superpowers` 技能要求，檢查是否有技能適用於您的問題
- 您的問題是關於啟動流程的說明，不涉及實作任務，無需呼叫其他技能

完成這些初始化後，才能處理使用者的提問並輸出回應。

---

## 技能系統載入順序與設定影響

### 載入順序

技能系統的載入**沒有嚴格的順序依賴**，但遵循以下優先級規則：

1. **使用者指令最高優先級**（CLAUDE.md、GEMINI.md、AGENTS.md）
2. **Superpowers 技能**（覆蓋預設系統行為）
3. **預設系統提示**（最低優先級）

### 影響技能行為的設定

| 設定類型 | 說明 | 影響 |
|---------|------|------|
| `CLAUDE.md` | 使用者自訂規則 | 可覆蓋技能的預設行為 |
| `settings.json` | Claude Code 設定 | 控制權限、Hooks、環境變數 |
| `keybindings.json` | 按鍵綁定 | 不影響技能邏輯，僅影響操作 |
| MCP 設定 | Model Context Protocol | 影響外部工具存取能力 |

### 技能載入機制

- 系統在啟動時掃描 `.claude/skills/` 和 `.claude/plugins/` 目錄
- 技能以 `SKILL.md` 為識別檔案
- 技能之間可能有依賴關係（如 `using-superpowers` 要求在所有任務前檢查適用技能）
- 多個技能可同時適用時，遵循「流程技能優先，實作技能其次」原則

### 可能造成的影響

1. **技能衝突**：當 `CLAUDE.md` 說「不使用 TDD」但技能說「總是使用 TDD」時，以使用者指令為準
2. **技能觸發條件**：某些技能在特定條件下自動觸發（如 `using-superpowers` 在每段對話開始時）
3. **插件版本**：不同版本的插件可能包含不同版本的技能（如 `superpowers/5.0.6`）

---

## 技能系統類型分類

技能系統包含多種類型，每種類型有不同的目的和行為模式：

### 1. 流程控制型（Process Skills）

控制工作方式和決策流程，決定「如何」執行任務。

| 技能 | 用途 | 類型 |
|------|------|------|
| `using-superpowers` | 所有對話開始時檢查適用技能 | 強制性 |
| `brainstorming` | 實作前的需求探索與設計 | 創造性 |
| `writing-plans` | 將需求轉換為實施計劃 | 規劃性 |
| `executing-plans` | 在獨立會話中執行計劃 | 執行性 |
| `dispatching-parallel-agents` | 將獨立任務分派給子代理 | 協調性 |
| `subagent-driven-development` | 在當前會話中執行獨立任務 | 協調性 |

### 2. 開發工作流型（Workflow Skills）

規範開發過程中的特定階段。

| 技能 | 用途 | 類型 |
|------|------|------|
| `test-driven-development` | TDD 開發流程 | 剛性 |
| `systematic-debugging` | 系統化除錯方法 | 剛性 |
| `verification-before-completion` | 完成前驗證 | 剛性 |
| `requesting-code-review` | 請求程式碼審查 | 剛性 |
| `receiving-code-review` | 接收審查意見處理 | 剛性 |
| `finishing-a-development-branch` | 完成開發分支的決策 | 規劃性 |
| `using-git-worktrees` | Git 工作區隔離 | 工具性 |

### 3. 文件產生型（Documentation Generators）

針對特定類型的文件或代碼分析產生標準化文件。

| 技能 | 用途 | 領域 |
|------|------|------|
| `architecture-doc-generator` | Spring Boot + DDD 架構文件 | 架構 |
| `dfd-analyzer` | 資料流程圖（DFD） | 架構 |
| `event-doc-generator` | Axon Framework 事件文件 | DDD/事件 |
| `error-doc-generator` | 錯誤處理文件 | 錯誤處理 |
| `schedule-doc-generator` | 排程工作文件 | 排程 |
| `entity-doc-generator` | JPA Entity 規格文件 | 資料模型 |
| `springboot-architecture-analyzer` | Spring Boot 專案分析 | 架構 |

### 4. 領域模式型（Domain Patterns）

提供特定技術棧或領域的最佳實踐。

| 技能 | 用途 | 領域 |
|------|------|------|
| `backend-patterns` | 後端開發模式 | 通用後端 |
| `frontend-patterns` | 前端開發模式 | 前端 |
| `golang-patterns` | Go 語言模式 | Go |
| `postgres-patterns` | PostgreSQL 模式 | 資料庫 |
| `clickhouse-io` | ClickHouse 整合 | 分析型 DB |

### 5. 專案管理型（Project Management）

管理需求、任務和報告。

| 技能 | 用途 |
|------|------|
| `requirement-executor` | 需求驅動的任务執行 |
| `ability-test` | 常用測試項目驗證 |
| `iterative-retrieval` | 迭代式資訊檢索 |

### 6. 工具整合型（Tool Integration）

與外部工具或平台整合。

| 技能 | 用途 |
|------|------|
| `obsidian-markdown` | Obsidian 筆記整合 |
| `obsidian-bases` | Obsidian Bases 插件 |
| `json-canvas` | JSON Canvas 整合 |
| `discord` / `imessage` / `telegram` | 通訊軟體整合 |

### 7. 學習與演化型（Learning & Evolution）

持續學習和知識累積。

| 技能 | 用途 |
|------|------|
| `continuous-learning` | 持續學習專案模式 |
| `continuous-learning-v2` | 進化版持續學習 |
| `strategic-compact` | 緊湊型戰略分析 |

### 8. 安全與驗證型（Security & Verification）

| 技能 | 用途 |
|------|------|
| `security-review` | 安全審查 |
| `eval-harness` | 評估測試 |
| `verification-loop` | 驗證迴圈 |

### 技能類型特性比較

| 特性 | 剛性技能 | 彈性技能 |
|------|---------|---------|
| 遵循程度 | 嚴格按照技能規定 | 可根據情境調整 |
| 範例 | TDD、debugging | patterns、architecture |
| 覆蓋 | 使用者明確指示可覆蓋 | 原則指導，靈活應用 |

### 技能來源

1. **官方 Superpowers**（`.claude/plugins/cache/claude-plugins-official/superpowers/`）
2. **自訂技能**（`.claude/skills/`）
3. **市場插件**（`.claude/plugins/marketplaces/`）
4. **插件快取**（`.claude/plugins/cache/`）

---

## 工具（Tools）與技能（Skills）的差異

**工具和技能是不同的概念**，兩者有不同的目的和運作方式：

### 工具（Tools）的定義

工具是 **Claude Code 可以直接呼叫的 API 或函數**，用來執行具體的動作。工具是技能與環境互動的媒介。

### 可用工具清單

| 工具 | 用途 | 範例 |
|------|------|------|
| `Bash` | 執行 shell 命令 | `git status`、`npm test` |
| `Read` | 讀取檔案內容 | 讀取程式碼、設定檔 |
| `Write` | 寫入檔案 | 建立新檔案、覆蓋檔案 |
| `Edit` | 編輯現有檔案 | 替換程式碼片段 |
| `Glob` | 搜尋檔案路徑 | `**/*.java` |
| `Grep` | 搜尋檔案內容 | 搜尋關鍵字、regex |
| `Agent` | 啟動子代理 | 委派複雜任務 |
| `TaskCreate` / `TaskUpdate` | 管理任務清單 | 追蹤進度 |
| `AskUserQuestion` | 向使用者提問 | 澄清需求 |
| `EnterPlanMode` / `ExitPlanMode` | 進入/離開計劃模式 | 規劃實施方案 |
| `WebSearch` / `WebFetch` | 網路搜尋與抓取 | 查詢文件、最新消息 |
| `LSP` | 語言伺服器協定 | 程式碼分析、跳轉定義 |
| `CronCreate` / `CronList` / `CronDelete` | 排程任務 | 定時提醒 |
| `RemoteTrigger` | 遠端觸發 API | claude.ai 整合 |

### 工具 vs 技能 比較

| 特性 | 工具（Tools） | 技能（Skills） |
|------|-------------|---------------|
| **本質** | 執行動作的 API | 指導行為的規則/流程 |
| **來源** | Claude Code 內建 | 使用者/社群定義的 `.md` 檔案 |
| **數量** | 固定（約 15-20 個） | 可無限擴充 |
| **调用方式** | 直接透過 tool_use | 透過 `Skill` 工具間接載入 |
| **內容** | 功能性程式碼 | Markdown 格式的指導文件 |
| **可自訂** | 否（由 Anthropic 定義） | 是（使用者可编写/修改） |
| **層級** | 低階（執行層） | 高階（策略/流程層） |

### 兩者的關係

```
┌─────────────────────────────────────────────────────────┐
│  Skill（技能）：指導「如何」完成任務                      │
│  ┌─────────────────────────────────────────────────┐    │
│  │ writing-plans                                   │    │
│  │ 1. 讀取需求文件                                   │    │
│  │ 2. 分析依賴關係                                   │    │
│  │ 3. 建立任務清單                                   │    │
│  │ 4. 逐一執行                                     │    │
│  │    └─> 使用 TaskCreate 建立任務                   │    │
│  │    └─> 使用 Bash 執行測試                         │    │
│  │    └─> 使用 Edit 修改程式碼                       │    │
│  └─────────────────────────────────────────────────┘    │
│                           ↓ 使用                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Tools（工具）：實際執行動作                       │    │
│  │ TaskCreate, Bash, Edit, Read, Write, Glob...    │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### 範例說明

**情境：使用者說「幫我修復這個 bug」**

1. **技能層級**（`systematic-debugging`）：
   - 指導你如何分析問題
   - 要求先重現問題
   - 建議假設並逐一驗證
   - 最後修復並增加測試覆蓋

2. **工具層級**：
   - `Read` 讀取相關程式碼
   - `Bash` 執行測試確認問題
   - `Edit` 修改程式碼
   - `Bash` 再次執行測試驗證修復

**簡單來說：**
- **技能 = 策略/流程/最佳實踐**（告訴你要怎麼想、怎麼做）
- **工具 = 實作手段**（讓你實際執行動作）

### 技能如何使用工具

技能文件中會指定何時使用哪些工具。例如：

- `test-driven-development` 技能會要求：
  1. 先用 `Glob` 找到測試檔案位置
  2. 用 `Read` 讀取現有測試
  3. 用 `Write` 建立新的測試檔案（紅）
  4. 用 `Bash` 執行測試確認失敗（紅）
  5. 用 `Edit` 修改實作（綠）
  6. 用 `Bash` 執行測試確認通過（綠）
  7. 用 `Edit` 重構程式碼（重構）

### 總結

| 問題 | 答案 |
|------|------|
| 工具和技能一樣嗎？ | **不一樣** |
| 誰定義工具？ | Anthropic（Claude Code 內建） |
| 誰定義技能？ | 使用者/社群（`.claude/skills/` 中的 `.md` 檔案） |
| 誰呼叫誰？ | 技能指導何時使用哪些工具 |
| 哪個可以自訂？ | 技能可以，工具不行 |

---

## Claude Code 工具詳細說明

### 1. Bash - Shell 命令執行

**用途**：執行系統命令、脚本、程式

**參數**：
- `command`（必填）：要執行的命令
- `description`（必填）：命令描述（5-10 字）
- `timeout`（選填）：超時時間（ms），預設 120000，最大 600000
- `run_in_background`（選填）：背景執行
- `dangerouslyDisableSandbox`（選填）：禁用沙盒

**使用範例**：
```
Bash(command="git status", description="Show working tree status")
Bash(command="npm test -- --coverage", description="Run tests with coverage")
Bash(command="docker-compose up -d", description="Start Docker containers", run_in_background=true)
```

**內部實做邏輯**：
1. 驗證命令是否在允許列表中（取決於權限模式）
2. 在工作目錄中啟動 shell 會話（bash 或 zsh）
3. 執行命令並捕捉 stdout/stderr
4. 若超時則終止程序
5. 返回輸出結果

**注意事項**：
- 路徑含空格需用雙引號包裹
- 多命令用 `&&` 串接，不用換行
- 避免用於 `find`、`grep`、`cat`（改用專用工具）
- 背景執行時需用 `Read` 查看輸出

---

### 2. Read - 檔案讀取

**用途**：讀取本地檔案內容

**參數**：
- `file_path`（必填）：絕對路徑
- `limit`（選填）：最大行數，預設 2000
- `offset`（選填）：起始行號
- `pages`（選填）：PDF 頁碼範圍（如 "1-5"）

**使用範例**：
```
Read(file_path="/Users/elliot/project/src/main.java")
Read(file_path="/path/to/large.log", limit=100, offset=500)
Read(file_path="/doc.pdf", pages="1-10")
```

**內部實做邏輯**：
1. 驗證檔案路徑有效性
2. 檢查檔案類型（純文字、PDF、圖片、notebook）
3. 根據類型選擇解析器：
   - 純文字：逐行讀取，添加行號前綴
   - PDF：使用 PDF 解析庫提取文字
   - 圖片：載入為多模態輸入
   - .ipynb：解析 JSON 並提取 cell
4. 返回格式化內容

**輸出格式**：
```
1→package com.example;
2→
3→public class Hello {
4→    public static void main(String[] args) {
5→        System.out.println("Hello");
6→    }
7→}
```

---

### 3. Write - 檔案寫入

**用途**：建立新檔案或覆蓋現有檔案

**參數**：
- `file_path`（必填）：絕對路徑
- `content`（必填）：檔案內容

**使用範例**：
```
Write(file_path="/Users/elliot/project/new.txt", content="Hello World")
```

**內部實做邏輯**：
1. 檢查父目錄是否存在
2. 若檔案已存在，先讀取（防止意外覆蓋）
3. 建立或截斷檔案
4. 寫入內容（UTF-8 編碼）
5. 返回成功狀態

**注意事項**：
- 會完全覆蓋現有檔案
- 編輯現有檔案優先用 `Edit`
- 不自動建立目錄結構

---

### 4. Edit - 檔案編輯

**用途**：替換檔案中的特定字串

**參數**：
- `file_path`（必填）：絕對路徑
- `old_string`（必填）：要替換的原始字串
- `new_string`（必填）：替換後的字串
- `replace_all`（選填）：替換所有出現，預設 false

**使用範例**：
```
Edit(
  file_path="/Users/elliot/project/src.java",
  old_string="public void oldMethod()",
  new_string="public void newMethod()"
)
```

**內部實做邏輯**：
1. 讀取檔案內容（若未讀取過）
2. 在檔案中搜尋 `old_string`
3. 檢查匹配次數：
   - 0 次：錯誤（字串不存在）
   - 1 次：替換該處
   - 多次：錯誤（除非 `replace_all=true`）
4. 驗證 `old_string` != `new_string`
5. 寫回檔案
6. 返回成功狀態

**注意事項**：
- `old_string` 必須完全匹配（包含空白）
- 行號前綴不計入匹配內容
- 唯一性驗證失敗時需提供更多上下文

---

### 5. Glob - 檔案路徑搜尋

**用途**：使用 glob 模式搜尋檔案

**參數**：
- `pattern`（必填）：glob 模式
- `path`（選填）：搜尋目錄，預設 cwd

**使用範例**：
```
Glob(pattern="**/*.java")
Glob(pattern="src/**/*.test.ts", path="/Users/elliot/project")
```

**內部實做邏輯**：
1. 解析 glob 模式（支援 `**`、`*`、`?`、`[]`）
2. 從指定目錄開始遞迴遍历
3. 過濾匹配的檔案路徑
4. 依修改時間排序
5. 返回匹配路徑列表

**支援的模式**：
- `*`：匹配任意字元（不含路徑分隔符）
- `**`：匹配任意目錄深度
- `?`：匹配單一字元
- `[]`：字元集合（如 `*.{java,kt}`）

---

### 6. Grep - 檔案內容搜尋

**用途**：使用 regex 搜尋檔案內容（基於 ripgrep）

**參數**：
- `pattern`（必填）：正則表達式
- `path`（選填）：搜尋目錄
- `type`（選填）：檔案類型（如 `java`、`py`）
- `glob`（選填）：檔案過濾（如 `*.test.ts`）
- `output_mode`（選填）：`content`、`files_with_matches`、`count`
- `-n`（選填）：顯示行號
- `-i`（選填）：忽略大小寫
- `-C`/`-A`/`-B`（選填）：上下文行數
- `multiline`（選填）：跨行匹配

**使用範例**：
```
Grep(pattern="class.*Controller", type="java")
Grep(pattern="TODO", output_mode="content", -n=true)
Grep(pattern="import.*react", glob="*.tsx", -i=true)
```

**內部實做邏輯**：
1. 構建 ripgrep 命令
2. 編譯正則表達式
3. 遍歷符合条件的檔案
4. 執行 regex 匹配
5. 格式化輸出（內容/檔案列表/計數）

---

### 7. Agent - 子代理啟動

**用途**：啟動專用子代理處理複雜任務

**參數**：
- `prompt`（必填）：任務描述
- `description`（必填）：短描述（3-5 字）
- `subagent_type`（選填）：
  - `general-purpose`：通用研究
  - `Explore`：程式碼庫探索
  - `Plan`：架構設計
  - `code-reviewer`：程式碼審查
- `isolation`（選填）：`worktree` 隔離模式
- `run_in_background`（選填）：背景執行

**使用範例**：
```
Agent(
  prompt="搜尋所有 API 端點並整理成表格",
  description="Find API endpoints",
  subagent_type="Explore"
)
```

**內部實做邏輯**：
1. 建立子程序
2. 複製會話上下文（可選隔離）
3. 執行子代理任務
4. 等待完成（若背景執行則非阻塞）
5. 返回結果摘要

---

### 8. TaskCreate / TaskGet / TaskList / TaskUpdate - 任務管理

**用途**：結構化任務追蹤

**TaskCreate 參數**：
- `subject`（必填）：任務標題
- `description`（必填）：任務描述
- `activeForm`（選填）：進行中顯示文字

**TaskUpdate 參數**：
- `taskId`（必填）：任務 ID
- `status`（選填）：`pending`、`in_progress`、`completed`、`deleted`
- `addBlockedBy`（選填）：依賴的任務 ID
- `addBlocks`（選填）：被此任務阻擋的任務

**使用範例**：
```
TaskCreate(subject="新增登入功能", description="實作 OAuth 2.0 登入")
TaskUpdate(taskId="1", status="in_progress")
TaskList()
TaskGet(taskId="1")
```

**內部實做邏輯**：
1. 維護記憶體中的任務清單
2. 分配唯一 ID
3. 追蹤狀態轉換
4. 驗證依賴關係

---

### 9. AskUserQuestion - 使用者提問

**用途**：向使用者澄清需求或選擇

**參數**：
- `questions`（必填）：問題列表（1-4 個）
  - `question`：完整問題
  - `header`：短標籤（最大 12 字元）
  - `options`：選項（2-4 個）
  - `multiSelect`：是否多選

**使用範例**：
```
AskUserQuestion(
  questions=[{
    question="選擇哪種資料庫？",
    header="Database",
    options=[
      {label="PostgreSQL", description="關連式資料庫"},
      {label="MongoDB", description="文件資料庫"}
    ],
    multiSelect=false
  }]
)
```

---

### 10. EnterPlanMode / ExitPlanMode - 計劃模式

**用途**：進入/退出計劃撰寫階段

**EnterPlanMode**：
- 無參數
- transition 到計劃模式
- 使用者需批准後才能實作

**ExitPlanMode**：
- 參數：`allowedPrompts`（權限列表）
- 提交計劃供使用者審查

---

### 11. WebSearch / WebFetch - 網路功能

**WebSearch**：
- `query`：搜尋詞
- `allowed_domains` / `blocked_domains`：網域過濾

**WebFetch**：
- `url`：目標 URL
- `prompt`：提取指令

**限制**：
- 無法存取需要認證的頁面
- 有 15 分鐘快取

---

### 12. LSP - 語言伺服器協定

**用途**：程式碼智慧功能

**操作**：
- `goToDefinition`：跳轉定義
- `findReferences`：尋找參考
- `hover`：懸浮資訊
- `documentSymbol`：文件符號
- `incomingCalls` / `outgoingCalls`：呼叫階層

---

### 13. CronCreate / CronList / CronDelete - 排程

**CronCreate**：
- `cron`：cron 表達式
- `prompt`：執行的提示詞
- `recurring`：是否重複
- `durable`：是否持久化

---

### 14. RemoteTrigger - 遠端觸發

**用途**：claude.ai 整合

**操作**：
- `list` / `get` / `create` / `update` / `run`

---

## Agent 工具詳細使用指南

### 一、什麼是 Agent（子代理）？

**Agent** 是一個可以啟動**獨立子程序**的工具，讓Claude能夠委派複雜任務給專門的子代理執行。

```
┌─────────────────────────────────────────────────────────┐
│                    主代理 (Main)                         │
│                     Claude Code                          │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Agent 工具呼叫                                    │   │
│  │  ↓                                                │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │            子代理 (Subagent)                │  │   │
│  │  │  - 獨立的 Claude 實例                        │  │   │
│  │  │  - 有自己的會話上下文                        │  │   │
│  │  │  - 可以呼叫工具                             │  │   │
│  │  │  - 執行完成後返回結果                        │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

### 二、子代理類型（subagent_type）

| 類型 | 用途 | 可用工具 |
|------|------|---------|
| `general-purpose` | 通用研究、複雜任務 | 所有工具 |
| `Explore` | 程式碼庫探索、搜尋 | 除 Agent 和 Edit/Write 外的工具 |
| `Plan` | 架構設計、實現計劃 | 除 Edit/Write/Agent 外的工具 |
| `code-reviewer` | 程式碼審查 | 所有工具 |
| `claude-code-guide` | Claude Code 使用問題 | Glob/Grep/Read/WebFetch/WebSearch |
| `statusline-setup` | 狀態列配置 | Read/Edit |
| `superpowers` | Superpowers 相關任務 | 所有工具 |

---

### 三、使用時機

#### 應該使用 Agent 的情況：

| 情境 | 說明 | 推薦 subagent_type |
|------|------|-------------------|
| **開放式搜尋** | 不確定目標位置，需要多次嘗試 | `Explore` |
| **多位置搜尋** | 同一關鍵字在不同命名慣例下 | `Explore` |
| **複雜研究** | 需要理解多個元件的互動 | `general-purpose` |
| **程式碼審查** | 完成主要功能後的審查 | `code-reviewer` |
| **架構設計** | 規劃實現策略 | `Plan` |
| **獨立任務** | 與主流程無關的工作 | `general-purpose` |

#### 不應該使用 Agent 的情況：

| 情境 | 原因 |
|------|------|
| 讀取特定檔案 | 直接用 `Read` |
| 已知位置的搜尋 | 直接用 `Glob`/`Grep` |
| 簡單修改 | 直接用 `Edit` |
| 需要保留主上下文 | 子代理會消耗額外 context |

---

### 四、參數詳解

```javascript
Agent({
  description: "3-5 字短描述",        // 必填：用於 UI 顯示
  prompt: "完整的任務說明...",         // 必填：越詳細越好
  subagent_type: "Explore",           // 選填：預設 general-purpose
  isolation: "worktree",              // 選填：git 工作樹隔離
  run_in_background: true,            // 選填：背景執行
  model: "opus"                       // 選填：模型選擇
})
```

#### prompt 撰寫最佳實踐：

**好的 prompt：**
```
請搜尋這個 Spring Boot 專案中所有 REST API 端點。
找出所有 @RestController 和 @RequestMapping 註解。
對於每個端點，記錄：
1. HTTP 方法（GET/POST/PUT/DELETE）
2. 路徑
3. 所屬的 Controller 類別
4. 方法名稱
5. 參數列表

請按照功能模組分類整理成表格。
```

**不好的 prompt：**
```
找一下 API
```

---

### 五、執行模式

#### 前景執行（預設）

```javascript
Agent({
  description: "搜尋 API 端點",
  prompt: "...",
  subagent_type: "Explore"
})
```

- 主代理**等待**子代理完成
- 結果立即返回
- 適合需要結果才能繼續的任務

#### 背景執行

```javascript
Agent({
  description: "執行長時間測試",
  prompt: "...",
  run_in_background: true
})
```

- 主代理**不等待**，立即繼續
- 收到通知時用 `TaskOutput` 查看結果
- 適合獨立、耗時的任務

---

### 六、isolation: "worktree" 模式

當子代理需要修改檔案但不想影響主工作區時：

```javascript
Agent({
  description: "重構模組",
  prompt: "進行大規模重構，測試多種方案",
  isolation: "worktree"
})
```

**效果：**
- 建立暫時的 git worktree
- 子代理在隔離環境中操作
- 若有修改，返回 worktree 路徑和分支名稱
- 若無修改，自動清理

---

### 七、執行流程

```
1. 主代理決定委派任務
       ↓
2. 呼叫 Agent 工具
       ↓
3. 系統建立子代理實例
   - 分配獨立上下文
   - 複製必要的會話狀態
   - 注入 prompt
       ↓
4. 子代理執行任務
   - 可以呼叫自己的工具
   - 有自己的思考過程
       ↓
5. 完成並返回結果
   - 摘要報告
   - 重要發現
   - 建議的下一步
       ↓
6. 主代理接收結果並繼續
```

---

### 八、結果與影響

#### 返回內容格式

子代理完成後返回：

```markdown
## 任務摘要
[簡短說明完成了什麼]

## 主要發現
1. [發現 1]
2. [發現 2]
...

## 詳細結果
[表格、程式碼片段、檔案路徑等]

## 建議
[後續行動建議]
```

#### 對主會話的影響

| 面向 | 影響 |
|------|------|
| **上下文** | 子代理的結果會佔用主會話 context |
| **檔案修改** | 若有修改檔案，會反映在工作區 |
| **任務清單** | 子代理建立的任務保持存在 |
| **Git 狀態** | 若有 commit，會保留在 git log |

#### 對成本/時間的影響

| 因子 | 說明 |
|------|------|
| **Token 消耗** | 子代理有自己的輸入/輸出 tokens |
| **執行時間** | 依任務複雜度，可能數秒到數分鐘 |
| **並行效益** | 背景執行可與主流程並行 |

---

### 九、實際範例

#### 範例 1：探索程式碼庫

```javascript
Agent({
  description: "探索認證模組",
  prompt: `
請探索這個專案的認證相關程式碼。

1. 找出所有與 authentication 相關的檔案
2. 了解認證流程（從 controller → service → repository）
3. 找出使用的認證提供者（JWT、OAuth、LDAP 等）
4. 繪製認證流程的 sequence diagram

請詳細記錄每個元件的職責和互動方式。
  `,
  subagent_type: "Explore"
})
```

#### 範例 2：程式碼審查

```javascript
Agent({
  description: "審查登入功能",
  prompt: `
我剛完成了登入功能的實作。請審查以下檔案：

- src/main/java/com/example/auth/LoginController.java
- src/main/java/com/example/auth/AuthService.java

審查重點：
1. 安全性（密碼儲存、token 管理、常見攻擊防護）
2. 程式碼品質（可讀性、重複程式碼、錯誤處理）
3. 測試覆蓋率

請指出任何潛在問題和改進建議。
  `,
  subagent_type: "code-reviewer"
})
```

#### 範例 3：架構設計

```javascript
Agent({
  description: "設計快取架構",
  prompt: `
我們需要在現有系統中加入 Redis 快取層。

已知條件：
- 現有系統：Spring Boot + JPA + PostgreSQL
- 主要瓶頸：重複查詢頻繁
- 資料一致性要求：最終一致性可接受

請設計快取架構方案，包含：
1. 快取策略（cache-aside/write-through/write-back）
2. 快取失效機制
3. Redis 資料結構設計
4. 錯誤處理和降級策略

輸出完整的實現計劃。
  `,
  subagent_type: "Plan"
})
```

#### 範例 4：平行執行多個任務

```javascript
// 同時啟動多個子代理
Agent({
  description: "前端 API 整合",
  prompt: "檢查所有 frontend 檔案中使用的 API 端點",
  subagent_type: "Explore",
  run_in_background: true
})

Agent({
  description: "後端 API 檢視",
  prompt: "列出所有後端 Controller 定義的端點",
  subagent_type: "Explore",
  run_in_background: true
})

// 主代理可以繼續其他工作，等待通知後查看結果
```

---

### 十、注意事項與最佳實踐

#### DO（應該做的）

- ✅ 提供詳細的 prompt，包含預期輸出格式
- ✅ 選擇合適的 subagent_type
- ✅ 對於獨立任務使用背景執行
- ✅ 在 prompt 中說明「不要做什麼」
- ✅ 對於需要多次工具呼叫的任務使用 Agent

#### DON'T（不應該做的）

- ❌ 用 Agent 執行簡單單一工具呼叫
- ❌ 給出模糊的 prompt
- ❌ 期望子代理保留主對話的隱含上下文
- ❌ 忘記背景執行需要主動查看結果

---

### 十一、常見問題

**Q: 子代理能看到我的對話歷史嗎？**
A: 不能，子代理只看到您透過 prompt 提供的資訊。

**Q: 子代理能修改檔案嗎？**
A: 可以，修改會直接反映在工作區。

**Q: 如何停止背景執行的子代理？**
A: 使用 `TaskStop` 工具。

**Q: 子代理的成本如何計算？**
A: 獨立計算 tokens，加總到總成本。

**Q: 可以巢狀呼叫 Agent 嗎？**
A: 可以，但不建議，會增加複雜度。

---

### 十二、與其他工具的關係

| 工具組合 | 使用情境 |
|---------|---------|
| Agent + TaskCreate | 建立任務後委派給子代理 |
| Agent + run_in_background | 長時間任務不干擾主流程 |
| Agent + isolation:worktree | 實驗性修改不影響主工作區 |
| 多個 Agent 平行 | 獨立任務同時執行 |

---

## 使用多個 Agent 執行 Todo List 任務

### 現有技能：dispatching-parallel-agents

這個技能專門用於**同時分派多個獨立任務給不同的子代理**。

**適用情境：**
- 有 2+ 個獨立任務可以平行執行
- 任務之間沒有共享狀態或順序依賴
- 每個任務可以獨立完成

### 範例：測試 Agent 多任務執行

假設您有一個待辦清單包含 3 個獨立任務：

```markdown
## Todo List

1. **任務 A**：為 User 實體新增 email 欄位驗證
2. **任務 B**：為 Product 實體新增 price 欄位驗證
3. **任務 C**：為 Order 實體新增 quantity 欄位驗證
```

這 3 個任務互不干擾，可以分派給 3 個不同的 Agent 平行執行。

### 實際操作範例

**步驟 1：建立任務清單**

```javascript
TaskCreate(
  subject="為 User 實體新增 email 欄位驗證",
  description="在 User.java 中加入 @Email 和 @NotBlank 驗證註解"
)
TaskCreate(
  subject="為 Product 實體新增 price 欄位驗證",
  description="在 Product.java 中加入 @DecimalMin('0') 驗證註解"
)
TaskCreate(
  subject="為 Order 實體新增 quantity 欄位驗證",
  description="在 Order.java 中加入 @Min('1') 驗證註解"
)
```

**步驟 2：分派 Agent 平行執行**

```javascript
// Agent 1 - 負責任務 A
Agent(
  description="User email 驗證",
  prompt=`
請為 User 實體的 email 欄位新增驗證：

1. 讀取 src/main/java/com/example/domain/User.java
2. 新增 @Email 和 @NotBlank 註解到 email 欄位
3. 確保 import 語句正確
4. 執行 mvn test -Dtest=UserTest 確認測試通過

只修改 User.java，不要動其他檔案。
  `,
  subagent_type="general-purpose",
  run_in_background=true
)

// Agent 2 - 負責任務 B
Agent(
  description="Product price 驗證",
  prompt=`
請為 Product 實體的 price 欄位新增驗證：

1. 讀取 src/main/java/com/example/domain/Product.java
2. 新增 @DecimalMin('0') 註解到 price 欄位
3. 確保 import 語句正確
4. 執行 mvn test -Dtest=ProductTest 確認測試通過

只修改 Product.java，不要動其他檔案。
  `,
  subagent_type="general-purpose",
  run_in_background=true
)

// Agent 3 - 負責任務 C
Agent(
  description="Order quantity 驗證",
  prompt=`
請為 Order 實體的 quantity 欄位新增驗證：

1. 讀取 src/main/java/com/example/domain/Order.java
2. 新增 @Min('1') 註解到 quantity 欄位
3. 確保 import 語句正確
4. 執行 mvn test -Dtest=OrderTest 確認測試通過

只修改 Order.java，不要動其他檔案。
  `,
  subagent_type="general-purpose",
  run_in_background=true
)
```

**步驟 3：等待並整合結果**

背景執行的 Agent 完成後會通知您。使用 `TaskOutput` 查看結果：

```javascript
// 等待通知後查看各 Agent 的結果
TaskOutput(task_id="agent-1", block=true, timeout=300000)
TaskOutput(task_id="agent-2", block=true, timeout=300000)
TaskOutput(task_id="agent-3", block=true, timeout=300000)
```

### 完整測試 Skill 範例

如果您想要一個**可重複使用的技能**來測試 Agent，可以建立以下技能：

**檔案位置：** `~/.claude/skills/test-agent-execution/SKILL.md`

```markdown
---
name: test-agent-execution
description: 測試多個 Agent 平行執行 Todo List 任務
---

# Agent 執行測試技能

這個技能用於測試 Agent 工具的多任務平行執行能力。

## 測試流程

### 1. 建立測試任務清單

建立 3-5 個獨立任務，例如：
- 任務 A：修改檔案 A 的特定欄位
- 任務 B：修改檔案 B 的特定欄位
- 任務 C：修改檔案 C 的特定欄位

### 2. 為每個任務建立 TodoWrite

```
TaskCreate(subject="任務 A", description="...")
TaskCreate(subject="任務 B", description="...")
TaskCreate(subject="任務 C", description="...")
```

### 3. 分派 Agent 平行執行

對每個任務呼叫 Agent 工具：
- 使用 `run_in_background=true` 實現平行執行
- 每個 Agent 只處理自己的任務範圍
- 明確指定不要修改其他檔案

### 4. 等待並驗證結果

- 等待所有 Agent 完成（會收到通知）
- 使用 `TaskOutput` 查看各 Agent 的結果
- 檢查 git diff 確認修改正確
- 執行完整測試套件

### 5. 總結報告

整理各 Agent 的執行結果：
- 執行時間
- 修改內容
- 測試結果
- 是否有衝突

## 測試檢查清單

- [ ] 所有 Agent 都完成任務
- [ ] 沒有檔案修改衝突
- [ ] 所有測試通過
- [ ] git 提交記錄正確
- [ ] 平行執行確實節省時間
```

## 關鍵要點

| 面向 | 說明 |
|------|------|
| **任務獨立性** | 確保任務間無依賴關係 |
| **明確的 prompt** | 清楚指定範圍和限制 |
| **背景執行** | 使用 `run_in_background=true` |
| **結果整合** | 等待所有完成後再驗證 |
| **衝突檢查** | 確認 Agent 修改沒有重疊 |
