# 錯誤案例文件模板

每個錯誤情境產出一個 `.md` 檔案，遵循以下結構。模板以 HTML 註解劃分兩種區塊：

- **AUTO-GENERATED 區塊**：由 skill 維護，update 模式下會被整段覆寫
- **MANUAL 區塊**：由使用者手動維護，update 模式下**永不覆寫**

```markdown
---
title: <錯誤名稱（繁體中文）>
component: <發生元件類別名稱>
alert_level: CRITICAL | WARNING | NONE
source_file: <原始碼相對路徑>
source_hash: <SHA1 前 6 字元，由 skill 自動計算與更新>
created: <YYYY-MM-DD，初次建立日期，後續永不變動>
updated: <YYYY-MM-DD，最後一次自動更新日期>
deprecated: <YYYY-MM-DD，僅在案例已從 codebase 移除時填入；否則省略此欄位>
---

<!--
若 frontmatter 的 deprecated 有值，於此處（# 標題之上）插入下列 callout；
否則不插入此區塊。
-->
> [!warning] Deprecated
> 本案例對應的錯誤處理邏輯已於 <date> 從 codebase 移除。保留作為歷史參考。

# <錯誤名稱>

<!-- AUTO-GENERATED:START -->

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

<!-- AUTO-GENERATED:END -->

<!-- MANUAL:START -->

## 排查經驗 / 備註

> 此區塊由使用者手動維護，update 模式下不會被覆寫。
> 可記錄：實際發生過的 incident、排查步驟、修復紀錄、root cause 分析、相關 ticket 連結等。

<!-- MANUAL:END -->
```

## 命名規則

- 檔案名稱使用英文 kebab-case
- 名稱應反映錯誤的本質，而非元件名稱
- 範例：
  - `feign-exception.md`（而非 `eai-to-ams-service-error.md`）
  - `null-token.md`（而非 `handler-illegal-argument.md`）
  - `db-connection-exception.md`（而非 `notification-service-critical.md`）

## source_hash 計算規則

對下列三個欄位以 `|` 串接後做 SHA1，取前 6 字元：

```
<source_file 相對路徑>|<catch 的例外類型>|<log 訊息模板（不含變數值）>
```

範例：

```
src/main/java/.../FeignClient.java|FeignException|"Feign call failed: {}"
↓ SHA1 ↓
a3f9c2
```

當其中任一輸入變動時，hash 會改變，diff 步驟即可偵測到該案例需要更新。

> **注意**：log 訊息模板需移除實際變數值（保留 `{}` 或 `%s` 等佔位符），避免每次重跑因執行期參數不同而誤判為變更。

## 自動區與手動區邊界

- **AUTO-GENERATED 區塊**內含章節：觸發條件、錯誤處理方式、Log 訊息、Alert、影響範圍、相關元件
- **MANUAL 區塊**內含章節：排查經驗 / 備註，以及任何使用者主動加入的章節

如需新增使用者自訂章節，請放在 `<!-- MANUAL:START -->` 與 `<!-- MANUAL:END -->` 之間，否則 update 時會被視為過期內容並被覆寫。

## Update 模式下的檔案處理流程

skill 在更新既有檔案時：

1. 讀取整份檔案
2. 解析 frontmatter，保留 `created` 原值
3. 抽出 `<!-- MANUAL:START -->` 至 `<!-- MANUAL:END -->` 之間的全部內容（含章節標題）
4. 重新生成 AUTO-GENERATED 區塊
5. 重新組合：新 frontmatter（更新 `updated`、`source_hash`）+ Deprecated callout（若有）+ `# 標題` + 新 AUTO-GENERATED 區塊 + 原 MANUAL 區塊
6. 寫回檔案

若解析時找不到 MANUAL 邊界註解（檔案損壞或被手動編輯破壞結構），skill 應停止寫入並回報使用者，由使用者確認後才強制覆寫。
