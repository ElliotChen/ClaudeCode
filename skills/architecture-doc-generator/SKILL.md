---
name: architecture-doc-generator
description: 為 Spring Boot 與 DDD 專案生成完整的軟體架構文件。當使用者要求產生架構文件、系統設計文件、技術規格，或要求記錄其 Spring Boot/微服務/DDD 系統架構時使用。輸出至 docs/architecture 目錄，包含涵蓋系統總覽、DDD 設計、Spring Boot 實作、整合模式和部署策略的結構化 Markdown 文件。
---

# 架構文件產生器

為 Spring Boot 和領域驅動設計 (DDD) 專案產生完整、符合生產標準的架構文件。

## 核心工作流程

按照以下步驟產生完整的架構文件：

### 1. 了解專案背景

若尚未掌握以下關鍵資訊，請向使用者詢問：
- 專案類型（單體、微服務、模組化單體）
- Bounded Context 及其關係
- 主要技術堆疊（資料庫、訊息系統、快取等）
- 部署環境（Kubernetes、雲端平台、地端）
- 非功能性需求（效能、擴展性、安全性目標）

### 2. 建立文件結構

在 `docs/architecture` 中建立以下目錄和檔案結構：

```
docs/architecture/
├── 01-系統總覽.md              # 系統概述與背景
├── 02-架構目標.md              # 品質屬性與限制條件
├── 03-DDD設計.md              # DDD 戰略與戰術設計
├── 04-系統架構.md              # 架構視圖與圖表
├── 05-技術堆疊.md              # 技術選型與整合
├── 06-資料架構.md              # 資料模型與持久化
├── 07-SpringBoot實作.md       # Spring Boot 特定實作
├── 08-橫切關注點.md            # 安全性、監控、錯誤處理
├── 09-部署架構.md              # 基礎設施與部署
├── 10-測試策略.md              # 測試方法
└── diagrams/                   # Mermaid 圖表原始檔
    ├── context.mmd
    ├── container.mmd
    ├── bounded-contexts.mmd
    └── deployment.mmd
```

### 3. 產生文件內容

針對每份文件，使用 `references/document-templates.md` 中的範本作為指引。關鍵原則：

**具體明確**：包含實際的技術版本、使用的具體模式、來自專案的實際範例。

**包含圖表**：所有架構圖使用 Mermaid 語法。原始檔儲存在 `diagrams/` 目錄並嵌入文件中。

**DDD 重點**：針對 DDD 專案，強調 Bounded Context 邊界、Aggregate 設計、Domain Event 和 Context Map 關係。

**Spring Boot 模式**：記錄 Spring 特定實作，如 Bean 生命週期、`@Transactional` 使用、Repository 模式、使用 `@EventListener` 的事件處理。

**決策記錄**：記錄關鍵架構決策的理由、考慮過的替代方案和權衡取捨。

### 4. 針對專案類型客製化

**微服務專案**：
- 新增服務發現模式（Consul、Eureka、Kubernetes DNS）
- 記錄服務間通訊（REST、gRPC、訊息傳遞）
- 包含分散式交易模式（Saga、最終一致性）
- 新增服務網格考量（Istio、Linkerd）

**模組化單體**：
- 記錄模組邊界與相依性
- 說明套件結構與相依性規則
- 包含模組間通訊模式

**事件驅動系統**：
- 詳述 Domain Event 定義與處理器
- 記錄訊息代理整合（Kafka、RabbitMQ）
- 如適用，包含事件溯源模式

### 5. 驗證檢查清單

完成前，確認每份文件包含：

- [ ] 清晰的章節標題與目錄
- [ ] 視覺化呈現的 Mermaid 圖表
- [ ] 具體的程式碼範例（不只是介面）
- [ ] 關鍵選擇的決策理由
- [ ] 相關文件之間的連結
- [ ] 所有技術的版本資訊
- [ ] 非功能性需求對應

### 6. 生成 README.md

最後一步，在 `docs/architecture` 目錄中建立 `README.md` 檔案：

**目的**：
- 提供架構文件的總覽與導航
- 列出所有已產生的文件並加上連結
- 簡要說明每份文件的用途
- 提供文件的閱讀順序建議

