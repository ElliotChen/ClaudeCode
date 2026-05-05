---
name: event-doc-generator
description: >
  Analyze Spring Boot + DDD + Axon Framework + Saga projects to generate comprehensive event documentation in Traditional Chinese.
  Use when the user wants to document, catalog, or understand events in an Axon-based project.
  Triggers include: requests to generate event docs, analyze Axon events, document domain events,
  create event catalogs, or produce event-driven architecture documentation for Axon/Saga projects.
  Outputs individual markdown files per event plus a README summary, all under docs/event/.
---

# Axon Event Documentation Generator

Generate per-event documentation files in Traditional Chinese for Spring Boot + DDD + Axon + Saga projects.

## Workflow

0. **Detect mode**（偵測模式）— per-event 判斷生成 or 更新（必執行的第一步）
1. **Scan** the project for all event classes
2. **Classify** each event as Inbound or Outbound
3. **Generate or update** individual event doc files
4. **Generate or update** README.md summary

## Step 0: Detect Mode（偵測模式） — Per-Event

This skill 支援兩種模式，**每個事件獨立判斷**：

- **生成模式（Generate）**：`docs/event/<EventClassName>.md` 不存在 → 從零產生完整文件
- **更新模式（Update）**：`docs/event/<EventClassName>.md` 已存在 → 切換為「智慧增量更新」，僅修改差異段落，保留其餘內容與使用者自訂修改

### 偵測流程

1. 完成 Step 1 掃描後，取得所有事件類別清單
2. 對每一個事件 `<EventClassName>`：
   - 檢查 `docs/event/<EventClassName>.md` 是否存在
   - 若不存在 → 該事件進入「生成模式」
   - 若已存在 → 該事件進入「更新模式」，依下方「Smart Incremental Update（智慧增量更新）」章節執行
3. 向使用者顯示偵測結果摘要，例如：

   > 共偵測到 12 個事件：9 個將進入**更新模式**（已有文件），3 個將進入**生成模式**（新事件）。

4. 同樣對 `docs/event/README.md` 做存在性檢查；存在則更新，不存在則生成

### Override

若使用者明確指示「重做 / regenerate / 全部重新產生」等，可由使用者覆寫為對全部事件採生成模式（**會覆蓋既有文件，建議先 Git commit**）。

---

## Step 1: Scan Project for Events

Recursively search the project source tree for event classes. Use these heuristics:

- Classes in packages matching `**/event/**`, `**/events/**`, `**/domain/event/**`
- Classes with names ending in `Event` (e.g., `OrderCreatedEvent`, `PaymentCompletedEvent`)
- Classes annotated with Axon-related annotations or referenced in `@EventHandler`, `@SagaEventHandler`, `@EventSourcingHandler`
- Classes used in `AggregateLifecycle.apply()`, `EventGateway.publish()`, `SagaLifecycle.associateWith()`

Use bash to scan:

```bash
# Find candidate event files
find <project-root>/src -type f -name "*.java" -o -name "*.kt" | xargs grep -l "Event\b" | head -200

# Find event handler references to discover events
grep -rn "@EventHandler\|@SagaEventHandler\|@EventSourcingHandler\|AggregateLifecycle.apply\|EventGateway.publish" <project-root>/src --include="*.java" --include="*.kt"
```

Read each discovered event class to extract: class name, package, fields (with types), and any Javadoc/KDoc.

## Step 2: Classify Events

Determine direction for each event based on context:

**Inbound（入站事件）**: Events consumed/handled by this service.
- Found in `@EventHandler`, `@SagaEventHandler`, `@EventSourcingHandler` methods
- Events from external services arriving via message broker

**Outbound（出站事件）**: Events published/emitted by this service.
- Published via `AggregateLifecycle.apply()` within Aggregates
- Published via `EventGateway.publish()` or `CommandGateway` side effects
- Events dispatched to external services

An event can be BOTH Inbound and Outbound if the same service publishes and consumes it (common in Event Sourcing with Axon). Mark these as `雙向（Inbound / Outbound）`.

## Step 3: Generate or Update Individual Event Files

Create `docs/event/` directory if it doesn't exist. For each event, decide based on Step 0 mode:

- **生成模式**：依照 [references/event-doc-template.md](references/event-doc-template.md) 範本，產生新的 `<EventClassName>.md`
- **更新模式**：依下方「Smart Incremental Update」章節執行精確的 `str_replace`，不重寫整份文件

Key rules:
- All content in **繁體中文**
- File name uses the original Java/Kotlin class name (English)
- Include all fields with types and descriptions
- Include Axon manual dispatch examples using `EventGateway` or `GenericEventMessage`
- Include the Aggregate or Saga context where the event participates
- 更新模式下：保留 `<!-- 自訂內容開始 -->` ~ `<!-- 自訂內容結束 -->` 標記內的內容；保留任何明顯人工編輯的段落

## Step 4: Generate or Update README.md

Create or update `docs/event/README.md` summarizing all events.

