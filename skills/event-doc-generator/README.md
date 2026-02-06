# Event Documentation Generator Skill

為 Spring Boot Event-Driven 架構專案自動產生完整的事件文件系統。

## 功能特色

✨ **自動掃描** - 掃描 Java 原始碼中的所有 Event 類別、Publisher 和 Listener  
📝 **獨立文件** - 為每個 Event 生成詳細的 Markdown 文件  
📊 **視覺化** - 自動產生 Mermaid 流程圖和序列圖  
🎯 **DDD 友善** - 依 Bounded Context 組織事件  
🔄 **CI/CD 整合** - 可輕鬆整合到自動化流程中  

## 快速開始

在你的 Spring Boot 專案根目錄執行：

```bash
python3 /path/to/scripts/generate_all_docs.py \
  --source-dir src/main/java \
  --output-dir docs/event
```

產生的文件結構：

```
docs/event/
├── README.md                    # 索引頁面
├── EVENT_CATALOG.md            # 完整目錄
├── BOUNDED_CONTEXTS.md         # DDD 分類
├── events/                     # 每個 Event 的詳細文件
│   ├── UserCreatedEvent.md
│   ├── OrderPlacedEvent.md
│   └── ...
└── diagrams/                   # Mermaid 流程圖
    ├── event-overview.mmd
    └── sequence-diagrams.mmd
```

## 檔案說明

### 核心腳本

- `scripts/scan_events.py` - 掃描 Event 類別
- `scripts/analyze_publishers.py` - 分析 Event Publisher
- `scripts/analyze_listeners.py` - 分析 Event Listener
- `scripts/generate_event_docs.py` - 產生個別 Event 文件
- `scripts/generate_diagrams.py` - 產生 Mermaid 流程圖
- `scripts/generate_all_docs.py` - 整合所有步驟的主程式
- `scripts/validate_docs.py` - 驗證產生的文件

### 參考資料

- `references/event-doc-template.md` - Event 文件範本
- `references/ddd-events.md` - DDD Event 模式說明
- `references/usage-examples.md` - 詳細使用範例

## 使用指南

### 基本使用

```bash
# 完整文件產生
python3 scripts/generate_all_docs.py \
  --source-dir src/main/java \
  --output-dir docs/event

# 驗證產生的文件
python3 scripts/validate_docs.py \
  --docs-dir docs/event
```

### 單獨步驟執行

```bash
# 步驟 1: 掃描 Event
python3 scripts/scan_events.py \
  --source-dir src/main/java \
  --output events.json

# 步驟 2: 分析 Publisher
python3 scripts/analyze_publishers.py \
  --source-dir src/main/java \
  --output publishers.json

# 步驟 3: 分析 Listener
python3 scripts/analyze_listeners.py \
  --source-dir src/main/java \
  --output listeners.json

# 步驟 4: 產生文件
python3 scripts/generate_event_docs.py \
  --events events.json \
  --publishers publishers.json \
  --listeners listeners.json \
  --output-dir docs/event
```

## 文件內容

每個 Event 的文件包含：

1. **基本資訊** - Package、Bounded Context、檔案路徑
2. **說明** - 從 JavaDoc 提取的說明
3. **欄位定義** - 所有欄位的型別和說明
4. **Publisher 資訊** - 誰發送此事件、在哪裡發送
5. **Listener 資訊** - 誰監聽此事件、如何處理
6. **事件流程圖** - Mermaid 序列圖展示完整流程
7. **使用場景** - 業務使用情境（需人工補充）
8. **相關事件** - 關聯的其他事件（需人工補充）

## CI/CD 整合

在 `.github/workflows/event-docs.yml`:

```yaml
name: Generate Event Documentation

on:
  push:
    paths:
      - 'src/**/event/**'
      - 'src/**/listener/**'

jobs:
  generate-docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      
      - name: Generate Documentation
        run: |
          python3 scripts/generate_all_docs.py \
            --source-dir src/main/java \
            --output-dir docs/event \
            --clean
      
      - name: Commit Changes
        run: |
          git config user.name "Doc Bot"
          git add docs/event
          git commit -m "docs: update event documentation [skip ci]" || true
          git push
```

## 需求

- Python 3.8+
- 標準 Python 函式庫（無需額外安裝套件）
- Spring Boot 專案（使用 Spring Events）

## 支援的 Event 類型

- 繼承 `ApplicationEvent` 的類別
- 標記 `@DomainEvent` 註解的類別
- 類別名稱以 `Event` 結尾的 Java Record
- 位於 `event` 或 `domain.event` 套件中的類別

## 注意事項

1. **JavaDoc 完整性** - 文件品質很大程度依賴原始碼的 JavaDoc
2. **Package 慣例** - 預設假設 Event 在 `*.event.*` 套件中
3. **編碼格式** - 原始碼必須是 UTF-8 編碼
4. **效能** - 大型專案可能需要幾分鐘掃描時間

## 疑難排解

### 找不到任何 Event

檢查：
- `--source-dir` 路徑是否正確
- Event 類別是否符合命名慣例
- Event 是否在正確的 package 中

### Listener 沒有被偵測到

檢查：
- 是否使用 `@EventListener` 或 `@TransactionalEventListener`
- Listener 方法是否為 public
- 參數型別是否正確

### Mermaid 圖表無法渲染

- 使用線上編輯器驗證：https://mermaid.live
- 確認 Markdown 預覽器支援 Mermaid

## 授權

Apache License 2.0 - 詳見 LICENSE.txt

## 參考資源

- Spring Events: https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
- Domain-Driven Design
- Event-Driven Architecture
- AsyncAPI Specification
