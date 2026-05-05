---
name: architecture-doc-generator
description: 為 Spring Boot 與 DDD 專案生成完整的軟體架構文件。當使用者要求產生架構文件、系統設計文件、技術規格，或要求記錄其 Spring Boot/微服務/DDD 系統架構時使用。輸出至 docs/architecture 目錄，包含涵蓋系統總覽、DDD 設計、Spring Boot 實作、整合模式和部署策略的結構化 Markdown 文件。
---

# 架構文件產生器

為 Spring Boot 和領域驅動設計 (DDD) 專案產生完整、符合生產標準的架構文件。

## 核心工作流程

本 skill 支援兩種模式，**請於每次任務開始時先執行「步驟 0：偵測模式」**：

- **生成模式（Generate）**：`docs/architecture/` 目錄不存在或為空時，從零產生完整文件集。
- **更新模式（Update）**：`docs/architecture/` 目錄已有文件時，自動切換為「智慧增量更新」，僅修改差異段落，保留其餘內容與使用者自訂修改。

### 0. 偵測模式（必執行第一步）

在執行任何其他步驟之前，先判斷目前應進入哪個模式：

```
1. 檢查目標路徑 docs/architecture/ 是否存在
2. 若不存在，或存在但目錄為空 → 進入「生成模式」，繼續步驟 1
3. 若存在且包含至少一份 .md 文件 → 進入「更新模式」，跳到下方「更新現有文件」章節
```

實作步驟：

1. 使用 `ls` 或 `Glob` 檢查 `docs/architecture/*.md` 是否存在
2. 列出已存在的文件清單與最後修改日期，向使用者顯示偵測結果，例如：
   - 「偵測到 `docs/architecture/` 已有 8 份文件，將進入**更新模式**並執行智慧增量更新。」
   - 「未偵測到既有架構文件，將進入**生成模式**從頭產生完整文件集。」
3. 若使用者明確指示「重新生成全部 / 重做 / regenerate」等關鍵字，可由使用者覆寫為生成模式（會覆蓋既有文件，建議先 Git commit）

> 進入更新模式後，請參考下方「更新現有文件」章節執行流程；以下步驟 1-6 為「生成模式」的工作流程。

---

### 生成模式工作流程

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

針對每份文件，使用 `references/` 目錄中的範本作為指引：

- `document-templates-foundation.md` - 01-04 文件範本（系統總覽到系統架構）
- `document-templates-tech-data.md` - 05-06 文件範本（技術堆疊與資料架構）
- `document-templates-implementation-testing.md` - 07-08, 10 文件範本（實作、橫切關注點、測試）
- `document-templates-deployment.md` - 09 文件範本（部署架構）

關鍵原則：

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
- [DDD設計](./03-DDD設計.md) - Bounded Context、Aggregate 與領域模型

### 技術架構
- [系統架構](./04-系統架構.md) - 架構視圖與通訊模式
- [技術堆疊](./05-技術堆疊.md) - 技術選型與整合方式
- [資料架構](./06-資料架構.md) - 資料模型、Repository 與持久化

### 實作細節
- [SpringBoot實作](./07-SpringBoot實作.md) - Spring Boot 特定實作指南
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

**範本在**：`references/readme-template.md`

使用此範本客製化專案的 README.md，包含：
- 實際的專案名稱
- 已產生的文件列表（可能不是全部 10 份）
- 專案特定的閱讀建議
- 當前日期與版本資訊

## 文件範本

文件範本分為四個檔案，涵蓋所有 10 份架構文件：

- `references/document-templates-foundation.md` - 系統總覽、架構目標、DDD 設計、系統架構
- `references/document-templates-tech-data.md` - 技術堆疊、資料架構
- `references/document-templates-implementation-testing.md` - Spring Boot 實作、橫切關注點、測試策略
- `references/document-templates-deployment.md` - 部署架構

產生特定文件時載入對應的範本檔案。

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

## 更新現有文件

當專案程式碼或架構變更後，需要更新已產生的文件時，遵循以下「智慧增量更新」工作流程。**進入此章節的前提是步驟 0 已偵測到既有文件**。

### 智慧增量更新原則

