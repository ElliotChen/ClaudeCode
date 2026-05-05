---
name: error-doc-generator
description: >
  掃描 Java/Spring Boot 專案中的錯誤處理邏輯，按請求流程階段分類，為每個錯誤情境生成獨立的文件。
  支援初次生成（initial）與後續增量更新（update）兩種模式：偵測到既有文件時自動進入更新模式，僅針對變動的案例做差異更新，並保留使用者手動維護的區塊。
  當使用者要求整理錯誤案例、產生錯誤處理文件、建立錯誤地圖、分析 exception handling 模式、或更新既有錯誤文件時使用。
  輸出為 Obsidian 相容 Markdown，含 Mermaid 流程圖，儲存於 docs/error/ 目錄下。
---

# Error Documentation Generator

掃描專案中所有錯誤處理邏輯，按請求流程階段分類，為每個錯誤情境生成獨立文件，附 README 索引與 Mermaid 流程錯誤地圖。支援 **initial（初次生成）** 與 **update（增量更新）** 兩種模式。

## Workflow

0. **Detect Mode** — 判斷是 initial 還是 update 模式
1. **Scan** — 掃描 codebase 中所有錯誤處理模式
2. **Classify** — 按請求流程階段分類
3. **Diff**（僅 update 模式）— 與既有文件比對，產出差異清單
4. **Confirm** — 與使用者確認案例清單或差異報告
5. **Generate** — 逐一產出/更新錯誤案例文件
6. **Generate README** — 產出/更新索引與流程錯誤地圖

## Step 0: Detect Mode — 判斷模式

執行任何掃描前，先檢查 `docs/error/` 目錄是否存在：

```bash
# 檢查既有文件
ls docs/error/ 2>/dev/null
find docs/error -name "*.md" -type f 2>/dev/null | wc -l
```

判定規則：

- **不存在或無 `.md` 檔** → `mode: initial`，走完整生成流程
- **已有 `.md` 檔** → `mode: update`，啟用差異更新流程

於後續步驟中明確告知使用者目前模式，例如：「偵測到既有 `docs/error/`（共 N 個案例文件），進入 **update 模式**。」

## Step 1: Scan — 掃描錯誤處理模式

遞迴搜尋專案原始碼，識別以下錯誤處理模式：

### 1.1 Exception 捕獲

```bash
# 找出所有 catch 區塊
grep -rn "catch\s*(" <project-root>/src --include="*.java"

# 找出自定義 Exception 類別
grep -rl "extends.*Exception" <project-root>/src --include="*.java"

# 找出 throw 語句
grep -rn "new\s.*Exception" <project-root>/src --include="*.java"
```

### 1.2 Error Log 訊息

```bash
# 找出所有 error level log
grep -rn "logger\.error\|log\.error" <project-root>/src --include="*.java"

# 找出 warn level log
grep -rn "logger\.warn\|log\.warn" <project-root>/src --include="*.java"
```

### 1.3 Alert / 通知機制

```bash
# 找出 alert 相關呼叫
grep -rn "alert\|notify\|notification" <project-root>/src --include="*.java" -i

# 找出 alert 級別定義
grep -rn "critical\|warning\|FATAL\|WARN" <project-root>/src --include="*.java"
```

### 1.4 Error Pattern 設定

```bash
# 檢查 application.yml 中的 error 設定
grep -rn "error" <project-root>/src/main/resources/application*.yml
```

讀取每個發現的檔案，提取：
- 錯誤觸發條件
- catch 的例外類型
- log 訊息格式（含變數佔位符）
- alert 級別（CRITICAL / WARNING / NONE）
- 錯誤後的處理行為（中斷/跳過/重試/忽略）

## Step 2: Classify — 按流程階段分類

根據專案的請求處理流程，將錯誤案例分組到對應的階段目錄。

### 分類原則

分析系統的請求處理流程（例如 Filter → Service / Processor → Handler → External API），為每個階段建立子目錄。典型的分類方式：