- **生成模式（README 不存在）**：依下方範本完整產生
- **更新模式（README 已存在）**：以增量方式同步：
  - 重新計算統計表（入站 / 出站 / 雙向 / 合計）並替換該區塊
  - 比對「事件清單」表格：新增列、移除已不存在的事件、更新方向或說明變動的列；**保留既有列的人工備註欄位**（若有）
  - **完整保留** `<!-- 自訂內容開始 -->` ~ `<!-- 自訂內容結束 -->` 標記內容（例如團隊撰寫的事件設計指引、命名慣例）

範本結構如下：

```markdown
# 事件文件總覽

> 本文件由工具自動產生，記錄專案中所有 Axon Event 的摘要資訊。

## 統計

| 類別 | 數量 |
|------|------|
| 入站事件 (Inbound) | N |
| 出站事件 (Outbound) | N |
| 雙向事件 (Both) | N |
| **合計** | **N** |

## 事件清單

| 事件名稱 | 方向 | 所屬 Bounded Context | 說明 | 文件連結 |
|----------|------|---------------------|------|----------|
| OrderCreatedEvent | 出站 | Order | 訂單建立時發布 | [查看](./OrderCreatedEvent.md) |
...
```

## Smart Incremental Update（智慧增量更新）

進入更新模式的事件文件，遵循以下原則執行：

### 增量更新原則

1. **永遠先讀後寫**：使用 view / Read 完整讀取既有 `docs/event/<EventClassName>.md`，再決定要改什麼，禁止盲目覆寫
2. **以差異為單位**：比對「最新事件原始碼」與「既有文件描述」，列出差異點清單後才動工
3. **只改必要段落**：使用 `str_replace` 精確替換，**不重寫整份文件**，未受影響的章節保持原樣
4. **保留使用者自訂**：辨識並保留 `<!-- 自訂內容開始 -->` ~ `<!-- 自訂內容結束 -->` 區塊；對於明顯人工撰寫的描述（業務語境、踩坑經驗、特殊備註），優先保留
5. **同步更新 README**：若事件方向、Bounded Context 或說明改變，同步更新 README.md 的對應列
6. **更新 metadata**：若文件範本含「最後更新日期」或版本欄位，改為當日

### 比對與差異識別流程

對每個進入更新模式的事件：

```
1. Read 既有 docs/event/<EventClassName>.md
2. 從最新原始碼擷取：
   - 事件類別 fully-qualified name
   - 全部欄位（名稱、型別、Javadoc / KDoc）
   - 標註的 annotation
   - 出現位置（Aggregate.apply / EventGateway.publish / @EventHandler / @SagaEventHandler 等）
3. 列出差異清單：
   - 欄位新增 / 移除 / 改名 / 改型別？
   - 方向（Inbound / Outbound / 雙向）改變了嗎？
   - 所屬 Aggregate / Saga / Bounded Context 改變了嗎？
   - Javadoc / KDoc 內容改了嗎？
4. 將差異對映到文件章節（事件結構、欄位表、Aggregate 關聯、手動派送範例…）
5. 對每個受影響章節執行 str_replace
```

### 常見更新情境與影響章節

| 變更類型 | 影響章節 |
|---------|---------|
| 新增 / 移除 / 改名欄位 | 欄位表、手動派送範例（sample values） |
| 欄位型別變更 | 欄位表、手動派送範例 |
| 事件方向改變（Outbound → 雙向 等） | 文件 metadata 表頭、README「事件清單」對應列、README 統計表 |
| 改變所屬 Aggregate / Saga | 「Aggregate / Saga 關聯」章節、手動派送範例上下文 |
| 包名（package）異動 | 文件中 fully-qualified name、import 範例 |
| Javadoc / KDoc 內容更新 | 事件描述章節 |
| 事件被刪除 | 從 README 移除該列；保留既有 `<EventClassName>.md` 但加上「⚠️ 已棄用」標記，等待使用者確認是否實際刪檔 |

### 保留使用者自訂內容

建議使用者在文件中以下列註解標記團隊自訂段落，本技能更新時會完整保留：

```markdown
<!-- 自訂內容開始 -->
這裡是事件的業務語境補充、實作踩坑紀錄、消費端注意事項…
<!-- 自訂內容結束 -->
```

更新時，本技能會：
- 識別並保留 `<!-- 自訂內容開始 -->` 和 `<!-- 自訂內容結束 -->` 之間的內容
- 僅更新標記之外的自動生成內容
- 若自訂內容引用的欄位 / Aggregate 已不存在，於變更回報中提醒使用者人工檢查

---

## Important Notes

- If event direction cannot be determined from code analysis alone, mark as `⚠️ 待確認` and add a note asking the developer to verify.
- For Saga events, document which Saga consumes/produces them and the association property.
- Preserve the original field names from the source code; add Chinese descriptions alongside.
- When generating manual dispatch examples, use the actual field types and provide realistic sample values.
