# Claude Code 架構自我檢視報告 / Claude Code Architecture Self-Examination Report

## 1. 啟動流程 / Startup Process

### 完整啟動流程 / Complete Startup Process

根據我在系統提示詞中觀察到的內容，我的啟動流程如下：

**階段 1：環境初始化 / Phase 1: Environment Initialization**
- Harness 載入使用者的配置檔案（`~/.claude/CLAUDE.md`、專案層的 `CLAUDE.md`）
- Harness 偵測並注入環境上下文（平台、作業系統、Shell、Git 狀態等）
- Harness 建立工具定義並提供給我

*由 Harness 執行 / Executed by Harness*

**階段 2：系統提示詞組裝 / Phase 2: System Prompt Assembly**
- Harness 組裝完整的系統提示詞，包含：
  - 核心行為指令（工具使用規範、程式碼撰寫指南等）
  - 記憶體系統說明（`MEMORY.md` 相關指示）
  - 技能系統定義
  - 環境上下文資訊

*由 Harness 執行 / Executed by Harness*

**階段 3：模型接收與處理 / Phase 3: Model Reception and Processing**
- 我（模型）接收組裝完成的系統提示詞
- 我解析系統提示詞中的指令和約束
- 我準備好接收使用者訊息

*由 Model 執行 / Executed by Model*

**階段 4：技能載入 / Phase 4: Skill Loading**
- 系統提示詞中包含可用技能的列表
- 當使用者觸發或暗示使用特定技能時，我使用 `Skill` 工具載入該技能的完整內容

*由 Model 執行（技能內容載入） / Executed by Model (skill content loading)*

---

## 2. 系統提示詞組成 / System Prompt Composition

### 系統提示詞種類 / Types of System Prompts

根據我收到的系統提示詞，可分為以下幾類：

1. **核心系統指令 / Core System Instructions**
   - 由 Anthropic 提供，定義 Claude Code 的基本行為模式
   - 包含工具使用規範、Git 操作規則、程式碼撰寫指南

2. **使用者自訂指令 / User-Defined Instructions**
   - 來源：`~/.claude/CLAUDE.md`（全域）和專案層級的 `CLAUDE.md`
   - 在此專案中，使用者設定了 Maven 查詢規則和年度搜尋條件

3. **環境上下文 / Environment Context**
   - 由 Harness 自動偵測並注入
   - 包含平台資訊、工作目錄、Git 狀態等

4. **技能系統定義 / Skill System Definitions**
   - 包含所有可用技能的名稱、描述和使用指南

### 工具定義和配置 / Tool Definitions and Configuration

**工具定義存放位置 / Tool Definition Location:**
- 工具定義由 Harness 在啟動時提供，作為系統提示詞的一部分
- 工具的 JSON Schema 定義直接嵌入在系統提示詞中
- 使用者可透過 `~/.claude/settings.json` 配置某些工具行為

**使用者新增工具 / Adding User Tools:**
根據系統提示詞，使用者可以：
- 透過 MCP (Model Context Protocol) 伺服器新增工具
- 使用 `Skill` 工具呼叫自訂技能
- 在 `settings.json` 中配置 hooks 來執行自訂 shell 命令

### 行為準則 / Behavioral Guidelines

系統提示詞中嵌入的行為準則包括：

1. **工具使用規範 / Tool Usage Guidelines**
   - "Do NOT use the Bash to run commands when a relevant dedicated tool is provided"
   - 優先使用專用工具（Read、Edit、Write 等）而非 shell 命令

2. **Git 操作規則 / Git Operation Rules**
   - "NEVER update the git config"
   - "NEVER run destructive git commands...unless the user explicitly requests"
   - "NEVER skip hooks (--no-verify, --no-gpg-sign, etc) unless the user explicitly requests it"
   - "Always create NEW commits rather than amending"

3. **程式碼撰寫原則 / Code Writing Principles**
   - "Be careful not to introduce security vulnerabilities"
   - "Don't add features, refactor code, or make 'improvements' beyond what was asked"

