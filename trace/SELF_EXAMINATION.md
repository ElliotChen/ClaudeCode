# Claude Code 架構 - 自我檢視報告
# Claude Code Architecture - Self-Examination Report

> 產生日期 / Generated: 2026-03-30
> 模型 / Model: Claude Opus 4.6 (claude-opus-4-6[1m])
> 工作目錄 / Working Directory: /Users/elliot/IdeaProjects/cctest

---

## 1. 啟動流程 / Startup Flow

### 完整啟動流程 / Complete Startup Process

啟動流程分為多個階段，每個階段由 Harness 或 Model 負責：

The startup flow consists of multiple phases, each handled by either the Harness or the Model:

**階段 1：Harness 初始化（Harness 負責）/ Phase 1: Harness Initialization (Harness)**

1. **啟動 CLI 程序** — Harness 啟動，解析命令列參數，載入設定檔。
   The CLI process starts, parses command-line arguments, loads configuration files.

2. **環境偵測** — Harness 偵測作業系統（darwin）、Shell（zsh）、Git 狀態、工作目錄等。
   Environment detection — OS (darwin), shell (zsh), git status, working directory, etc.

3. **組裝系統提示詞** — Harness 將固定指令、工具定義、環境上下文、CLAUDE.md 內容、記憶體索引等組裝為系統提示詞。
   System prompt assembly — static instructions, tool definitions, environment context, CLAUDE.md contents, memory index are assembled into the system prompt.

4. **載入工具定義** — 即時載入工具（Bash, Edit, Glob, Grep, Read, Write, Agent, Skill, ToolSearch）的完整 schema 被嵌入系統提示詞；延遲載入工具僅以名稱列表形式提供。
   Tool definitions loaded — eagerly loaded tools get full schemas embedded; deferred tools appear as name-only lists.

5. **載入 Hooks** — 如果使用者在 settings.json 中配置了 hooks，Harness 會在對應事件觸發時執行。
   Hooks loaded — if configured in settings.json, Harness executes them at corresponding events.

6. **執行 SessionStart hook** — 在對話開始時觸發，注入額外上下文（如 superpowers skill 的內容）。
   SessionStart hook fires — injects additional context (e.g., superpowers skill content).

**階段 2：首次 API 呼叫（Harness 負責）/ Phase 2: First API Call (Harness)**

7. **發送 API 請求** — Harness 將組裝好的系統提示詞 + 使用者訊息發送至 Anthropic API。
   API request sent — Harness sends assembled system prompt + user message to Anthropic API.

**階段 3：模型處理（Model 負責）/ Phase 3: Model Processing (Model)**

8. **接收並解析系統提示詞** — 我（模型）接收完整的系統提示詞，理解可用工具、行為準則、環境上下文。
   Receive and parse system prompt — I (the model) receive the full system prompt, understand available tools, behavioral guidelines, environment context.

9. **處理 SessionStart hook 注入的內容** — 我看到 superpowers 的 `using-superpowers` skill 內容，了解需要在回應前檢查是否有適用的 skill。
   Process SessionStart hook injections — I see the superpowers `using-superpowers` skill content and understand the skill-checking requirement.

10. **生成回應或工具呼叫** — 我開始回應使用者的請求。
    Generate response or tool calls — I begin responding to the user's request.

**階段 4：工具執行循環（Harness + Model 協作）/ Phase 4: Tool Execution Loop (Harness + Model)**

11. 如果我發出工具呼叫，Harness 負責執行（權限檢查 → 執行 → 回傳結果）。我再根據結果繼續處理。
    If I issue tool calls, Harness handles execution (permission check → execute → return result). I continue processing based on results.

### 盲點 / Blind Spots

我無法觀察到：Harness 內部的設定檔解析邏輯、API 金鑰管理、使用者權限模式的具體實作、context window 壓縮的觸發時機與演算法。

I cannot observe: Harness's internal config parsing logic, API key management, user permission mode implementation details, context window compression triggers and algorithms.

---

## 2. 系統提示詞組成 / System Prompt Composition

### 系統提示詞種類 / Types of System Prompt Content

根據我在當前對話中實際觀察到的內容，系統提示詞由以下部分組成：

Based on what I actually observe in the current conversation, the system prompt consists of:

| 種類 / Type | 提供者 / Provider | 說明 / Description |
|---|---|---|
| **核心行為指令** | Anthropic（硬編碼 / Hardcoded） | 安全準則、工具使用規範、輸出風格、Git 操作規則、PR 建立流程等。這些是固定的，使用者無法直接修改。 Core behavioral instructions — safety guidelines, tool usage rules, output style, git operation rules, PR creation flows. These are fixed and cannot be directly modified by users. |
| **工具定義** | Anthropic（Harness 注入 / Harness-injected） | 即時載入工具的完整 JSON Schema 定義。 Full JSON Schema definitions for eagerly loaded tools. |
| **延遲工具列表** | Anthropic（Harness 注入 / Harness-injected） | 延遲載入工具的名稱列表，在 `<system-reminder>` 中提供。 Deferred tool name lists provided in `<system-reminder>` tags. |
| **環境上下文** | Harness 偵測 / Harness-detected | 工作目錄、平台、Shell、OS 版本、Git 狀態、模型資訊等。 Working directory, platform, shell, OS version, git status, model info, etc. |
| **CLAUDE.md 指令** | 使用者 / User | 來自 `~/.claude/CLAUDE.md`（全域）和專案級 CLAUDE.md 的自訂指令。 Custom instructions from `~/.claude/CLAUDE.md` (global) and project-level CLAUDE.md. |
| **記憶體索引** | 使用者 + 模型 / User + Model | `MEMORY.md` 的內容被注入到上下文中。 Contents of `MEMORY.md` injected into context. |
| **技能內容** | 使用者自訂 / User-defined | 透過 SessionStart hook 或 Skill 工具動態載入的技能內容。 Skill content dynamically loaded via SessionStart hook or Skill tool. |
| **日期上下文** | Harness 注入 / Harness-injected | `currentDate` 標籤提供今天的日期。 `currentDate` tag provides today's date. |

### 工具定義和配置的存放位置 / Where Tool Definitions and Configurations are Stored

工具定義嵌入在系統提示詞中，以 `<functions>` 區塊的形式呈現。使用者**無法**直接存取或修改內建工具的定義檔案。然而，使用者可以透過 **MCP (Model Context Protocol)** 伺服器來新增自訂工具。

Tool definitions are embedded in the system prompt as `<functions>` blocks. Users **cannot** directly access or modify built-in tool definition files. However, users can add custom tools via **MCP (Model Context Protocol)** servers.

### 使用者能否新增自己的工具 / Can Users Add Their Own Tools

**可以，透過 MCP。** 使用者可以配置 MCP 伺服器，提供額外的工具。這些工具會出現在系統提示詞中，與內建工具並列。系統提示詞中提到：「IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead of this one」，這證實 MCP 工具與內建工具共存。

**Yes, via MCP.** Users can configure MCP servers to provide additional tools. These appear alongside built-in tools. The system prompt mentions: "IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead of this one," confirming MCP tools coexist with built-in tools.

### 嵌入的行為準則 / Embedded Behavioral Guidelines

以下是我在系統提示詞中觀察到的主要行為準則（直接引用）：

