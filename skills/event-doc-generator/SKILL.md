---
name: event-doc-generator
description: 為 Spring Boot Event-Driven 專案自動產生完整的事件文件。掃描 Java 原始碼分析 Event 類別、Publisher 和 Listener，為每個 Event 生成獨立的 Markdown 文件，包含欄位說明、使用場景、事件流程圖等。文件輸出至 docs/event/ 目錄，並自動產生索引和 Mermaid 圖表。適用於採用 DDD、Event Sourcing 或 Event-Driven Architecture 的 Spring Boot 專案。
license: Apache 2.0
---

# Event Documentation Generator

為 Spring Boot Event-Driven 架構專案自動產生完整的事件文件系統。

## 觸發時機

當使用者提出以下需求時啟用此 skill：
- "產生事件文件"、"生成 Event 文件"
- "分析專案中的所有 Event"
- "建立事件說明文件"
- "文件化 Event-Driven 架構"
- 任何關於 Spring Boot Event 的文件產生需求

## 工作流程

### 階段 1: 專案掃描與分析

1. **確認專案結構**
   ```bash
   # 檢查是否為 Spring Boot 專案
   test -f pom.xml || test -f build.gradle
   
   # 找出主要的 Java 原始碼目錄
   find . -type d -name "java" | grep "src/main"
   ```

2. **掃描 Event 類別**
   執行 `scripts/scan_events.py` 掃描所有符合以下條件的類別：
   - 繼承 `ApplicationEvent`, `CanonicalDomainEvent` 或 `DomainEvent`
   - 類別名稱以 `Event` 結尾
   - 標記 `@DomainEvent` 註解
   - 位於 `event` 或 `domain.event` 套件中

3. **分析 Event Publisher**
   搜尋發送事件的程式碼：
   ```bash
   # 找出所有 publishEvent 呼叫
   rg "publishEvent\s*\(" --type java -A 2 -B 2 --json | \
     python3 scripts/analyze_publishers.py
   ```

4. **分析 Event Listener**
   搜尋事件監聽器：
   ```bash
   # 找出所有 @EventListener 和 @TransactionalEventListener
   rg "@(Transactional)?EventListener" --type java -A 10 -B 2 --json | \
     python3 scripts/analyze_listeners.py
   ```

### 階段 2: 建立文件結構

確保文件目錄存在：
```bash
mkdir -p docs/event/{events,flows,diagrams}
```

文件結構：
```
docs/event/
├── README.md                    # 索引頁面，列出所有 Event
├── EVENT_CATALOG.md            # 完整的 Event 目錄
├── BOUNDED_CONTEXTS.md         # 依 Bounded Context 分類
├── events/                     # 每個 Event 的詳細文件
│   ├── UserCreatedEvent.md
│   ├── OrderPlacedEvent.md
│   └── PaymentProcessedEvent.md
├── flows/                      # 事件流程說明
│   ├── user-registration-flow.md
│   └── order-processing-flow.md
└── diagrams/                   # Mermaid 圖表
    ├── all-events.mmd          # 整體事件圖
    └── sequence-diagrams.mmd   # 序列圖
```

### 階段 3: 產生個別 Event 文件

為每個 Event 生成獨立的 Markdown 檔案，使用 `scripts/generate_event_doc.py`。

**每個 Event 文件包含：**

1. **基本資訊**
   - Event 名稱
   - 完整的 package 路徑
   - Bounded Context（從 package 結構推斷）
   - JavaDoc 說明

2. **欄位定義**
   - 欄位名稱、型別、說明
   - 是否為必填
   - 預設值（如有）
   - 驗證規則（從 JSR-380 註解解析）

3. **使用場景**
   - 何時觸發此事件
   - 業務意義
   - 相關的 Use Case

4. **Publisher 資訊**
   - 哪些類別/方法會發送此事件
   - 發送時機
   - 程式碼範例

5. **Listener 資訊**
   - 哪些 Listener 監聽此事件
   - 處理邏輯說明
   - 是否為異步處理
   - Transaction 階段（BEFORE_COMMIT, AFTER_COMMIT 等）

6. **事件流程圖**
   - Mermaid 序列圖
   - 顯示從 Publisher → Event → Listener 的完整流程

7. **相關事件**
   - 通常一起觸發的事件
   - 事件鏈（Event Chain）

**文件模板參考 `references/event-doc-template.md`**

### 階段 4: 產生彙總文件