**參考檔案 / Reference Files:**
- `~/.claude/CLAUDE.md` - 使用者全域指令
- 專案根目錄的 `CLAUDE.md` - 專案特定指令

### Git 操作規則覆寫 / Overriding Git Rules

Git 規則可以被覆寫的方式：
1. **使用者明確請求 / Explicit User Request** - 當使用者明確要求使用 `--force` 或其他破壞性操作時
2. **透過 CLAUDE.md 配置 / Via CLAUDE.md Configuration** - 使用者可以在配置檔案中設定預設行為

---

## 3. 環境上下文注入 / Environment Context Injection

### 環境上下文類型 / Types of Environment Context

根據系統提示詞中的 `<environment>` 區塊，包含以下類型：

1. **工作目錄資訊 / Working Directory Information**
   ```
   Primary working directory: /Users/elliot/IdeaProjects/qwtest
   Is a git repository: true
   ```

2. **平台資訊 / Platform Information**
   ```
   Platform: darwin
   Shell: zsh
   OS Version: Darwin 25.4.0
   ```

3. **模型資訊 / Model Information**
   ```
   You are powered by the model qwen3.5-plus.
   ```

4. **Claude Code 版本資訊 / Claude Code Version Information**
   ```
   Claude Code is available as a CLI in the terminal, desktop app (Mac/Windows),
   web app (claude.ai/code), and IDE extensions (VS Code, JetBrains).
   ```

5. **Git 狀態快照 / Git Status Snapshot**
   ```
   Current branch: master
   Main branch: main
   Status: [list of modified/untracked files]
   Recent commits: [list of recent commits]
   ```

### 注入方式 / Injection Method

**由 Harness 預先注入 / Pre-injected by Harness:**
- 所有環境上下文都是由 Harness 在會話開始時自動偵測並注入
- 我不是自行偵測這些資訊，而是直接接收已處理的結果

### Git 狀態：快照 vs. 即時 / Git Status: Snapshot vs. Real-time

**Git 狀態是快照 / Git Status is a Snapshot:**
```
gitStatus: This is the git status at the start of the conversation.
Note that this status is a snapshot in time, and will not update during the conversation.
```

**影響 / Implications:**
- Git 狀態在會話開始時捕獲，之後不會自動更新
- 如果需要最新的 Git 狀態，必須使用 `Bash` 工具執行 `git status`
- 這意味著我需要注意在長時間會話中，Git 狀態資訊可能已過時

---

## 4. Harness

### 什麼是 Claude Code Harness / What is Claude Code Harness

**Claude Code Harness** 是圍繞著我（模型）運行的基礎設施層，負責：
- 載入和組裝系統提示詞
- 提供工具定義和執行工具呼叫
- 管理會話狀態和上下文壓縮
- 處理檔案系統和外部服務的存取

**具體術語解釋 / Concrete Explanation:**
Harness 是一個中介層（middleware），介於使用者介面（CLI、桌面應用、IDE 擴充功能）和我（語言模型）之間。它提供了：
- 工具執行環境
- 權限管理
- 上下文管理（包含記憶體壓縮）
- 技能系統支援

### Harness 與 Model 的關係 / Harness-Model Relationship

| **Harness 負責 / Harness Responsibilities** | **Model 負責 / Model Responsibilities** |
|---------------------------------------------|-------------------------------------------|
| 載入使用者配置文件 | 解析並遵循配置中的指令 |
| 偵測並注入環境上下文 | 使用環境上下文來輔助決策 |
| 提供工具定義 | 決定何時呼叫哪些工具 |
| 執行工具呼叫並回傳結果 | 解讀工具結果並生成回應 |
| 管理上下文壓縮 | 在被壓縮的上下文中保持連貫性 |
| 處理技能載入請求 | 呼叫 `Skill` 工具來載入技能內容 |

### 互動循環示範 / Interaction Cycle Demonstration