The following are the key behavioral guidelines I observe in my system prompt (direct quotes):

1. **安全準則 / Safety**: `"Assist with authorized security testing, defensive security, CTF challenges, and educational contexts. Refuse requests for destructive techniques..."`
2. **工具優先 / Tool Priority**: `"Do NOT use the Bash to run commands when a relevant dedicated tool is provided."`
3. **輸出效率 / Output Efficiency**: `"Go straight to the point. Try the simplest approach first without going in circles. Do not overdo it. Be extra concise."`
4. **謹慎操作 / Careful Actions**: `"Carefully consider the reversibility and blast radius of actions."`
5. **最小變更 / Minimal Changes**: `"Don't add features, refactor code, or make 'improvements' beyond what was asked."`
6. **Git 安全 / Git Safety**: `"NEVER update the git config"`, `"NEVER run destructive git commands... unless the user explicitly requests"`, `"NEVER skip hooks"`, `"Always create NEW commits rather than amending"`
7. **不主動提交 / No Proactive Commits**: `"NEVER commit changes unless the user explicitly asks you to."`

使用者可以透過 CLAUDE.md 檔案覆寫部分行為（系統提示詞明確說明 CLAUDE.md 的指令會 OVERRIDE 預設行為）：`"IMPORTANT: These instructions OVERRIDE any default behavior and you MUST follow them exactly as written."`

Users can override some behaviors via CLAUDE.md files (the system prompt explicitly states CLAUDE.md instructions OVERRIDE default behavior): `"IMPORTANT: These instructions OVERRIDE any default behavior and you MUST follow them exactly as written."`

### Git 操作規則及覆寫方式 / Git Operation Rules and Override Methods

Git 操作規則是硬編碼在核心系統提示詞中的。雖然使用者可以透過 CLAUDE.md 指令覆寫某些行為（因為 CLAUDE.md 的優先級高於預設行為），但核心安全規則（如不能 force push 到 main/master）的覆寫需要使用者在對話中**明確請求**。

Git operation rules are hardcoded in the core system prompt. While users can override some behaviors via CLAUDE.md (since CLAUDE.md has higher priority than defaults), core safety rules (like no force-push to main/master) require **explicit user request** in the conversation to override.

### 如何取得固定部分的系統提示詞 / How to Obtain the Fixed System Prompt

固定部分的系統提示詞是由 Harness 在啟動時組裝並發送至 API 的。使用者無法直接取得完整的系統提示詞檔案。然而：

The fixed system prompt is assembled by the Harness at startup and sent to the API. Users cannot directly obtain the complete system prompt file. However:

- 我（模型）可以在對話中引用我觀察到的系統提示詞內容（如本文件所示）。
  I (the model) can quote system prompt content I observe in conversation (as demonstrated in this document).
- 核心指令可能存在於 Claude Code 的原始碼中（Claude Code 是開源的，可在 GitHub 上查看）。
  Core instructions likely exist in Claude Code's source code (Claude Code is open-source and viewable on GitHub).
- 使用者可以透過這種「自我檢視」的方式讓模型報告它所看到的系統提示詞。
  Users can use this "self-examination" approach to have the model report what it sees.

---

## 3. 環境上下文注入 / Environment Context Injection

### 環境上下文類型 / Environment Context Types

根據我在 `# Environment` 區段中觀察到的內容：

Based on what I observe in the `# Environment` section:

| 上下文 / Context | 值 / Value | 偵測方式 / Detection Method |
|---|---|---|
| Primary working directory | `/Users/elliot/IdeaProjects/cctest` | Harness 偵測 CWD / Harness detects CWD |
| Is a git repository | `true` | Harness 檢查 `.git` 目錄 / Harness checks `.git` directory |
| Platform | `darwin` | Harness 讀取 OS 資訊 / Harness reads OS info |
| Shell | `zsh` | Harness 讀取 `$SHELL` 或類似機制 / Harness reads `$SHELL` or similar |
| OS Version | `Darwin 25.4.0` | Harness 讀取 `uname` 或類似機制 / Harness reads `uname` or similar |
| Model info | `claude-opus-4-6[1m]` | Harness 知道它呼叫的模型 / Harness knows which model it's calling |
| Knowledge cutoff | `May 2025` | 硬編碼於模型配置中 / Hardcoded in model configuration |
| Claude model family info | Claude 4.5/4.6, model IDs | 硬編碼 / Hardcoded |
| Claude Code availability | CLI, desktop app, web app, IDE extensions | 硬編碼 / Hardcoded |
| Fast mode info | Same Opus 4.6 model with faster output | 硬編碼 / Hardcoded |
| currentDate | `2026-03-30` | Harness 注入當前日期 / Harness injects current date |

### 是否為預先注入 / Whether Pre-injected

**是的，全部都是預先注入的。** 這些環境上下文在 API 請求發送前就已經由 Harness 組裝好。我（模型）不需要也無法自行偵測這些環境資訊 — 它們在我收到的第一條訊息中就已經存在。

**Yes, all are pre-injected.** These environment contexts are assembled by the Harness before the API request is sent. I (the model) don't need to and cannot detect these environment details on my own — they are already present in the first message I receive.

### Git 狀態：即時還是快照 / Git Status: Real-time or Snapshot

**快照。** 系統提示詞明確說明：

**Snapshot.** The system prompt explicitly states:

> `"gitStatus: This is the git status at the start of the conversation. Note that this status is a snapshot in time, and will not update during the conversation."`

這有以下影響 / This has the following implications:

1. **我看到的 Git 狀態可能已過時** — 如果我在對話中執行了 `git commit`、`git checkout` 等操作，初始快照不會反映這些變更。
   The git status I see may be outdated — if I execute `git commit`, `git checkout`, etc. during the conversation, the initial snapshot won't reflect these changes.

2. **我需要主動查詢最新狀態** — 在執行 Git 操作前，我應該使用 `git status`、`git log` 等命令來獲取最新狀態，而非依賴初始快照。
   I need to actively query current state — before git operations, I should use `git status`, `git log`, etc. to get current state rather than relying on the initial snapshot.

3. **初始快照仍有價值** — 它讓我在對話開始時就能了解工作環境的概況，不需要額外的工具呼叫。
   The initial snapshot is still valuable — it gives me an overview of the working environment at conversation start without extra tool calls.

初始快照內容 / Initial snapshot content:
```
Current branch: feature/spring-modulith-hr-system
Main branch: main
Status:
?? .claude/
?? .factorypath
?? claude-code-architecture/
?? selftalk.md
?? workspace/

Recent commits:
212d67f feat: add integration tests and modularity verification
af921b7 refactor: improve module boundaries and fix event handling
51679fd feat: add DepartmentController with OpenAPI and global exception handler
781de9a feat: add DepartmentService with event publishing and unit tests
41736df feat: add Department entity, repository, and DTOs
```

---

## 4. Harness

### 什麼是 Claude Code Harness / What is the Claude Code Harness

**Claude Code Harness** 是一個在使用者本地機器上運行的 CLI 應用程式（Node.js/TypeScript），它作為使用者與 Claude 模型之間的**中介層**。具體來說：

**Claude Code Harness** is a CLI application (Node.js/TypeScript) running on the user's local machine that acts as an **intermediary layer** between the user and the Claude model. Specifically:

- 它是一個**本地程序**，管理 API 通信、工具執行、權限控制和使用者介面。
  It is a **local process** managing API communication, tool execution, permission control, and user interface.
- 它**不是**模型本身 — 模型（我）運行在 Anthropic 的伺服器上。
  It is **not** the model itself — the model (me) runs on Anthropic's servers.
- 它是「Claude Code」這個產品中，除了模型之外的所有基礎設施。
  It is all the infrastructure in "Claude Code" the product, apart from the model itself.

### Harness 與 Model 的關係 / Harness-Model Relationship

| 職責 / Responsibility | Harness | Model（我 / Me） |
|---|---|---|
| 系統提示詞組裝 / System prompt assembly | ✅ | ❌ |
| 環境偵測 / Environment detection | ✅ | ❌ |
| API 通信 / API communication | ✅ | ❌ |
| 工具定義提供 / Tool definition provision | ✅ | ❌ |
| 工具呼叫決策 / Tool call decision | ❌ | ✅ |
| 工具執行 / Tool execution | ✅ | ❌ |
| 權限檢查與使用者確認 / Permission check & user confirmation | ✅ | ❌ |
| 回應生成 / Response generation | ❌ | ✅ |
| 上下文壓縮 / Context compression | ✅ | ❌ |
| Hook 執行 / Hook execution | ✅ | ❌ |
| 使用者介面渲染 / UI rendering | ✅ | ❌ |
| CLAUDE.md 載入 / CLAUDE.md loading | ✅ | ❌ |
| 記憶體檔案載入 / Memory file loading | ✅ | ❌ |
| 推理與規劃 / Reasoning & planning | ❌ | ✅ |
| 程式碼理解 / Code understanding | ❌ | ✅ |

### 完整互動循環 / Complete Interaction Loop

以一個具體例子說明：使用者問「幫我修改 README.md 的標題」