#### 4.1 README.md - 索引頁面
```markdown
# Event 文件系統

本專案的 Event-Driven 架構文件。

## 快速導航

- [完整 Event 目錄](EVENT_CATALOG.md)
- [依 Bounded Context 分類](BOUNDED_CONTEXTS.md)
- [事件流程圖](diagrams/)

## Events 列表

| Event | Bounded Context | Description |
|-------|-----------------|-------------|
| [UserCreatedEvent](events/UserCreatedEvent.md) | User Management | 使用者建立完成 |
| ... | ... | ... |

## 統計資訊

- 總 Event 數量: X
- Publisher 數量: Y
- Listener 數量: Z
- Bounded Context 數量: N
```

#### 4.2 EVENT_CATALOG.md - 完整目錄
依 Bounded Context 分組，列出所有 Event 的簡要說明。

#### 4.3 BOUNDED_CONTEXTS.md - DDD 分類
按照 Domain-Driven Design 的 Bounded Context 組織事件。

#### 4.4 整體流程圖
使用 Mermaid 產生：
- **all-events.mmd**: 所有事件的關聯圖（graph TD）
- **sequence-diagrams.mmd**: 重要業務流程的序列圖

### 階段 5: 驗證與優化

1. **檢查文件完整性**
   - 確認每個 Event 都有對應的文件
   - 驗證所有連結正確
   - 確保 Mermaid 語法正確

2. **產生檢查報告**
   ```bash
   python3 scripts/validate_docs.py docs/event/
   ```

3. **建議改進項目**
   - 找出缺少 JavaDoc 的 Event
   - 找出沒有 Listener 的 Event（可能是孤立事件）
   - 找出沒有 Publisher 的 Event（可能是未使用的類別）

## 執行指令

### 完整文件產生
```bash
# 在專案根目錄執行
python3 /path/to/scripts/generate_all_docs.py \
  --source-dir src/main/java \
  --output-dir docs/event \
  --project-name "My Project"
```

### 單一 Event 文件更新
```bash
python3 /path/to/scripts/generate_event_doc.py \
  --event UserCreatedEvent \
  --source-dir src/main/java \
  --output docs/event/events/UserCreatedEvent.md
```

### 只產生流程圖
```bash
python3 /path/to/scripts/generate_diagrams.py \
  --analysis-result analysis.json \
  --output docs/event/diagrams/
```

## 進階功能

### 增量更新
只更新有變更的 Event 文件：
```bash
# 比對 Git 變更
git diff --name-only HEAD~1 | grep -E "event|Event" | \
  xargs python3 scripts/incremental_update.py
```

### CI/CD 整合
在 `.github/workflows/event-docs.yml` 中自動觸發：
```yaml
on:
  push:
    paths:
      - 'src/**/event/**'
      - 'src/**/listener/**'
```

### 自訂模板
可以提供自訂的 Markdown 模板：
```bash
python3 scripts/generate_all_docs.py \
  --template custom-template.md
```

### AsyncAPI 規格產生（選用）
若專案使用訊息中介層（Kafka、RabbitMQ），可額外產生 AsyncAPI 規格：
```bash
python3 scripts/generate_asyncapi.py \
  --analysis-result analysis.json \
  --output docs/event/asyncapi.yaml
```

## 注意事項

1. **確保 JavaDoc 完整**：文件品質很大程度依賴原始碼的 JavaDoc，建議先完善註解。

2. **Package 命名慣例**：分析腳本假設：
   - Event 類別在 `*.event.*` 或 `*.domain.event.*` 套件
   - Listener 在 `*.listener.*` 或 `*.handler.*` 套件
   - 如果專案結構不同，可能需要調整掃描邏輯

3. **效能考量**：大型專案（>1000 個類別）掃描可能需要幾分鐘，建議使用增量更新模式。

4. **保持同步**：建議在 CI/CD 中自動執行，確保文件與程式碼同步。

## 輸出範例

詳細的輸出範例請參考 `references/example-output/`。

## 疑難排解

### 問題：找不到任何 Event
- 檢查 `--source-dir` 是否正確
- 確認 Event 類別符合命名慣例或繼承正確的父類別

### 問題：Listener 沒有被偵測到
- 確認使用 `@EventListener` 或 `@TransactionalEventListener` 註解
- 檢查 Listener 類別是否有 `@Component` 或其他 Spring stereotype 註解

### 問題：Mermaid 圖表無法渲染
- 檢查節點名稱是否包含特殊字元（建議用引號包裝）
- 確認箭頭語法正確
- 使用線上 Mermaid 編輯器驗證

## 參考資源

- Spring Events 官方文件：https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
- AsyncAPI 規格：https://www.asyncapi.com/
- Mermaid 圖表語法：https://mermaid.js.org/
- Domain-Driven Design 事件模式：參考 `references/ddd-events.md`