1. **永遠先讀後寫**：使用 view/Read 完整讀取既有文件，再決定要改什麼，禁止盲目覆寫
2. **以差異為單位**：比對「最新原始碼/輸入」與「既有文件描述」，列出差異點清單後才動工
3. **只改必要段落**：使用 `str_replace` 精確替換，**不重寫整份文件**，未受影響的章節保持原樣
4. **保留使用者自訂**：辨識並保留 `<!-- 自訂內容開始 -->` ~ `<!-- 自訂內容結束 -->` 區塊；對於明顯人工撰寫的段落（如團隊註解、特殊說明），優先保留並僅在必要時提示使用者
5. **同步更新交叉引用**：圖表、README 連結、版本號需一併校正
6. **記錄變更**：每份被修改文件 frontmatter 的 `date` 應更新為當日，並在 README 留下更新摘要

### 比對與差異識別流程

進入更新模式後，依序執行：

```
1. 讀取既有文件（view 全部 docs/architecture/*.md）
2. 收集最新輸入：
   - 使用者描述的變更（新功能、技術升級、新增 Context...）
   - 必要時讀取程式碼（pom.xml/build.gradle、新增的 @Service/@Aggregate、Dockerfile 等）
3. 列出「差異清單」：
   - 哪些技術版本變了
   - 哪些 Bounded Context / Aggregate 是新的
   - 哪些圖表節點需要新增/移除
   - 哪些章節描述已過時
4. 將差異清單對映到「受影響文件 × 區段」
5. 與使用者確認差異清單後，才開始執行 str_replace
```

### 更新前檢查

1. **檢測現有文件**：
   - 檢查 `docs/architecture/` 目錄是否存在
   - 列出已存在的文件清單
   - 詢問使用者更新意圖

2. **確認更新範圍**：
   - 詢問哪些部分發生變更（新增 Context、技術升級、架構調整等）
   - 確認需要更新的文件範圍
   - 提醒使用者先用 Git commit 保存現有版本

### 更新策略

**策略 1：完整重新生成**（適用於大規模重構）
```
使用情境：系統架構大幅調整、技術堆疊全面升級
作法：重新執行完整工作流程，覆蓋所有文件
風險：會覆蓋使用者的自訂修改
建議：使用前確保已 Git commit，更新後使用 git diff 檢查差異
```

**策略 2：增量更新**（建議方式）
```
使用情境：新增功能、局部調整、新增整合
作法：僅更新受影響的文件章節
保留：不受影響的章節與使用者自訂內容
```

**策略 3：新增補充文件**
```
使用情境：新增 Bounded Context、新增 ADR
作法：在現有文件中新增章節，或建立補充文件
保留：完整保留現有內容
```

### 常見更新情境

#### 情境 1：新增 Bounded Context

**影響文件**：
- `03-DDD設計.md` - 新增 Context 描述與 Context Map
- `04-系統架構.md` - 更新容器圖與元件圖
- `06-資料架構.md` - 新增資料庫綱要（如需要）
- `README.md` - 更新 Context 列表

**更新方式**：
在 03-DDD設計.md 中找到「Bounded Context」章節，在現有 Context 之後新增新的 Context 描述。更新 Context Map 圖表，加入新的關係線。

#### 情境 2：技術堆疊升級

**影響文件**：
- `05-技術堆疊.md` - 更新版本資訊與設定
- `07-SpringBoot實作.md` - 更新程式碼範例（如 API 變更）
- `09-部署架構.md` - 更新 Dockerfile 基礎映像
- `README.md` - 更新技術堆疊摘要表

**更新方式**：
使用 str_replace 精確替換版本資訊。檢查是否有 breaking changes 需要更新程式碼範例。

#### 情境 3：新增架構決策 (ADR)

**影響文件**：
- `02-架構目標.md` - 在 ADR 章節新增

**更新方式**：
在「架構決策記錄」章節找到最後一個 ADR，在其後新增新的 ADR（編號遞增）。保持格式一致：背景、決策、後果、替代方案、狀態。

#### 情境 4：部署環境調整

**影響文件**：
- `09-部署架構.md` - 更新 Kubernetes manifest、HPA 設定等

