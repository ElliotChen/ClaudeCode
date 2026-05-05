---
name: error-doc-generator
description: >
  掃描 Java/Spring Boot 專案中的錯誤處理邏輯，按請求流程階段分類，為每個錯誤情境生成獨立的文件。
  當使用者要求整理錯誤案例、產生錯誤處理文件、建立錯誤地圖、分析 exception handling 模式時使用。
  適用場景：整理 codebase 中所有 try-catch、alert、error log 等錯誤處理邏輯並產出結構化文件。
  輸出為 Obsidian 相容 Markdown，含 Mermaid 流程圖，儲存於 docs/error/ 目錄下。
---

# Error Documentation Generator

掃描專案中所有錯誤處理邏輯，按請求流程階段分類，為每個錯誤情境生成獨立文件，附 README 索引與 Mermaid 流程錯誤地圖。

## Workflow

1. **Scan** — 掃描 codebase 中所有錯誤處理模式
2. **Classify** — 按請求流程階段分類
3. **Confirm** — 與使用者確認錯誤案例清單
4. **Generate** — 逐一產出錯誤案例文件
5. **Generate README** — 產出索引與流程錯誤地圖

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

## Step 3: Confirm — 與使用者確認

在生成文件前，向使用者呈現完整的錯誤案例清單，格式如下：

```markdown
### <階段名稱>/（N 個）

| 檔案 | 錯誤摘要 | Alert |Http Return Code | Error Code |
|------|---------|-------|---------|-------|
| `<filename>.md` | <一句話描述> | CRITICAL / WARNING / NONE | 400 / 500 | 2901 / 3054 |
```

等待使用者確認或調整後再開始生成。

## Step 4: Generate — 產出錯誤案例文件

為每個錯誤情境建立獨立 Markdown 文件，使用標準模板。

**模板：** 見 [references/error-doc-template.md](references/error-doc-template.md)

關鍵規則：
- 所有內容使用**繁體中文**
- 檔案名稱使用英文 kebab-case（如 `feign-exception.md`）
- 程式碼片段需標注原始碼檔案名和行號
- Log 訊息使用 code block 呈現實際格式
- Alert 欄位需說明級別升級條件（如資料庫連線異常升級為 CRITICAL）
- 相關元件需使用相對路徑連結到上下游錯誤案例

**增量寫入：** 每完成一個檔案立即寫入，避免因中斷而遺失進度。

## Step 5: Generate README — 索引與流程錯誤地圖

建立 `docs/error/README.md`，包含：

### 5.1 YAML Frontmatter

```yaml
---
title: <專案名稱> 錯誤案例地圖
created: <日期>
module: <模組名稱>
version: <版本號>
---
```

### 5.2 Mermaid 流程錯誤地圖

使用 Mermaid `flowchart TD` 繪製請求處理流程，在每個階段標註可能發生的錯誤，並連結到對應文件。

顏色規則：
- 黃色（`fill:#fff3cd`）：無 Alert（NONE）
- 紅色（`fill:#f8d7da`）：有 Alert（WARNING 或 CRITICAL）

### 5.3 錯誤案例索引表

按階段分組的表格，欄位：

| 檔案（含連結） | 錯誤摘要 | 發生元件 | Alert 級別 | Http Return Code | Error Code |
|---------------|---------|---------|-----------|-----------|-----------|

### 5.4 Alert 級別說明

列出專案中使用的 alert 級別定義、觸發方式、以及節流/限流機制（如有）。

## Important Notes

- 掃描時排除 `docs/` 目錄和測試檔案（除非使用者指定）
- 排除 JAXB/Protobuf 等自動生成的類別
- 若錯誤處理邏輯跨多個元件（如 handler 拋出 → service 捕獲），在兩者的文件中互相連結
- 對於由上層統一捕獲的例外，在 handler 文件的 Alert 欄位標註「由上層捕獲」並連結
- 檢查 `application.yml` 中的 error pattern、retry、timeout 等設定作為觸發條件的補充資料
