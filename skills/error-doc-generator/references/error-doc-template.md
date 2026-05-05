# 錯誤案例文件模板

每個錯誤情境產出一個 `.md` 檔案，遵循以下結構：

```markdown
---
title: <錯誤名稱（繁體中文）>
component: <發生元件類別名稱>
alert_level: CRITICAL | WARNING | NONE
source_file: <原始碼相對路徑>
---

# <錯誤名稱>

## 觸發條件

<描述什麼情況下會發生此錯誤>

列出可能原因（bullet list）：
- 原因一
- 原因二
- ...

附上觸發錯誤的程式碼片段（含檔名和行號）：

```java
// <FileName>.java:<line>-<line>
<程式碼片段>
```

## 錯誤處理方式

<描述程式碼中如何處理此錯誤：>
- catch 行為（捕獲何種例外）
- 是否中斷流程
- 是否有重試機制
- 例外是否重新拋出

附上 catch 區塊的程式碼片段。

## Log 訊息

<以 code block 呈現實際會出現在 log 中的訊息格式>

```
<LOG_LEVEL> <ClassName> - <訊息格式，含佔位符說明>
```

> 若有特殊注意事項（如拼寫錯誤、重複 log 等），在此標註。

## Alert

<說明是否觸發 alert：>
- **級別**：CRITICAL / WARNING / 無
- **訊息格式**：`"<alert 訊息內容>"`
- **升級條件**：說明何時從 WARNING 升級為 CRITICAL（如資料庫連線異常）
- **節流**：說明 alert 的限流機制（如有）

若無 alert，標註「無。僅記錄 log。」

## 影響範圍

<此錯誤對系統的影響，回答以下問題：>
- 是否中斷當前請求的處理？
- 是否影響下游元件？
- 資料狀態是否會不一致或卡住？
- 是否會自動重試/恢復？
- 是否為預期行為（如上游回傳錯誤時的正常跳過）？

## 相關元件

<使用相對路徑連結到上下游相關的錯誤案例文件：>
- 上游：[<錯誤名稱>](<相對路徑>)
- 下游：[<錯誤名稱>](<相對路徑>)
- 相關：[<錯誤名稱>](<相對路徑>)
```

## 命名規則

- 檔案名稱使用英文 kebab-case
- 名稱應反映錯誤的本質，而非元件名稱
- 範例：
  - `feign-exception.md`（而非 `eai-to-ams-service-error.md`）
  - `null-token.md`（而非 `handler-illegal-argument.md`）
  - `db-connection-exception.md`（而非 `notification-service-critical.md`）