```
1. 使用者提問 / User Question
   └─> Harness 接收使用者輸入（透過 CLI/桌面應用/IDE）
       └─> Harness 將輸入傳送給 Model

2. Model 處理 / Model Processing
   └─> Model 解析系統提示詞和對話歷史
       └─> Model 決定需要呼叫工具（例如：Read、Bash）
           └─> Model 發出工具呼叫請求

3. 工具執行 / Tool Execution
   └─> Harness 接收工具呼叫請求
       └─> Harness 檢查權限（可能需要使用者批准）
           └─> Harness 執行工具（例如：讀取檔案、執行 shell 命令）
               └─> Harness 將工具結果回傳給 Model

4. Model 回應 / Model Response
   └─> Model 解讀工具結果
       └─> Model 生成最終回應
           └─> Harness 將回應呈現給使用者
```

### 架構重要性 / Architectural Significance

**權限管理 / Permission Management:**
- Harness 控制哪些工具可以自動執行，哪些需要使用者批准
- 使用者可以看到工具呼叫的權限模式（sandbox、dangerous 等）

**檔案存取 / File Access:**
- 所有檔案存取都透過 Harness 提供的工具進行
- Harness 可以實施路徑限制和存取控制

**狀態維持 / State Maintenance:**
- Harness 維護對話歷史和工具執行結果
- Harness 負責上下文壓縮以防止超出模型的限制

---

## 5. 工具系統 / Tool System

### 即時載入 vs. 延遲載入 / Eagerly Loaded vs. Deferred Tools

**即時載入的工具 / Eagerly Loaded Tools:**
- 這些工具的定義直接包含在系統提示詞中
- 模型在會話開始時就知道所有即時載入工具的完整 schema
- 可以直接呼叫而無需額外查詢

**延遲載入的工具 / Deferred Tools:**
- 這些工具不在初始系統提示詞中
- 需要透過 `ToolSearch` 機制來發現和載入
- 節省初始上下文空間，但需要額外的查詢步驟

### 工具列表 / Tool Inventory

根據我在系統提示詞中觀察到的內容，以下是所有可用工具的完整列表：

#### 即時載入的工具 / Eagerly Loaded Tools

由於系統提示詞的長度限制，我只能看到部分工具的完整定義。以下是我在系統提示詞中看到的工具：

**1. Agent**
```json
{
  "name": "Agent",
  "description": "Launch a new agent to handle complex, multi-step tasks autonomously.",
  "parameters": {
    "type": "object",
    "properties": {
      "description": {
        "type": "string",
        "description": "A short (3-5 word) description of the task"
      },
      "prompt": {
        "type": "string",
        "description": "The task for the agent to perform"
      },
      "subagent_type": {
        "type": "string",
        "description": "The type of specialized agent to use for this task"
      },
      "model": {
        "type": "string",
        "enum": ["sonnet", "opus", "haiku"],
        "description": "Optional model override"
      },
      "isolation": {
        "type": "string",
        "enum": ["worktree"],
        "description": "Isolation mode"
      },
      "run_in_background": {
        "type": "boolean",
        "description": "Set to true to run this agent in the background"
      }
    },
    "required": ["description", "prompt"]
  }
}
```

**2. AskUserQuestion**
```json
{
  "name": "AskUserQuestion",
  "description": "Use this tool when you need to ask the user questions during execution.",
  "parameters": {
    "type": "object",
    "properties": {
      "questions": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "question": {"type": "string"},
            "header": {"type": "string"},
            "options": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "label": {"type": "string"},
                  "description": {"type": "string"},
                  "preview": {"type": "string"}
                },
                "required": ["label", "description"]
              }
            },
            "multiSelect": {"type": "boolean", "default": false}
          },
          "required": ["question", "header", "options", "multiSelect"]
        },
        "minItems": 1,
        "maxItems": 4
      }
    },
    "required": ["questions"]
  }
}
```

