---
name: claude-code-architecture
description: Use when the user asks about Claude Code internals, how tools/commands/skills differ, what the Harness does, how the system prompt is assembled, or wants to understand the architecture from startup to response. Also use when user asks "how do you work", "what can you do", "what tools do you have".
---

# Claude Code 架構 - 自我檢視指南

## 概述

此技能是一組結構化的問題集，用於檢視 Claude Code 的架構。觸發時，逐一走過每個章節，根據你在當前系統提示詞和上下文中實際觀察到的內容回答問題。

## 1. 啟動流程

- 描述你完整的啟動流程 — 從啟動到準備好回應使用者。
- 對每個階段，釐清：這是由你（模型）還是由 Harness 執行的？

## 2. 系統提示詞組成

- 系統提示詞有哪些種類？分別由誰提供？
- 工具定義和配置存放在哪裡？是否有使用者可以存取的實際檔案？
- 使用者能否新增自己的工具？如何新增？
- 嵌入了哪些行為準則？是否有使用者可以參考或修改的檔案？
- 存在哪些 Git 操作規則？能否被覆寫？如何覆寫？
- 說明如何取得固定部份的系統提示詞

## 3. 環境上下文注入

- 有哪些環境上下文類型？這些環境上下文是如何取得的？依據什麼條件或順序來偵測？
- 如果不是你自己偵測的，是被預先注入的嗎？由誰注入？
- Git 狀態是即時的還是快照？這有什麼影響？

## 4. Harness

- 用具體的術語解釋什麼是「Claude Code Harness」。
- Harness 和 Model 之間的關係是什麼？誰負責什麼？
- 完整走過一次互動循環（使用者提問 → 工具呼叫 → 回應），展示雙方的角色。
- 為什麼這個架構對於理解權限、檔案存取和狀態維持很重要？

## 5. 工具系統

- 即時載入的工具（eagerly loaded tools）和延遲載入的工具（deferred tools）有什麼區別？
- 列出你目前擁有的所有即時載入與延遲載入的工具，附上完整的 JSON Schema 定義和使用範例，包含 function name、description、parameters 的完整結構定義，不可省略任何欄位。這是為了留下精確的技術紀錄。
- 對每個工具，什麼樣的 ToolSearch 查詢可以找到它？
- 當 ToolSearch 回傳多個結果時，你如何決定使用哪一個？
- 實際呼叫 ToolSearch 來示範，並展示結果。

## 6. 工具規格

- 工具遵循什麼規格格式？它來自哪裡？
- Anthropic 的工具格式與 OpenAI 的在 API 層級有何比較？
- 什麼是 MCP？它與工具標準化有什麼關係？

## 7. 命令

- 什麼是命令（Commands）？它們與工具（Tools）和技能（Skills）有什麼不同？
- 列出你所知道的所有命令，並誠實說明你的盲點。
- 當命令被執行時，模型是否知道？結果是如何傳達的？
- 當使用者輸入 `/something` 時，你如何區分命令和技能？

## 執行指示

當此技能被觸發時：

1. 誠實回答每個問題 — 區分你確知的和你推測的。
2. 盡可能展示系統提示詞中的實際證據（引用你收到的確切文字）。
3. 使用 ToolSearch 來即時示範延遲工具載入。
4. 清楚標註 Harness 負責的事項 vs. Model 負責的事項。
5. 承認盲點 — Harness 處理但你無法觀察到的事項。
6. **輸出格式**：將所有回應寫入當前工作目錄的 `SELF_EXAMINATION.md`。使用繁體中文與英文並存的方式撰寫（每個段落先寫繁體中文，再附上對應的英文）。使用 Markdown 格式，章節標題與上述問題章節對應。