**內容結構**：
```markdown
# 架構文件

本目錄包含 [專案名稱] 的完整架構文件。

## 📚 文件總覽

以下是本專案的架構文件清單：

### 系統總覽與設計
- [系統總覽](./01-系統總覽.md) - 系統背景、範疇與目標
- [架構目標](./02-架構目標.md) - 品質屬性、限制條件與架構決策
- [DDD 設計](./03-DDD設計.md) - Bounded Context、Aggregate 與領域模型

### 技術架構
- [系統架構](./04-系統架構.md) - 架構視圖與通訊模式
- [技術堆疊](./05-技術堆疊.md) - 技術選型與整合方式
- [資料架構](./06-資料架構.md) - 資料模型、Repository 與持久化

### 實作細節
- [Spring Boot 實作](./07-SpringBoot實作.md) - Spring Boot 特定實作指南
- [橫切關注點](./08-橫切關注點.md) - 安全性、監控、錯誤處理

### 部署與測試
- [部署架構](./09-部署架構.md) - 基礎設施、容器化與 Kubernetes
- [測試策略](./10-測試策略.md) - 測試方法與範例

## 🎯 快速開始

**首次閱讀建議順序**：
1. 先閱讀「系統總覽」了解專案背景
2. 查看「DDD 設計」理解領域模型
3. 參考「系統架構」掌握整體架構
4. 深入「Spring Boot 實作」了解技術細節

**針對特定需求**：
- 🏗️ 了解系統設計 → 系統總覽、架構目標、系統架構
- 💻 開發新功能 → DDD 設計、Spring Boot 實作、資料架構
- 🚀 部署與維運 → 部署架構、橫切關注點
- 🧪 撰寫測試 → 測試策略

## 📊 架構圖表

重要的架構圖表包含在 `diagrams/` 目錄中：
- `context.mmd` - 系統情境圖
- `container.mmd` - 容器架構圖
- `bounded-contexts.mmd` - Bounded Context Map
- `deployment.mmd` - 部署架構圖

## 🔄 文件維護

- **更新頻率**：架構變更時應即時更新相關文件
- **版本控制**：所有文件納入 Git 版本控制
- **責任人**：架構師負責維護文件正確性與即時性

## 📝 文件產生資訊

- **產生日期**：YYYY-MM-DD
- **產生工具**：Claude Architecture Doc Generator
- **專案版本**：v1.0.0
```

**範本在**：`references/README範本.md`

使用此範本客製化專案的 README.md，包含：
- 實際的專案名稱
- 已產生的文件列表（可能不是全部 10 份）
- 專案特定的閱讀建議
- 當前日期與版本資訊

## 文件範本

每種文件類型的詳細範本在 `references/document-templates.md` 中。產生特定文件時載入此檔案。

## 圖表指引

所有圖表使用 Mermaid。常見圖表類型：

**情境圖 (Context Diagram)**：系統邊界與外部相依性
**容器圖 (Container Diagram)**：主要應用元件與資料儲存
**Bounded Context Map**：DDD 情境關係
**部署圖 (Deployment Diagram)**：基礎設施與執行時架構
**循序圖 (Sequence Diagram)**：關鍵業務流程
**元件圖 (Component Diagram)**：內部模組結構

參見 `references/diagram-examples.md` 的 Mermaid 範本。

## Spring Boot 與 DDD 細節

記錄 Spring Boot + DDD 專案時：

1. **層級對應**：清楚對應 DDD 層級到 Spring 刻板印象（`@Service`、`@Component`、`@Repository`）
2. **套件結構**：記錄使用 layer-first 或 context-first 組織方式
3. **相依性規則**：說明領域層如何保持框架獨立
4. **Repository 模式**：展示 JPA Repository 實作 DDD Repository 介面
5. **Domain Event**：記錄 Spring Event vs 訊息代理的使用
6. **交易邊界**：釐清 `@Transactional` 範圍（Application Service 層）
7. **Aggregate 持久化**：展示 `@Embeddable` Value Object、`@Version` 樂觀鎖

## 輸出格式

- 所有檔案採用 Markdown 格式
- UTF-8 編碼
- 使用繁體中文
- 包含 YAML frontmatter（標題與日期）
- 程式碼區塊使用適當的語言語法高亮

## 使用範例

**使用者**：「請為我們使用 Spring Boot 和 DDD 建構的訂單管理微服務產生架構文件。」

**處理流程**：
1. 詢問 Bounded Context（訂單、庫存、出貨？）
2. 確認技術堆疊（PostgreSQL、Kafka、Redis？）
3. 在 `docs/architecture/` 建立全部 10 份文件
4. 產生展示關係的 Context Map
5. 記錄 Spring Boot 整合模式
6. 包含來自其領域的具體範例

## 參考資料

- `references/document-templates.md` - 每種文件類型的詳細範本
- `references/diagram-examples.md` - Mermaid 圖表範本與範例
- `references/ddd-patterns.md` - Spring Boot 中常見 DDD 模式實作