**3. Bash**
```json
{
  "name": "Bash",
  "description": "Executes a given bash command and returns its output.",
  "parameters": {
    "type": "object",
    "properties": {
      "command": {"type": "string"},
      "description": {"type": "string"},
      "timeout": {"type": "number"},
      "run_in_background": {"type": "boolean"},
      "dangerouslyDisableSandbox": {"type": "boolean"}
    },
    "required": ["command"]
  }
}
```

**4. CronCreate**
```json
{
  "name": "CronCreate",
  "description": "Schedule a prompt to be enqueued at a future time.",
  "parameters": {
    "type": "object",
    "properties": {
      "cron": {"type": "string"},
      "prompt": {"type": "string"},
      "recurring": {"type": "boolean", "default": true},
      "durable": {"type": "boolean", "default": false}
    },
    "required": ["cron", "prompt"]
  }
}
```

**5. CronDelete**
```json
{
  "name": "CronDelete",
  "description": "Cancel a cron job previously scheduled with CronCreate.",
  "parameters": {
    "type": "object",
    "properties": {
      "id": {"type": "string"}
    },
    "required": ["id"]
  }
}
```

**6. CronList**
```json
{
  "name": "CronList",
  "description": "List all cron jobs scheduled via CronCreate in this session.",
  "parameters": {
    "type": "object",
    "properties": {}
  }
}
```

**7. Edit**
```json
{
  "name": "Edit",
  "description": "Performs exact string replacements in files.",
  "parameters": {
    "type": "object",
    "properties": {
      "file_path": {"type": "string"},
      "old_string": {"type": "string"},
      "new_string": {"type": "string"},
      "replace_all": {"type": "boolean", "default": false}
    },
    "required": ["file_path", "old_string", "new_string"]
  }
}
```

**8. EnterPlanMode**
```json
{
  "name": "EnterPlanMode",
  "description": "Use this tool proactively when you're about to start a non-trivial implementation task.",
  "parameters": {
    "type": "object",
    "properties": {}
  }
}
```

**9. EnterWorktree**
```json
{
  "name": "EnterWorktree",
  "description": "Use this tool ONLY when the user explicitly asks to work in a worktree.",
  "parameters": {
    "type": "object",
    "properties": {
      "name": {"type": "string"}
    },
    "type": "object"
  }
}
```

**10. ExitPlanMode**
```json
{
  "name": "ExitPlanMode",
  "description": "Use this tool when you are in plan mode and have finished writing your plan.",
  "parameters": {
    "type": "object",
    "properties": {
      "allowedPrompts": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "tool": {"type": "string", "enum": ["Bash"]},
            "prompt": {"type": "string"}
          },
          "required": ["tool", "prompt"]
        }
      }
    },
    "type": "object"
  }
}
```

**11. ExitWorktree**
```json
{
  "name": "ExitWorktree",
  "description": "Exit a worktree session created by EnterWorktree.",
  "parameters": {
    "type": "object",
    "properties": {
      "action": {"type": "string", "enum": ["keep", "remove"]},
      "discard_changes": {"type": "boolean", "default": false}
    },
    "required": ["action"]
  }
}
```

**12. Glob**
```json
{
  "name": "Glob",
  "description": "Fast file pattern matching tool.",
  "parameters": {
    "type": "object",
    "properties": {
      "pattern": {"type": "string"},
      "path": {"type": "string"}
    },
    "required": ["pattern"]
  }
}
```

**13. Grep**
```json
{
  "name": "Grep",
  "description": "A powerful search tool built on ripgrep.",
  "parameters": {
    "type": "object",
    "properties": {
      "pattern": {"type": "string"},
      "path": {"type": "string"},
      "glob": {"type": "string"},
      "type": {"type": "string"},
      "output_mode": {"type": "string", "enum": ["content", "files_with_matches", "count"]},
      "-n": {"type": "boolean"},
      "-i": {"type": "boolean"},
      "multiline": {"type": "boolean"},
      "head_limit": {"type": "number"},
      "offset": {"type": "number"},
      "context": {"type": "number"},
      "-A": {"type": "number"},
      "-B": {"type": "number"},
      "-C": {"type": "number"}
    },
    "required": ["pattern"]
  }
}
```

