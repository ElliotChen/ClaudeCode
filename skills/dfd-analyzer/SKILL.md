---
name: dfd-analyzer
description: |
  為 Spring Boot + DDD 架構專案產生資料流程圖（Data Flow Diagram）文件。
  當使用者需要：(1) 為 Bounded Context 繪製 DFD、(2) 說明系統資料流動與整合方式、
  (3) 產生包含 Mermaid 圖表的 DFD 架構文件、(4) 分析跨 Context 的資料流與事件流時使用此技能。
  適用於任何提及「DFD」、「資料流程圖」、「data flow」搭配 Spring Boot 或 DDD 的場景。
---

# DFD 文件產生技能（Spring Boot + DDD）

## 概述

為採用 Spring Boot 與 DDD 架構的專案產生結構化的 DFD（Data Flow Diagram）文件。
所有產出使用繁體中文，圖表以 Mermaid flowchart 語法繪製，方便嵌入 Markdown 文件。

## 核心原則

1. **以 Bounded Context 為分解邊界** — DFD 的 Process 分解對齊領域邊界，而非技術模組
2. **使用 Ubiquitous Language 命名** — 所有標籤使用該 Context 的通用語言
3. **區分同步與非同步資料流** — 明確標註 REST/gRPC（實線）與事件驅動（虛線）
4. **Data Store 對應 Aggregate/Repository** — 避免直接使用資料庫表名
5. **CQRS 讀寫分離** — 若採用 CQRS，讀模型與寫模型繪製為不同的 Data Store

## DFD 層次結構

產生 DFD 文件時，依照以下層次逐步細化：

| 層次 | 對應 DDD 概念 | 說明 |
|------|--------------|------|
| Level 0（Context Diagram） | 系統全貌 | 系統與外部實體的互動 |
| Level 1 | Bounded Context | 每個主要 Process 對應一個 BC |
| Level 2 | Application Service / Use Case | 各 BC 內的核心用例 |
| Level 3+（視需要） | Domain Service / Aggregate 互動 | 僅在複雜協調邏輯時展開 |

詳細分層指南見 [references/dfd-levels.md](references/dfd-levels.md)。

## DDD 對應規則

在 Spring Boot + DDD 專案中繪製 DFD 時，需遵循特定的對應規則，
包含 Process、Data Store、資料流的命名與語義約束。

完整對應規則見 [references/ddd-mapping.md](references/ddd-mapping.md)。

## Mermaid 繪圖規範

所有 DFD 圖表使用 Mermaid `flowchart` 語法，搭配 `subgraph` 表達 Bounded Context 邊界。

繪圖範例與模板見 [references/mermaid-examples.md](references/mermaid-examples.md)。

## 產出流程

依序執行以下步驟：

1. **蒐集資訊** — 確認系統的 Bounded Context 清單、外部系統、主要 Use Case
2. **繪製 Level 0** — 建立 Context Diagram，標註所有外部實體與系統邊界
3. **繪製 Level 1** — 將系統拆解為各 Bounded Context，標註 Context 間的資料流與事件
4. **繪製 Level 2**（視需要）— 展開關鍵 Context 的內部 Use Case
5. **產出文件** — 每個層次產生獨立的 Markdown 檔案，包含 Mermaid 圖表與文字說明
6. **產出總覽** — 生成 README.md 彙整所有 DFD 文件的導覽

## 產出檔案結構

```
docs/
  └──dfd/
      ├── level-0-context-diagram.md    # 系統上下文圖
      ├── level-1-bounded-contexts.md   # Bounded Context 層級
      ├── level-2-{context-name}.md     # 各 BC 的內部用例（視需要）
      └── README.md                     # 導覽與總結
```

## 文件模板

每份 DFD 文件應包含以下區段：

```markdown
# {層次名稱}：{範圍描述}

## 圖表

（Mermaid flowchart）

## 元素說明

### 外部實體
- {名稱}：{說明}

### 處理（Process）
- {名稱}：{對應的 DDD 概念}，{職責說明}

### 資料儲存（Data Store）
- {名稱}：{對應的 Aggregate/Repository}，{儲存內容}

### 資料流
- {來源} → {目標}：{資料內容}，{同步/非同步}

## 設計決策與注意事項
- {重要的架構決策或限制}
```