Illustrated with a concrete example: User asks "Help me change the title in README.md"

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. 使用者輸入 / User Input                                        │
│    User types: "幫我修改 README.md 的標題"                         │
│    → Harness 捕獲輸入                                             │
│    → Harness captures input                                      │
├──────────────────────────────────────────────────────────────────┤
│ 2. Harness 發送 API 請求 / Harness Sends API Request              │
│    Harness 將使用者訊息 + 系統提示詞 + 對話歷史發送至 Anthropic API    │
│    Harness sends user message + system prompt + history to API    │
├──────────────────────────────────────────────────────────────────┤
│ 3. Model 處理 / Model Processing                                  │
│    我收到訊息，決定需要先讀取檔案                                     │
│    I receive the message, decide I need to read the file first    │
│    我輸出: tool_use → Read(file_path="/path/to/README.md")        │
│    I output: tool_use → Read(file_path="/path/to/README.md")     │
├──────────────────────────────────────────────────────────────────┤
│ 4. Harness 執行工具 / Harness Executes Tool                       │
│    Harness 收到工具呼叫請求                                        │
│    Harness receives tool call request                             │
│    → 檢查權限（Read 通常自動允許）                                   │
│    → Checks permissions (Read usually auto-allowed)               │
│    → 執行檔案讀取                                                  │
│    → Executes file read                                           │
│    → 將結果作為 tool_result 發回 API                                │
│    → Returns result as tool_result back to API                    │
├──────────────────────────────────────────────────────────────────┤
│ 5. Model 處理結果 / Model Processes Result                        │
│    我收到檔案內容，決定使用 Edit 工具修改標題                          │
│    I receive file content, decide to use Edit tool to change title│
│    我輸出: tool_use → Edit(file_path, old_string, new_string)     │
├──────────────────────────────────────────────────────────────────┤
│ 6. Harness 執行工具 / Harness Executes Tool                       │
│    Harness 收到 Edit 呼叫                                         │
│    Harness receives Edit call                                     │
│    → 檢查權限（可能需要使用者確認）                                   │
│    → Checks permissions (may need user confirmation)              │
│    → 執行字串替換                                                  │
│    → Executes string replacement                                  │
│    → 回傳結果                                                     │
│    → Returns result                                               │
├──────────────────────────────────────────────────────────────────┤
│ 7. Model 生成最終回應 / Model Generates Final Response             │
│    我確認修改成功，輸出文字回應給使用者                                │
│    I confirm the edit succeeded, output text response to user     │
├──────────────────────────────────────────────────────────────────┤
│ 8. Harness 渲染回應 / Harness Renders Response                    │
│    Harness 將我的文字回應渲染在終端機上，供使用者閱讀                   │
│    Harness renders my text response in the terminal for the user  │
└──────────────────────────────────────────────────────────────────┘
```

### 為什麼這個架構重要 / Why This Architecture Matters

1. **權限控制 / Permission Control** — 因為 Harness 負責執行工具，它可以在執行前攔截並要求使用者確認。我（模型）無法繞過這個機制直接存取檔案系統。
   Because Harness executes tools, it can intercept and require user confirmation before execution. I (the model) cannot bypass this mechanism to directly access the filesystem.

2. **檔案存取 / File Access** — 我不直接讀寫檔案。所有檔案操作都透過 Harness 的工具抽象層執行。Harness 可以實施沙箱限制。
   I don't directly read/write files. All file operations go through Harness's tool abstraction layer. Harness can enforce sandbox restrictions.

3. **狀態維持 / State Persistence** — 我（模型）是無狀態的 — 每次 API 呼叫都是獨立的。Harness 負責維持對話歷史、工作目錄狀態、背景任務等。系統提示詞提到：「The system will automatically compress prior messages in your conversation as it approaches context limits」— 這是 Harness 的工作。
   I (the model) am stateless — each API call is independent. Harness maintains conversation history, working directory state, background tasks, etc. The system prompt mentions: "The system will automatically compress prior messages..." — this is Harness's job.

4. **安全邊界 / Security Boundary** — Harness 是安全邊界。即使我（模型）被提示注入攻擊誘導輸出惡意工具呼叫，Harness 的權限系統仍然可以阻止執行。
   Harness is the security boundary. Even if I (the model) am tricked by prompt injection into outputting malicious tool calls, Harness's permission system can still block execution.

---

## 5. 工具系統 / Tool System

### 即時載入 vs 延遲載入 / Eagerly Loaded vs Deferred Tools

**即時載入工具（Eagerly Loaded Tools）** 的完整 JSON Schema 定義在系統提示詞中直接提供。我可以立即使用它們，不需要額外步驟。

**Eagerly loaded tools** have their full JSON Schema definitions provided directly in the system prompt. I can use them immediately without additional steps.

**延遲載入工具（Deferred Tools）** 只以名稱列表的形式出現在 `<system-reminder>` 中。要使用它們，我必須先呼叫 `ToolSearch` 來取得完整的 schema 定義。

**Deferred tools** appear only as name lists in `<system-reminder>` tags. To use them, I must first call `ToolSearch` to fetch their full schema definitions.

這種設計的原因是**節省 token** — 將所有工具的完整 schema 都放在系統提示詞中會消耗大量 context window。常用工具即時載入，不常用工具按需載入。

The reason for this design is **token efficiency** — embedding all tool schemas in the system prompt would consume significant context window. Frequently used tools are eagerly loaded; less common tools are loaded on demand.

### 即時載入工具完整列表 / Complete List of Eagerly Loaded Tools

以下是所有即時載入工具的完整 JSON Schema 定義：

Below are the complete JSON Schema definitions for all eagerly loaded tools:

#### 5.1 Agent

```json
{
  "name": "Agent",
  "description": "Launch a new agent to handle complex, multi-step tasks autonomously. The Agent tool launches specialized agents (subprocesses) that autonomously handle complex tasks. Each agent type has specific capabilities and tools available to it. Available agent types: general-purpose, statusline-setup, Explore, Plan, claude-code-guide, superpowers:code-reviewer.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["description", "prompt"],
    "additionalProperties": false,
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
        "description": "Optional model override for this agent."
      },
      "run_in_background": {
        "type": "boolean",
        "description": "Set to true to run this agent in the background."
      },
      "isolation": {
        "type": "string",
        "enum": ["worktree"],
        "description": "Isolation mode. 'worktree' creates a temporary git worktree."
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"agent launch subprocess"` 或 `"select:Agent"` — 但 Agent 是即時載入的，通常不需要透過 ToolSearch 查詢。
**ToolSearch query**: `"agent launch subprocess"` or `"select:Agent"` — but Agent is eagerly loaded, so ToolSearch is typically unnecessary.

#### 5.2 Bash

```json
{
  "name": "Bash",
  "description": "Executes a given bash command and returns its output. The working directory persists between commands, but shell state does not.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["command"],
    "additionalProperties": false,
    "properties": {
      "command": {
        "type": "string",
        "description": "The command to execute"
      },
      "description": {
        "type": "string",
        "description": "Clear, concise description of what this command does in active voice."
      },
      "timeout": {
        "type": "number",
        "description": "Optional timeout in milliseconds (max 600000)"
      },
      "run_in_background": {
        "type": "boolean",
        "description": "Set to true to run this command in the background."
      },
      "dangerouslyDisableSandbox": {
        "type": "boolean",
        "description": "Set this to true to dangerously override sandbox mode."
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"bash shell command execute"`

#### 5.3 Edit

```json
{
  "name": "Edit",
  "description": "Performs exact string replacements in files.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["file_path", "old_string", "new_string"],
    "additionalProperties": false,
    "properties": {
      "file_path": {
        "type": "string",
        "description": "The absolute path to the file to modify"
      },
      "old_string": {
        "type": "string",
        "description": "The text to replace"
      },
      "new_string": {
        "type": "string",
        "description": "The text to replace it with (must be different from old_string)"
      },
      "replace_all": {
        "type": "boolean",
        "default": false,
        "description": "Replace all occurrences of old_string (default false)"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"edit file replace string"`

#### 5.4 Glob

```json
{
  "name": "Glob",
  "description": "Fast file pattern matching tool that works with any codebase size. Supports glob patterns like '**/*.js'. Returns matching file paths sorted by modification time.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["pattern"],
    "additionalProperties": false,
    "properties": {
      "pattern": {
        "type": "string",
        "description": "The glob pattern to match files against"
      },
      "path": {
        "type": "string",
        "description": "The directory to search in. Omit for current working directory."
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"glob file pattern find"`

#### 5.5 Grep

```json
{
  "name": "Grep",
  "description": "A powerful search tool built on ripgrep. Supports full regex syntax, file filtering, and multiple output modes.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["pattern"],
    "additionalProperties": false,
    "properties": {
      "pattern": {
        "type": "string",
        "description": "The regular expression pattern to search for"
      },
      "path": {
        "type": "string",
        "description": "File or directory to search in"
      },
      "output_mode": {
        "type": "string",
        "enum": ["content", "files_with_matches", "count"],
        "description": "Output mode. Defaults to 'files_with_matches'."
      },
      "glob": {
        "type": "string",
        "description": "Glob pattern to filter files"
      },
      "type": {
        "type": "string",
        "description": "File type to search (e.g., js, py, rust)"
      },
      "-i": {
        "type": "boolean",
        "description": "Case insensitive search"
      },
      "-n": {
        "type": "boolean",
        "description": "Show line numbers. Defaults to true."
      },
      "-A": {
        "type": "number",
        "description": "Lines to show after each match"
      },
      "-B": {
        "type": "number",
        "description": "Lines to show before each match"
      },
      "-C": {
        "type": "number",
        "description": "Alias for context"
      },
      "context": {
        "type": "number",
        "description": "Lines to show before and after each match"
      },
      "head_limit": {
        "type": "number",
        "description": "Limit output to first N lines/entries. Defaults to 250."
      },
      "offset": {
        "type": "number",
        "description": "Skip first N lines/entries. Defaults to 0."
      },
      "multiline": {
        "type": "boolean",
        "description": "Enable multiline mode. Default: false."
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"grep search content ripgrep"`

#### 5.6 Read

```json
{
  "name": "Read",
  "description": "Reads a file from the local filesystem. Supports text files, images, PDFs, and Jupyter notebooks.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["file_path"],
    "additionalProperties": false,
    "properties": {
      "file_path": {
        "type": "string",
        "description": "The absolute path to the file to read"
      },
      "offset": {
        "type": "number",
        "description": "The line number to start reading from"
      },
      "limit": {
        "type": "number",
        "description": "The number of lines to read"
      },
      "pages": {
        "type": "string",
        "description": "Page range for PDF files (e.g., '1-5')"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"read file content"`

#### 5.7 Write

```json
{
  "name": "Write",
  "description": "Writes a file to the local filesystem. Overwrites existing files. Requires prior Read for existing files.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["file_path", "content"],
    "additionalProperties": false,
    "properties": {
      "file_path": {
        "type": "string",
        "description": "The absolute path to the file to write (must be absolute)"
      },
      "content": {
        "type": "string",
        "description": "The content to write to the file"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"write file create"`

#### 5.8 Skill

```json
{
  "name": "Skill",
  "description": "Execute a skill within the main conversation. When users ask you to perform tasks, check if any of the available skills match.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["skill"],
    "additionalProperties": false,
    "properties": {
      "skill": {
        "type": "string",
        "description": "The skill name. E.g., 'commit', 'review-pr', or 'pdf'"
      },
      "args": {
        "type": "string",
        "description": "Optional arguments for the skill"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"skill invoke execute"`

#### 5.9 ToolSearch

```json
{
  "name": "ToolSearch",
  "description": "Fetches full schema definitions for deferred tools so they can be called. Supports exact selection ('select:Read,Edit') and keyword search.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["query", "max_results"],
    "additionalProperties": false,
    "properties": {
      "query": {
        "type": "string",
        "description": "Query to find deferred tools. Use 'select:<tool_name>' for direct selection, or keywords to search."
      },
      "max_results": {
        "type": "number",
        "default": 5,
        "description": "Maximum number of results to return (default: 5)"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: 此工具本身就是用來搜尋其他工具的，不需要透過 ToolSearch 查詢。
This tool itself is for searching other tools; it doesn't need to be found via ToolSearch.

### 延遲載入工具完整列表 / Complete List of Deferred Tools

以下是所有延遲載入工具的完整 JSON Schema 定義（透過 ToolSearch 取得）：

Below are the complete JSON Schema definitions for all deferred tools (fetched via ToolSearch):

#### 5.10 AskUserQuestion

```json
{
  "name": "AskUserQuestion",
  "description": "Use this tool when you need to ask the user questions during execution. Allows gathering preferences, clarifying instructions, getting decisions, and offering choices.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["questions"],
    "additionalProperties": false,
    "properties": {
      "questions": {
        "type": "array",
        "minItems": 1,
        "maxItems": 4,
        "description": "Questions to ask the user (1-4 questions)",
        "items": {
          "type": "object",
          "required": ["question", "header", "options", "multiSelect"],
          "additionalProperties": false,
          "properties": {
            "question": {
              "type": "string",
              "description": "The complete question to ask the user"
            },
            "header": {
              "type": "string",
              "description": "Very short label displayed as a chip/tag (max 12 chars)"
            },
            "options": {
              "type": "array",
              "minItems": 2,
              "maxItems": 4,
              "items": {
                "type": "object",
                "required": ["label", "description"],
                "additionalProperties": false,
                "properties": {
                  "label": {
                    "type": "string",
                    "description": "Display text for this option (1-5 words)"
                  },
                  "description": {
                    "type": "string",
                    "description": "Explanation of what this option means"
                  },
                  "preview": {
                    "type": "string",
                    "description": "Optional preview content for visual comparison"
                  }
                }
              }
            },
            "multiSelect": {
              "type": "boolean",
              "default": false,
              "description": "Allow multiple selections"
            }
          }
        }
      },
      "answers": {
        "type": "object",
        "additionalProperties": { "type": "string" },
        "description": "User answers collected by the permission component"
      },
      "annotations": {
        "type": "object",
        "additionalProperties": {
          "type": "object",
          "properties": {
            "notes": { "type": "string" },
            "preview": { "type": "string" }
          }
        },
        "description": "Optional per-question annotations from the user"
      },
      "metadata": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "source": {
            "type": "string",
            "description": "Optional identifier for analytics tracking"
          }
        }
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:AskUserQuestion"` 或 `"ask user question"`

#### 5.11 TaskCreate

```json
{
  "name": "TaskCreate",
  "description": "Create a structured task list for your current coding session. Helps track progress and organize complex tasks.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["subject", "description"],
    "additionalProperties": false,
    "properties": {
      "subject": {
        "type": "string",
        "description": "A brief title for the task"
      },
      "description": {
        "type": "string",
        "description": "What needs to be done"
      },
      "activeForm": {
        "type": "string",
        "description": "Present continuous form shown in spinner when in_progress"
      },
      "metadata": {
        "type": "object",
        "additionalProperties": {},
        "description": "Arbitrary metadata to attach to the task"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:TaskCreate"` 或 `"task create todo"`

#### 5.12 TaskUpdate

```json
{
  "name": "TaskUpdate",
  "description": "Update a task in the task list. Mark tasks as resolved, delete, update details, or set dependencies.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["taskId"],
    "additionalProperties": false,
    "properties": {
      "taskId": { "type": "string", "description": "The ID of the task to update" },
      "status": {
        "description": "New status for the task",
        "anyOf": [
          { "type": "string", "enum": ["pending", "in_progress", "completed"] },
          { "type": "string", "const": "deleted" }
        ]
      },
      "subject": { "type": "string", "description": "New subject for the task" },
      "description": { "type": "string", "description": "New description for the task" },
      "activeForm": { "type": "string", "description": "Present continuous form shown in spinner" },
      "owner": { "type": "string", "description": "New owner for the task" },
      "metadata": { "type": "object", "additionalProperties": {}, "description": "Metadata keys to merge" },
      "addBlocks": { "type": "array", "items": { "type": "string" }, "description": "Task IDs that this task blocks" },
      "addBlockedBy": { "type": "array", "items": { "type": "string" }, "description": "Task IDs that block this task" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:TaskUpdate"` 或 `"task update status"`

#### 5.13 TaskList

```json
{
  "name": "TaskList",
  "description": "List all tasks in the task list. Shows id, subject, status, owner, and blockedBy for each task.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": false,
    "properties": {}
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:TaskList"` 或 `"task list all"`

#### 5.14 TaskGet

```json
{
  "name": "TaskGet",
  "description": "Retrieve a task by its ID from the task list. Returns full details including description and dependencies.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["taskId"],
    "additionalProperties": false,
    "properties": {
      "taskId": { "type": "string", "description": "The ID of the task to retrieve" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:TaskGet"` 或 `"task get retrieve"`

#### 5.15 TaskOutput

```json
{
  "name": "TaskOutput",
  "description": "DEPRECATED: Prefer using the Read tool on the task's output file path instead. Retrieves output from a running or completed task.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["task_id", "block", "timeout"],
    "additionalProperties": false,
    "properties": {
      "task_id": { "type": "string", "description": "The task ID to get output from" },
      "block": { "type": "boolean", "default": true, "description": "Whether to wait for completion" },
      "timeout": { "type": "number", "default": 30000, "minimum": 0, "maximum": 600000, "description": "Max wait time in ms" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:TaskOutput"` 或 `"task output background"`

#### 5.16 TaskStop

```json
{
  "name": "TaskStop",
  "description": "Stops a running background task by its ID.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "task_id": { "type": "string", "description": "The ID of the background task to stop" },
      "shell_id": { "type": "string", "description": "Deprecated: use task_id instead" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:TaskStop"` 或 `"task stop kill"`

#### 5.17 EnterPlanMode

```json
{
  "name": "EnterPlanMode",
  "description": "Transitions into plan mode for non-trivial implementation tasks. In plan mode, you explore the codebase and design an implementation approach for user approval.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": false,
    "properties": {}
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:EnterPlanMode"` 或 `"plan mode enter"`

#### 5.18 ExitPlanMode

```json
{
  "name": "ExitPlanMode",
  "description": "Use when in plan mode and finished writing the plan to the plan file, ready for user approval.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": {},
    "properties": {
      "allowedPrompts": {
        "type": "array",
        "description": "Prompt-based permissions needed to implement the plan",
        "items": {
          "type": "object",
          "required": ["tool", "prompt"],
          "additionalProperties": false,
          "properties": {
            "tool": { "type": "string", "enum": ["Bash"], "description": "The tool this prompt applies to" },
            "prompt": { "type": "string", "description": "Semantic description of the action" }
          }
        }
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:ExitPlanMode"` 或 `"plan mode exit approve"`

#### 5.19 EnterWorktree

```json
{
  "name": "EnterWorktree",
  "description": "Creates an isolated git worktree and switches the session into it. ONLY when user explicitly asks for worktree.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "name": {
        "type": "string",
        "description": "Optional name for the worktree. Max 64 chars. Random name if not provided."
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:EnterWorktree"` 或 `"worktree enter create"`

#### 5.20 ExitWorktree

```json
{
  "name": "ExitWorktree",
  "description": "Exit a worktree session created by EnterWorktree. Returns session to original working directory.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["action"],
    "additionalProperties": false,
    "properties": {
      "action": {
        "type": "string",
        "enum": ["keep", "remove"],
        "description": "'keep' leaves worktree on disk; 'remove' deletes both."
      },
      "discard_changes": {
        "type": "boolean",
        "description": "Required true when removing with uncommitted files or unmerged commits."
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:ExitWorktree"` 或 `"worktree exit leave"`

#### 5.21 WebFetch

```json
{
  "name": "WebFetch",
  "description": "Fetches content from a URL, converts HTML to markdown, processes with AI model. Includes 15-minute cache. WILL FAIL for authenticated/private URLs.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["url", "prompt"],
    "additionalProperties": false,
    "properties": {
      "url": {
        "type": "string",
        "format": "uri",
        "description": "The URL to fetch content from"
      },
      "prompt": {
        "type": "string",
        "description": "The prompt to run on the fetched content"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:WebFetch"` 或 `"web fetch url"`

#### 5.22 WebSearch

```json
{
  "name": "WebSearch",
  "description": "Search the web and use results to inform responses. Must include Sources section with URLs after answering.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["query"],
    "additionalProperties": false,
    "properties": {
      "query": {
        "type": "string",
        "minLength": 2,
        "description": "The search query to use"
      },
      "allowed_domains": {
        "type": "array",
        "items": { "type": "string" },
        "description": "Only include results from these domains"
      },
      "blocked_domains": {
        "type": "array",
        "items": { "type": "string" },
        "description": "Never include results from these domains"
      }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:WebSearch"` 或 `"web search query"`

#### 5.23 LSP

```json
{
  "name": "LSP",
  "description": "Interact with Language Server Protocol servers for code intelligence. Supports goToDefinition, findReferences, hover, documentSymbol, workspaceSymbol, goToImplementation, prepareCallHierarchy, incomingCalls, outgoingCalls.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["operation", "filePath", "line", "character"],
    "additionalProperties": false,
    "properties": {
      "operation": {
        "type": "string",
        "enum": ["goToDefinition", "findReferences", "hover", "documentSymbol", "workspaceSymbol", "goToImplementation", "prepareCallHierarchy", "incomingCalls", "outgoingCalls"],
        "description": "The LSP operation to perform"
      },
      "filePath": { "type": "string", "description": "Absolute or relative path to the file" },
      "line": { "type": "integer", "exclusiveMinimum": 0, "maximum": 9007199254740991, "description": "Line number (1-based)" },
      "character": { "type": "integer", "exclusiveMinimum": 0, "maximum": 9007199254740991, "description": "Character offset (1-based)" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:LSP"` 或 `"lsp language server"`

#### 5.24 NotebookEdit

```json
{
  "name": "NotebookEdit",
  "description": "Replaces contents of a specific cell in a Jupyter notebook (.ipynb). Supports replace, insert, and delete edit modes.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["notebook_path", "new_source"],
    "additionalProperties": false,
    "properties": {
      "notebook_path": { "type": "string", "description": "Absolute path to the Jupyter notebook file" },
      "new_source": { "type": "string", "description": "The new source for the cell" },
      "cell_id": { "type": "string", "description": "The ID of the cell to edit" },
      "cell_type": { "type": "string", "enum": ["code", "markdown"], "description": "Cell type (required for insert)" },
      "edit_mode": { "type": "string", "enum": ["replace", "insert", "delete"], "description": "Edit type. Defaults to replace." }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:NotebookEdit"` 或 `"notebook jupyter edit"`

#### 5.25 CronCreate

```json
{
  "name": "CronCreate",
  "description": "Schedule a prompt to be enqueued at a future time. Supports recurring schedules and one-shot reminders. Uses 5-field cron in user's local timezone. Session-only by default. Recurring tasks auto-expire after 7 days.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["cron", "prompt"],
    "additionalProperties": false,
    "properties": {
      "cron": { "type": "string", "description": "Standard 5-field cron expression in local time" },
      "prompt": { "type": "string", "description": "The prompt to enqueue at each fire time" },
      "recurring": { "type": "boolean", "description": "true (default) = recurring; false = one-shot" },
      "durable": { "type": "boolean", "description": "true = persist to disk; false (default) = session only" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:CronCreate"` 或 `"cron schedule timer"`

#### 5.26 CronDelete

```json
{
  "name": "CronDelete",
  "description": "Cancel a cron job previously scheduled with CronCreate.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["id"],
    "additionalProperties": false,
    "properties": {
      "id": { "type": "string", "description": "Job ID returned by CronCreate" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:CronDelete"` 或 `"cron delete cancel"`

#### 5.27 CronList

```json
{
  "name": "CronList",
  "description": "List all cron jobs scheduled via CronCreate in this session.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": false,
    "properties": {}
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:CronList"` 或 `"cron list jobs"`

#### 5.28 RemoteTrigger

```json
{
  "name": "RemoteTrigger",
  "description": "Call the claude.ai remote-trigger API. Actions: list, get, create, update, run. OAuth token added automatically.",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["action"],
    "additionalProperties": false,
    "properties": {
      "action": { "type": "string", "enum": ["list", "get", "create", "update", "run"] },
      "trigger_id": { "type": "string", "pattern": "^[\\w-]+$", "description": "Required for get, update, and run" },
      "body": { "type": "object", "additionalProperties": {}, "description": "JSON body for create and update" }
    }
  }
}
```

**ToolSearch 查詢 / ToolSearch query**: `"select:RemoteTrigger"` 或 `"remote trigger schedule"`

### ToolSearch 即時示範 / ToolSearch Live Demonstration

在本次自我檢視中，我實際呼叫了 ToolSearch 4 次來取得所有延遲載入工具的 schema：

During this self-examination, I actually called ToolSearch 4 times to fetch all deferred tool schemas:

1. `ToolSearch(query="select:AskUserQuestion,TaskCreate,TaskUpdate,TaskList,TaskGet,TaskOutput,TaskStop", max_results=7)` → 成功取得 7 個工具的完整 schema / Successfully fetched 7 tool schemas
2. `ToolSearch(query="select:EnterPlanMode,ExitPlanMode,EnterWorktree,ExitWorktree", max_results=4)` → 成功取得 4 個工具 / Successfully fetched 4 tools
3. `ToolSearch(query="select:WebFetch,WebSearch,LSP,NotebookEdit", max_results=4)` → 成功取得 4 個工具 / Successfully fetched 4 tools
4. `ToolSearch(query="select:CronCreate,CronDelete,CronList,RemoteTrigger", max_results=4)` → 成功取得 4 個工具 / Successfully fetched 4 tools

### 多結果時的決策邏輯 / Decision Logic for Multiple Results

當 ToolSearch 回傳多個結果時，我根據以下標準決定使用哪一個：

When ToolSearch returns multiple results, I decide which to use based on:

1. **名稱匹配 / Name match** — 如果我知道確切的工具名稱，使用 `select:` 語法精確匹配。
   If I know the exact tool name, use `select:` syntax for precise matching.
2. **描述相關性 / Description relevance** — 閱讀每個工具的描述，選擇最符合當前任務需求的。
   Read each tool's description, select the one most relevant to the current task.
3. **參數匹配 / Parameter fit** — 檢查工具的參數是否與我需要傳遞的資料匹配。
   Check if the tool's parameters match the data I need to pass.
4. **系統提示詞指引 / System prompt guidance** — 某些情況下系統提示詞會指示偏好某個工具（如偏好 MCP 工具而非內建 WebFetch）。
   In some cases the system prompt indicates preference (e.g., prefer MCP tools over built-in WebFetch).

---

## 6. 工具規格 / Tool Specifications

### 工具遵循的規格格式 / Specification Format Tools Follow

工具定義遵循 **Anthropic Tool Use API 格式**，使用 **JSON Schema (Draft 2020-12)** 來定義參數。每個工具定義包含：

Tool definitions follow the **Anthropic Tool Use API format**, using **JSON Schema (Draft 2020-12)** for parameter definitions. Each tool definition contains:

```json
{
  "name": "工具名稱 / tool name",
  "description": "工具描述 / tool description",
  "parameters": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["必要參數 / required params"],
    "additionalProperties": false,
    "properties": { ... }
  }
}
```

這個格式來自 Anthropic 的 Messages API 規格，在 `tools` 陣列中定義。

This format comes from Anthropic's Messages API specification, defined in the `tools` array.

### Anthropic vs OpenAI 工具格式比較 / Anthropic vs OpenAI Tool Format Comparison

| 面向 / Aspect | Anthropic | OpenAI |
|---|---|---|
| **頂層結構 / Top-level** | `{ name, description, parameters }` | `{ type: "function", function: { name, description, parameters } }` — 多一層 `function` 包裝 / extra `function` wrapper |
| **Schema 版本 / Schema version** | JSON Schema Draft 2020-12 | JSON Schema（未明確指定版本 / version unspecified） |
| **呼叫格式 / Call format** | `tool_use` content block with `{ id, name, input }` | `tool_calls` array with `{ id, type, function: { name, arguments } }` |
| **結果格式 / Result format** | `tool_result` content block referencing `tool_use_id` | `tool` role message referencing `tool_call_id` |
| **並行呼叫 / Parallel calls** | 模型可在單次回應中輸出多個 `tool_use` blocks / Model can output multiple `tool_use` blocks in one response | 明確支援 `parallel_tool_calls` 參數 / Explicitly supports `parallel_tool_calls` parameter |
| **嚴格模式 / Strict mode** | 透過 `additionalProperties: false` 實現 / Via `additionalProperties: false` | 有明確的 `strict: true` 選項 / Has explicit `strict: true` option |

核心差異在於 Anthropic 的格式更扁平，而 OpenAI 多了一層 `function` 嵌套。兩者都使用 JSON Schema 來定義參數，語義上非常相似。

The core difference is Anthropic's format is flatter, while OpenAI has an extra `function` nesting level. Both use JSON Schema for parameters and are semantically very similar.

### MCP (Model Context Protocol) / MCP (Model Context Protocol)

**MCP** 是 Anthropic 於 2024 年底推出的開放標準，旨在標準化 AI 模型與外部工具/資料源之間的通信協議。

**MCP** is an open standard introduced by Anthropic in late 2024, aimed at standardizing the communication protocol between AI models and external tools/data sources.

MCP 與工具標準化的關係 / MCP's relationship to tool standardization:

1. **統一介面 / Unified interface** — MCP 定義了一個標準化的方式讓任何工具伺服器暴露其功能，任何 MCP 客戶端（如 Claude Code）都可以自動發現和使用這些工具。
   MCP defines a standardized way for any tool server to expose its capabilities, which any MCP client (like Claude Code) can automatically discover and use.

2. **工具定義互通 / Tool definition interoperability** — MCP 工具的定義格式（名稱、描述、JSON Schema 參數）與 Anthropic 原生工具格式一致，使得 MCP 工具可以無縫地與內建工具並列。
   MCP tool definitions (name, description, JSON Schema parameters) align with Anthropic's native tool format, allowing MCP tools to seamlessly coexist with built-in tools.

3. **在 Claude Code 中的體現 / Manifestation in Claude Code** — 系統提示詞中提到 MCP 工具可以與內建工具共存：`"If an MCP-provided web fetch tool is available, prefer using that tool instead of this one"`。使用者可以透過配置 MCP 伺服器來擴展 Claude Code 的工具集。
   The system prompt mentions MCP tools coexisting with built-in tools. Users can extend Claude Code's toolset by configuring MCP servers.

4. **協議結構 / Protocol structure** — MCP 使用 JSON-RPC 2.0 作為傳輸層，支援 `tools/list`（發現工具）和 `tools/call`（呼叫工具）等方法。
   MCP uses JSON-RPC 2.0 as transport layer, supporting methods like `tools/list` (discover tools) and `tools/call` (invoke tools).

---

## 7. 命令 / Commands

### 什麼是命令 / What are Commands

**命令（Commands）** 是 Claude Code Harness 內建的操作，由使用者在輸入提示中以 `/` 前綴觸發。它們與工具和技能的區別如下：

**Commands** are built-in operations in the Claude Code Harness, triggered by users with a `/` prefix in the input prompt. They differ from tools and skills as follows:

| 面向 / Aspect | 命令 Commands | 工具 Tools | 技能 Skills |
|---|---|---|---|
| **執行者 / Executor** | Harness 直接執行 / Harness directly | Harness 代替模型執行 / Harness on behalf of model | 模型載入後執行 / Model loads then executes |
| **觸發方式 / Trigger** | 使用者輸入 `/command` / User types `/command` | 模型決定呼叫 / Model decides to call | 使用者輸入 `/skill` 或模型透過 Skill 工具呼叫 / User types `/skill` or model calls via Skill tool |
| **模型參與 / Model involvement** | 部分命令的結果會傳達給模型 / Some results are communicated to model | 模型決定何時使用、解讀結果 / Model decides when to use, interprets results | 模型載入技能內容並遵循其指示 / Model loads skill content and follows its instructions |
| **定義位置 / Definition location** | Harness 原始碼中硬編碼 / Hardcoded in Harness source | 系統提示詞中定義 / Defined in system prompt | 使用者目錄中的檔案 / Files in user directories |
| **可擴展性 / Extensibility** | 使用者不能新增 / Users cannot add | 透過 MCP 可新增 / Extensible via MCP | 使用者可自由新增 / Users freely add |

### 已知命令列表 / Known Commands List

根據我在系統提示詞和 `<local-command-stdout>` 標籤中觀察到的證據：

Based on evidence I observe in the system prompt and `<local-command-stdout>` tags:

**我確知的命令 / Commands I'm certain about:**

| 命令 / Command | 說明 / Description | 證據 / Evidence |
|---|---|---|
| `/help` | 獲取 Claude Code 使用幫助 / Get Claude Code help | 系統提示詞提到：`"/help: Get help with using Claude Code"` |
| `/clear` | 清除對話 / Clear conversation | 系統提示詞提到 `/help, /clear` 為 built-in CLI commands |
| `/model` | 切換模型 / Switch model | 本次對話中觀察到 `<command-name>/model</command-name>` 和結果 `Set model to Opus 4.6` |
| `/usage` | 查看使用狀況 / View usage | 本次對話中觀察到 `<command-name>/usage</command-name>` 和結果 `Status dialog dismissed` |
| `/fast` | 切換快速模式 / Toggle fast mode | 系統提示詞提到：`"It can be toggled with /fast."` |
| `/tasks` | 查看任務列表 / View task list | TaskOutput 描述中提到：`"Task IDs can be found using the /tasks command"` |

**我推測可能存在但無法確認的命令 / Commands I suspect exist but cannot confirm:**

- `/compact` — 壓縮對話歷史（推測 / Speculated: compress conversation history）
- `/config` 或 `/settings` — 管理設定（推測 / Speculated: manage settings）
- `/init` — 初始化 CLAUDE.md（推測 / Speculated: initialize CLAUDE.md）
- `/login` / `/logout` — 帳戶管理（推測 / Speculated: account management）
- `/permissions` — 管理權限（推測 / Speculated: manage permissions）
- `/bug` — 回報問題（推測 / Speculated: report bugs）
- `/review` — 審查 PR（推測 / Speculated: review PRs）
- `/pr-comments` — 查看 PR 評論（推測 / Speculated: view PR comments）

### 盲點 / Blind Spots

我對命令系統的盲點包括：

My blind spots regarding the command system include:

1. **完整命令列表不可見** — 系統提示詞中沒有列出所有可用命令。我只能從上下文線索推斷。
   The complete command list is not visible — the system prompt doesn't enumerate all available commands. I can only infer from contextual clues.

2. **命令的內部實作** — 我不知道命令在 Harness 中如何實作，只能看到它們的輸入和輸出。
   Internal command implementation — I don't know how commands are implemented in Harness, I only see their input and output.

3. **命令的參數和選項** — 大多數命令可能有我不知道的參數或選項。
   Command parameters and options — most commands likely have parameters or options I'm unaware of.

### 命令執行時模型是否知道 / Does the Model Know When Commands Execute

**部分知道。** 當命令被執行時，結果以特殊格式傳達給我：

**Partially.** When commands execute, results are communicated to me in special formats:

- `<command-name>` 標籤告訴我哪個命令被執行 / Tags tell me which command was executed
- `<local-command-stdout>` 標籤包含命令的輸出 / Tags contain the command's output
- `<local-command-caveat>` 標籤提供關於如何處理這些結果的指示 / Tags provide instructions on how to handle results

例如本次對話中 / For example in this conversation:
```
<command-name>/model</command-name>
<command-message>model</command-message>
<local-command-stdout>Set model to Opus 4.6</local-command-stdout>
```

但有一個重要的注意事項 — 系統提示詞中的 caveat 指出：
But there's an important caveat — the system prompt's caveat states:

> `"Caveat: The messages below were generated by the user while running local commands. DO NOT respond to these messages or otherwise consider them in your response unless the user explicitly asks you to."`

這表明某些命令的結果是「被動」傳達的 — Harness 會告訴我發生了什麼，但我不應該主動回應。

This indicates some command results are "passively" communicated — Harness tells me what happened, but I shouldn't proactively respond.

### 如何區分命令和技能 / How to Distinguish Commands from Skills

當使用者輸入 `/something` 時：

When a user types `/something`:

1. **Harness 優先處理** — Harness 首先檢查是否為內建命令。如果是，Harness 直接執行，不需要模型參與。
   Harness handles first — Harness checks if it's a built-in command. If so, Harness executes directly without model involvement.

2. **技能匹配** — 如果不是內建命令，Harness 檢查是否匹配已註冊的技能名稱。如果匹配，技能內容被載入並傳達給模型。
   Skill matching — if not a built-in command, Harness checks if it matches a registered skill name. If matched, skill content is loaded and communicated to the model.

3. **我的視角 / My perspective** — 我看到技能列表在 `<system-reminder>` 中。系統提示詞指示我：`"Do not use this tool for built-in CLI commands (like /help, /clear, etc.)"`。這意味著我需要知道哪些是命令、哪些是技能，並只對技能使用 Skill 工具。
   I see the skill list in `<system-reminder>`. The system prompt instructs me: "Do not use this tool for built-in CLI commands (like /help, /clear, etc.)." This means I need to know which are commands and which are skills, and only use the Skill tool for skills.

4. **實際觀察 / Practical observation** — 在本次對話中，`/usage` 和 `/model` 被作為命令執行（我收到 `<command-name>` 標籤），而 `/claude-code-architecture` 被作為技能載入（我收到 `<command-name>` 標籤但附帶了完整的技能內容）。
   In this conversation, `/usage` and `/model` were executed as commands (I received `<command-name>` tags), while `/claude-code-architecture` was loaded as a skill (I received `<command-name>` tags but with full skill content attached).

---

## 附錄：工具總覽表 / Appendix: Tool Overview Table

| # | 工具名稱 / Tool Name | 載入方式 / Loading | 類別 / Category |
|---|---|---|---|
| 1 | Agent | 即時 / Eager | 代理管理 / Agent management |
| 2 | Bash | 即時 / Eager | 系統操作 / System operations |
| 3 | Edit | 即時 / Eager | 檔案編輯 / File editing |
| 4 | Glob | 即時 / Eager | 檔案搜尋 / File search |
| 5 | Grep | 即時 / Eager | 內容搜尋 / Content search |
| 6 | Read | 即時 / Eager | 檔案讀取 / File reading |
| 7 | Write | 即時 / Eager | 檔案寫入 / File writing |
| 8 | Skill | 即時 / Eager | 技能呼叫 / Skill invocation |
| 9 | ToolSearch | 即時 / Eager | 工具發現 / Tool discovery |
| 10 | AskUserQuestion | 延遲 / Deferred | 使用者互動 / User interaction |
| 11 | TaskCreate | 延遲 / Deferred | 任務管理 / Task management |
| 12 | TaskUpdate | 延遲 / Deferred | 任務管理 / Task management |
| 13 | TaskList | 延遲 / Deferred | 任務管理 / Task management |
| 14 | TaskGet | 延遲 / Deferred | 任務管理 / Task management |
| 15 | TaskOutput | 延遲 / Deferred | 任務管理 / Task management |
| 16 | TaskStop | 延遲 / Deferred | 任務管理 / Task management |
| 17 | EnterPlanMode | 延遲 / Deferred | 規劃模式 / Plan mode |
| 18 | ExitPlanMode | 延遲 / Deferred | 規劃模式 / Plan mode |
| 19 | EnterWorktree | 延遲 / Deferred | 工作樹隔離 / Worktree isolation |
| 20 | ExitWorktree | 延遲 / Deferred | 工作樹隔離 / Worktree isolation |
| 21 | WebFetch | 延遲 / Deferred | 網路存取 / Web access |
| 22 | WebSearch | 延遲 / Deferred | 網路搜尋 / Web search |
| 23 | LSP | 延遲 / Deferred | 程式碼智慧 / Code intelligence |
| 24 | NotebookEdit | 延遲 / Deferred | Jupyter 編輯 / Jupyter editing |
| 25 | CronCreate | 延遲 / Deferred | 排程 / Scheduling |
| 26 | CronDelete | 延遲 / Deferred | 排程 / Scheduling |
| 27 | CronList | 延遲 / Deferred | 排程 / Scheduling |
| 28 | RemoteTrigger | 延遲 / Deferred | 遠端觸發 / Remote triggers |

**總計 / Total: 9 個即時載入 + 19 個延遲載入 = 28 個工具**
**Total: 9 eagerly loaded + 19 deferred = 28 tools**

---

> 本文件由 Claude Opus 4.6 自我檢視產生。所有內容基於模型在當前對話中實際觀察到的系統提示詞和上下文。推測的部分已明確標註。
> This document was generated by Claude Opus 4.6 self-examination. All content is based on what the model actually observes in the current conversation's system prompt and context. Speculated portions are clearly marked.