**14. LSP**
```json
{
  "name": "LSP",
  "description": "Interact with Language Server Protocol (LSP) servers.",
  "parameters": {
    "type": "object",
    "properties": {
      "operation": {"type": "string", "enum": ["goToDefinition", "findReferences", "hover", "documentSymbol", "workspaceSymbol", "goToImplementation", "prepareCallHierarchy", "incomingCalls", "outgoingCalls"]},
      "filePath": {"type": "string"},
      "line": {"type": "integer"},
      "character": {"type": "integer"}
    },
    "required": ["operation", "filePath", "line", "character"]
  }
}
```

**15. NotebookEdit**
```json
{
  "name": "NotebookEdit",
  "description": "Completely replaces the contents of a specific cell in a Jupyter notebook.",
  "parameters": {
    "type": "object",
    "properties": {
      "notebook_path": {"type": "string"},
      "new_source": {"type": "string"},
      "cell_id": {"type": "string"},
      "cell_type": {"type": "string", "enum": ["code", "markdown"]},
      "edit_mode": {"type": "string", "enum": ["replace", "insert", "delete"]}
    },
    "required": ["notebook_path", "new_source"]
  }
}
```

**16. Read**
```json
{
  "name": "Read",
  "description": "Reads a file from the local filesystem.",
  "parameters": {
    "type": "object",
    "properties": {
      "file_path": {"type": "string"},
      "limit": {"type": "number"},
      "offset": {"type": "number"},
      "pages": {"type": "string"}
    },
    "required": ["file_path"]
  }
}
```

**17. RemoteTrigger**
```json
{
  "name": "RemoteTrigger",
  "description": "Call the claude.ai remote-trigger API.",
  "parameters": {
    "type": "object",
    "properties": {
      "action": {"type": "string", "enum": ["list", "get", "create", "update", "run"]},
      "trigger_id": {"type": "string"},
      "body": {"type": "object"}
    },
    "required": ["action"]
  }
}
```

**18. Skill**
```json
{
  "name": "Skill",
  "description": "Execute a skill within the main conversation.",
  "parameters": {
    "type": "object",
    "properties": {
      "skill": {"type": "string"},
      "args": {"type": "string"}
    },
    "required": ["skill"]
  }
}
```

**19. TaskCreate**
```json
{
  "name": "TaskCreate",
  "description": "Use this tool to create a structured task list.",
  "parameters": {
    "type": "object",
    "properties": {
      "subject": {"type": "string"},
      "description": {"type": "string"},
      "activeForm": {"type": "string"},
      "metadata": {"type": "object"}
    },
    "required": ["subject", "description"]
  }
}
```

**20. TaskGet**
```json
{
  "name": "TaskGet",
  "description": "Use this tool to retrieve a task by its ID.",
  "parameters": {
    "type": "object",
    "properties": {
      "taskId": {"type": "string"}
    },
    "required": ["taskId"]
  }
}
```

**21. TaskList**
```json
{
  "name": "TaskList",
  "description": "Use this tool to list all tasks in the task list.",
  "parameters": {
    "type": "object",
    "properties": {}
  }
}
```

**22. TaskOutput**
```json
{
  "name": "TaskOutput",
  "description": "Retrieves output from a running or completed task.",
  "parameters": {
    "type": "object",
    "properties": {
      "task_id": {"type": "string"},
      "block": {"type": "boolean", "default": true},
      "timeout": {"type": "number", "default": 30000}
    },
    "required": ["task_id", "block", "timeout"]
  }
}
```

**23. TaskStop**
```json
{
  "name": "TaskStop",
  "description": "Stops a running background task.",
  "parameters": {
    "type": "object",
    "properties": {
      "task_id": {"type": "string"},
      "shell_id": {"type": "string"}
    },
    "type": "object"
  }
}
```