| 目錄 | 說明 | 常見元件 |
|------|------|---------|
| `filter/` | 請求攔截階段的錯誤 | Servlet Filter, Zuul Filter, Interceptor |
| `processor/` | 資料處理/持久化階段的錯誤 | Processor, @Async 任務 |
| `service/` | 商業邏輯處理階段的錯誤 | Service |
| `handler/` | 資訊邏輯處理階段的錯誤 | Handler, Strategy, Validator |
| `integration/` | 外部系統整合階段的錯誤 | Feign Client, RestTemplate, HTTP 呼叫 |
| `support/` | 支援性服務的錯誤 | Token, 連線管理, 排程任務 |

根據實際專案調整目錄名稱和分組。不要硬套上述範例，應依據掃描結果中的實際元件組織。

## Step 3: Diff — 比對既有文件（僅 update 模式）

對每個掃描出的錯誤案例計算穩定識別資訊：

- **case_id**：以 `<階段>/<檔名>`（不含副檔名）作為主鍵，例如 `service/feign-timeout`
- **source_hash**：對「觸發點原始碼路徑 + catch 例外類型 + log 訊息模板（不含變數值）」做 SHA1，取前 6 字元

source_hash 計算範例：

```
src/main/java/.../FeignClient.java|FeignException|"Feign call failed: {}"
↓ SHA1 ↓
a3f9c2
```

讀取每份既有文件的 frontmatter（`source_hash`、`created`、`updated`），與本次掃描結果比對，分為四類：

| 類別 | 條件 | 動作 |
|------|------|------|
| `added` | 掃描有，文件無 | 新建檔案，frontmatter 寫入 `created: <today>`、`updated: <today>` |
| `modified` | case_id 相同但 source_hash 不同 | 只重寫 `<!-- AUTO-GENERATED -->` 區塊；更新 `source_hash` 與 `updated`；保留 `created` 與 `<!-- MANUAL -->` 區塊 |
| `removed` | 文件有，掃描無 | **不刪除檔案**。在 frontmatter 加 `deprecated: <today>`、文件最上方插入 Deprecated callout，並從 README 主索引移到「歷史案例」區段 |
| `unchanged` | source_hash 相同 | 跳過，不寫入 |

對於 `modified` 類別，盡量在 diff 報告中標出**具體變動欄位**（log 訊息、catch 類型、Alert 級別、行號等），方便使用者判讀。

若既有文件缺少 `source_hash` 欄位（舊版檔案），一律歸類為 `modified`，視為強制更新一次以補上識別資訊。

## Step 4: Confirm — 與使用者確認

**Initial 模式：** 呈現完整錯誤案例清單，格式如下：

```markdown
### <階段名稱>/（N 個）

| 檔案 | 錯誤摘要 | Alert | Http Return Code | Error Code |
|------|---------|-------|------------------|-----------|
| `<filename>.md` | <一句話描述> | CRITICAL / WARNING / NONE | 400 / 500 | 2901 / 3054 |
```

**Update 模式：** 呈現 diff 報告，格式如下：

```markdown
### 變更摘要

- 新增：N 個
- 變更：N 個
- 移除：N 個（將標記為 Deprecated）
- 未變：N 個（將跳過）

### 詳細

#### + 新增（added）

| case_id | 摘要 | Alert |
|---------|------|-------|
| `filter/jwt-expired` | JWT 過期 | WARNING |

#### ~ 變更（modified）

| case_id | 變動內容 |
|---------|---------|
| `service/feign-timeout` | log 訊息變更（行 142→158）；Alert 升級 WARNING → CRITICAL |

#### - 移除（removed → 標記 Deprecated）

| case_id | 原元件 |
|---------|--------|
| `handler/legacy-xml` | LegacyXmlHandler |
```

等待使用者確認或調整後再開始寫入。**任何寫檔動作都需明確確認**。

## Step 5: Generate — 產出/更新錯誤案例文件

為每個錯誤情境建立或更新 Markdown 文件，使用標準模板。

**模板：** 見 [references/error-doc-template.md](references/error-doc-template.md)

關鍵規則：
- 所有內容使用**繁體中文**
- 檔案名稱使用英文 kebab-case（如 `feign-exception.md`）
- 程式碼片段需標注原始碼檔案名和行號
- Log 訊息使用 code block 呈現實際格式
- Alert 欄位需說明級別升級條件
- 相關元件需使用相對路徑連結到上下游錯誤案例