**更新方式**：
找到特定的 YAML 區塊（如 Deployment、HPA），使用 str_replace 替換舊設定。確保更新後的設定與實際環境一致。

#### 情境 5：新增外部服務整合

**影響文件**：
- `04-系統架構.md` - 更新情境圖，加入外部系統
- `05-技術堆疊.md` - 新增第三方整合章節
- `07-SpringBoot實作.md` - 新增 Anticorruption Layer 範例

**更新方式**：
在相關章節找到適當位置插入新內容。更新 Mermaid 圖表加入新的外部系統節點。提供整合的程式碼範例。

### 更新工作流程

1. **準備階段**：
   - 掃描現有文件
   - 詢問變更類型與範圍
   - 建議受影響的文件清單
   - 提醒先 Git commit

2. **執行更新**：
   - 使用 view 工具讀取現有文件
   - 使用 str_replace 精確替換需要更新的部分
   - 保留不受影響的章節
   - 更新相關的 Mermaid 圖表

3. **驗證更新**：
   - 檢查文件間的交叉引用是否一致
   - 確認圖表與文字描述相符
   - 驗證程式碼範例的正確性
   - 更新 README.md 中的「最後更新」日期

4. **版本記錄**：
   - 在文件 frontmatter 更新日期
   - 在 README.md 記錄此次更新內容
   - 建議使用者建立 Git commit

### 保留使用者自訂內容

**使用註解標記**（建議使用者採用）：
```markdown
<!-- 自訂內容開始 -->
這裡是團隊自行新增的特定說明...
<!-- 自訂內容結束 -->
```

更新時，Claude 會：
- 識別並保留 `<!-- 自訂內容開始 -->` 和 `<!-- 自訂內容結束 -->` 之間的內容
- 僅更新標記之外的自動生成內容
- 提醒使用者檢查自訂內容是否仍然適用

## 使用範例

### 範例 1：初次產生文件

**使用者**：「請為我們使用 Spring Boot 和 DDD 建構的訂單管理微服務產生架構文件。」

**處理流程**：
1. 詢問 Bounded Context（訂單、庫存、出貨？）
2. 確認技術堆疊（PostgreSQL、Kafka、Redis？）
3. 在 `docs/architecture/` 建立全部 10 份文件
4. 產生展示關係的 Context Map
5. 記錄 Spring Boot 整合模式
6. 包含來自其領域的具體範例
7. 產生 README.md 提供導航

### 範例 2：更新現有文件

**使用者**：「我們升級到 Spring Boot 3.3 並新增了 Redis Sentinel 支援，請更新文件。」

**處理流程**：
1. 掃描發現已存在文件
2. 識別需要更新的文件：`05-技術堆疊.md`、`06-資料架構.md`
3. 更新 Spring Boot 版本號（3.2.x → 3.3.x）
4. 在快取章節新增 Redis Sentinel 設定
5. 更新相關的 YAML 設定範例
6. 更新 README.md 的技術堆疊表格
7. 更新「最後更新」日期

### 範例 3：新增 Bounded Context

**使用者**：「我新增了一個 Notification Context，請更新架構文件。」

**處理流程**：
1. 掃描 `docs/architecture/` 目錄，發現已存在文件
2. 詢問：「請描述 Notification Context 的職責與它和其他 Context 的關係」
3. 使用 view 讀取 `03-DDD設計.md`
4. 在 Bounded Context 章節新增 Notification Context 描述
5. 更新 Context Map Mermaid 圖表，加入 Notification 節點與關係
6. 使用 view 讀取 `04-系統架構.md`
7. 更新容器圖，加入 notification-service
8. 更新 `README.md` 的 Context 列表
9. 提示：「已更新 3 份文件，請使用 git diff 檢查變更」

## 參考資料

- `references/document-templates-foundation.md` - 01-04 文件範本
- `references/document-templates-tech-data.md` - 05-06 文件範本
- `references/document-templates-implementation-testing.md` - 07-08, 10 文件範本
- `references/document-templates-deployment.md` - 09 文件範本
- `references/diagram-examples.md` - Mermaid 圖表範本與範例
- `references/ddd-patterns.md` - Spring Boot 中常見 DDD 模式實作
- `references/readme-template.md` - README.md 範本