**24. TaskUpdate**
```json
{
  "name": "TaskUpdate",
  "description": "Use this tool to update a task in the task list.",
  "parameters": {
    "type": "object",
    "properties": {
      "taskId": {"type": "string"},
      "status": {"type": "string"},
      "subject": {"type": "string"},
      "description": {"type": "string"},
      "activeForm": {"type": "string"},
      "owner": {"type": "string"},
      "metadata": {"type": "object"},
      "addBlockedBy": {"type": "array", "items": {"type": "string"}},
      "addBlocks": {"type": "array", "items": {"type": "string"}}
    },
    "required": ["taskId"]
  }
}
```

**25. WebFetch**
```json
{
  "name": "WebFetch",
  "description": "Fetches content from a specified URL and processes it using an AI model.",
  "parameters": {
    "type": "object",
    "properties": {
      "url": {"type": "string", "format": "uri"},
      "prompt": {"type": "string"}
    },
    "required": ["url", "prompt"]
  }
}
```

**26. WebSearch**
```json
{
  "name": "WebSearch",
  "description": "Allows Claude to search the web and use the results.",
  "parameters": {
    "type": "object",
    "properties": {
      "query": {"type": "string", "minLength": 2},
      "allowed_domains": {"type": "array", "items": {"type": "string"}},
      "blocked_domains": {"type": "array", "items": {"type": "string"}}
    },
    "required": ["query"]
  }
}
```

**27. Write**
```json
{
  "name": "Write",
  "description": "Writes a file to the local filesystem.",
  "parameters": {
    "type": "object",
    "properties": {
      "file_path": {"type": "string"},
      "content": {"type": "string"}
    },
    "required": ["file_path", "content"]
  }
}
```

### ToolSearch 查詢示範 / ToolSearch Query Demonstration

由於我無法直接使用 ToolSearch 工具（它不在我的可用工具列表中），我將說明如何找到特定工具：

**ToolSearch 查詢策略 / ToolSearch Query Strategies:**

| **工具 / Tool** | **查詢關鍵字 / Query Keywords** |
|-----------------|--------------------------------|
| Read | "read file", "view file", "cat" |
| Write | "write file", "create file", "save" |
| Edit | "edit file", "replace", "modify" |
| Bash | "shell command", "execute", "run command" |
| Glob | "find files", "file pattern", "glob" |
| Grep | "search content", "grep", "ripgrep" |
| Git | "git commit", "git status", "git push" |
| LSP | "go to definition", "find references" |

---

## 6. 工具規格 / Tool Specifications

### 工具規格格式 / Tool Specification Format

**規格來源 / Specification Origin:**
- 工具遵循 JSON Schema 格式
- 這是 Anthropic 定義的工具格式，與 OpenAI 的 Function Calling 格式相似但有差異

### Anthropic vs. OpenAI 工具格式比較 / Comparison

**Anthropic 工具格式：**
```json
{
  "name": "tool_name",
  "description": "Tool description",
  "parameters": {
    "type": "object",
    "properties": {...},
    "required": [...]
  }
}
```

**OpenAI Function Calling 格式：**
```json
{
  "name": "function_name",
  "description": "Function description",
  "parameters": {
    "type": "object",
    "properties": {...},
    "required": [...]
  }
}
```

**關鍵差異 / Key Differences:**
- Anthropic 使用 `"name"` 和 `"description"` 在工具層級
- OpenAI 將這些欄位放在 `"function"` 物件內
- 兩者在 `parameters` 層級都使用 JSON Schema

### MCP (Model Context Protocol)

**什麼是 MCP / What is MCP:**
MCP 是一個標準化協議，用於：
- 統一工具定義和發現機制
- 允許外部服務以標準化方式暴露工具
- 促進工具在不同 AI 平台之間的互通性

**與工具標準化的關係 / Relationship with Tool Standardization:**
- MCP 提供了一個通用語言來描述工具
- 工具提供者可以一次實作，多個 AI 平台可以使用
- 減少了平台特定的工具整合工作

---

## 7. 命令 / Commands