### 自動區與手動區隔離（重要）

模板中以 HTML 註解劃分兩種區塊：

- `<!-- AUTO-GENERATED:START -->` … `<!-- AUTO-GENERATED:END -->` — 由 skill 維護，update 模式下會被覆寫
- `<!-- MANUAL:START -->` … `<!-- MANUAL:END -->` — 由使用者手動維護，update 模式下**永不覆寫**

更新既有檔案時的處理流程：

1. 讀取既有檔案完整內容
2. 解析並抽出 `<!-- MANUAL:START -->` 至 `<!-- MANUAL:END -->` 之間的內容
3. 重新生成 AUTO-GENERATED 區塊
4. 拼接：frontmatter + Deprecated callout（若有）+ AUTO-GENERATED 區塊 + 原 MANUAL 區塊
5. 寫回檔案

### Frontmatter 規則

- `created`：僅在初次建立時寫入，後續更新**永不覆寫**
- `updated`：每次寫入時更新為當日
- `source_hash`：每次更新時重新計算
- `deprecated`：僅在案例被判定為 `removed` 時填入

### Deprecated 標記

被判定為 `removed` 的檔案，在 frontmatter 加入 `deprecated: <today>`，並在內文最上方（# 標題之上）插入 callout：

```markdown
> [!warning] Deprecated
> 本案例對應的錯誤處理邏輯已於 <date> 從 codebase 移除。保留作為歷史參考。
```

**增量寫入：** 每完成一個檔案立即寫入，避免因中斷而遺失進度。

## Step 6: Generate README — 索引與流程錯誤地圖

建立或更新 `docs/error/README.md`，包含：

### 6.1 YAML Frontmatter

```yaml
---
title: <專案名稱> 錯誤案例地圖
created: <初次建立日期，永不覆寫>
updated: <最後更新日期>
module: <模組名稱>
version: <版本號>
---
```

### 6.2 Mermaid 流程錯誤地圖

使用 Mermaid `flowchart TD` 繪製請求處理流程，在每個階段標註可能發生的錯誤，並連結到對應文件。

顏色規則：
- 黃色（`fill:#fff3cd`）：無 Alert（NONE）
- 紅色（`fill:#f8d7da`）：有 Alert（WARNING 或 CRITICAL）
- 灰色（`fill:#e9ecef`，虛線邊框）：Deprecated 案例（僅出現在歷史區段）

### 6.3 錯誤案例索引表

按階段分組的表格，欄位：

| 檔案（含連結） | 錯誤摘要 | 發生元件 | Alert 級別 | Http Return Code | Error Code |
|---------------|---------|---------|-----------|------------------|-----------|

**Update 模式下需在頁尾額外加上「歷史案例（Deprecated）」區段**，列出所有帶有 `deprecated` frontmatter 的案例，與主索引分離。

### 6.4 Alert 級別說明

列出專案中使用的 alert 級別定義、觸發方式、以及節流/限流機制（如有）。

## Important Notes

- 掃描時排除 `docs/` 目錄和測試檔案（除非使用者指定）
- 排除 JAXB/Protobuf 等自動生成的類別
- 若錯誤處理邏輯跨多個元件（如 handler 拋出 → service 捕獲），在兩者的文件中互相連結
- 對於由上層統一捕獲的例外，在 handler 文件的 Alert 欄位標註「由上層捕獲」並連結
- 檢查 `application.yml` 中的 error pattern、retry、timeout 等設定作為觸發條件的補充資料
- **Update 模式下永不直接刪除既有文件**，僅標記為 Deprecated，讓 git history 保留可追溯性
- **Update 模式下永不覆寫 `<!-- MANUAL -->` 區塊**，這是使用者長期維護的內容（排查經驗、實際 incident 紀錄等）
- 若既有文件缺少 `source_hash`（舊版檔案），一律強制更新一次以補上識別資訊
- 若使用者手動刪除某個 `<!-- AUTO-GENERATED -->` 區塊或破壞了區塊邊界，視為損壞檔案，回報使用者並要求確認後才覆寫