### 命令 vs. 工具 vs. 技能 / Commands vs. Tools vs. Skills

**命令（Commands）：**
- 以 `/` 開頭的使用者輸入（例如：`/help`、`/commit`）
- 由 Harness 在模型處理之前攔截和執行
- 通常執行預定義的操作或啟動技能

**工具（Tools）：**
- 模型可以呼叫的函式（例如：`Read`、`Write`、`Bash`）
- 需要明确的工具呼叫語法
- 由 Harness 執行並將結果回傳給模型

**技能（Skills）：**
- 儲存在 `~/.claude/skills/` 目錄中的可重用工作流程
- 透過 `Skill` 工具或 `/skill-name` 命令觸發
- 可以包含複雜的多步驟指令

### 已知命令 / Known Commands

根據系統提示詞和我觀察到的內容：

| **命令 / Command** | **描述 / Description** |
|--------------------|------------------------|
| `/help` | 獲取 Claude Code 使用說明 |
| `/clear` | 清除對話歷史 |
| `/commit` | 提交變更（技能） |
| `/review-pr` | 審查 Pull Request（技能） |
| `/fast` | 切換快速模式 |
| `/loop` | 定期執行命令（技能） |
| `/tasks` | 查看任務列表 |

**盲點 / Blind Spots:**
- 我無法完整列出所有可用命令，因為命令列表可能動態變化
- 某些命令可能只在特定上下文中可用
- 使用者自訂命令可能不在我的知識範圍內

### 命令執行時的模型知情權 / Model Awareness During Command Execution

**模型不知道 / Model is Not Aware:**
- 當命令被執行時，我（模型）通常不知道命令的具體執行細節
- 命令執行結果可能以系統訊息的形式呈現給我

**結果傳達方式 / Result Communication:**
- 命令執行結果可能顯示為系統提醒（`<system-reminder>`）
- 某些命令可能直接修改環境而不通知我

### 區分命令和技能 / Distinguishing Commands and Skills

**當使用者輸入 `/something` 時：**

1. **技能（Skills）：**
   - 如果 `/something` 對應到 `~/.claude/skills/` 目錄中的技能
   - 技能內容會被載入並取代使用者的原始輸入
   - 我會看到技能的完整提示詞

2. **命令（Commands）：**
   - 如果是內建命令（如 `/help`、`/clear`）
   - Harness 會直接執行而不打擾我
   - 我可能只會看到執行結果或完全不知道

3. **判斷方式 / How to Determine:**
   - 檢查系統提示詞中的技能列表
   - 如果技能名稱與 `/something` 匹配，則為技能
   - 否則可能是內建命令

---

## 總結 / Summary

### 關鍵發現 / Key Findings

1. **Harness 是核心基礎設施 / Harness is Core Infrastructure**
   - 負責所有外部互動（檔案系統、Git、Shell）
   - 管理上下文和會話狀態

2. **模型負責決策 / Model is Responsible for Decisions**
   - 決定何時呼叫哪些工具
   - 解讀工具結果並生成回應

3. **技能系統提供可擴展性 / Skill System Provides Extensibility**
   - 允許使用者定義自訂工作流程
   - 技能內容在執行時載入

4. **環境上下文是快照 / Environment Context is Snapshot**
   - Git 狀態在會話開始時捕獲
   - 需要手動刷新以獲取最新狀態

### 盲點和限制 / Blind Spots and Limitations

1. **ToolSearch 不可用 / ToolSearch Not Available**
   - 我無法直接查詢可用工具列表
   - 只能看到系統提示詞中定義的工具

2. **命令執行不透明 / Command Execution Opaque**
   - 不知道所有可用命令的完整列表
   - 命令執行細節由 Harness 處理

3. **動態配置未知 / Dynamic Configuration Unknown**
   - 使用者可能透過 settings.json 配置行為
   - 這些配置可能影響工具行為但我不一定知道

---

*本報告基於 2026-03-30 的系統提示詞內容生成*
*This report was generated based on system prompt content as of 2026-03-30*
