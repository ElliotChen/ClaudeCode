# DFD 層次分解指南

## Level 0：系統上下文圖（Context Diagram）

將整個系統視為單一 Process，聚焦於：

- 系統與哪些**外部實體**互動（使用者、第三方系統、外部服務）
- 每條資料流的**方向與內容**（輸入/輸出各攜帶什麼資料）

### 注意事項

- 不呈現系統內部細節
- 外部實體的命名應使用業務角色（如「供應商」）而非技術名稱（如「ERP API」）
- 若系統有多種使用者角色，應分別列出（如「買家」、「賣家」、「管理員」）

## Level 1：Bounded Context 分解

將 Level 0 的系統 Process 拆解為多個子 Process，每個對應一個 Bounded Context。

### 分解原則

1. **一個 BC 對應一個 Process** — 不要將多個 BC 合併
2. **Context Map 決定資料流** — 參考 DDD Context Map 中的關係類型：
   - **上游/下游（Upstream/Downstream）**：箭頭方向反映依賴方向
   - **共享核心（Shared Kernel）**：標註為雙向資料流
   - **防腐層（ACL）**：在資料流上標註「經 ACL 轉譯」
   - **開放主機服務（OHS）+ 發佈語言（PL）**：標註使用的協定
3. **Data Store 歸屬明確** — 每個 Data Store 只能被一個 BC 的 Process 直接存取

### 常見錯誤

- ❌ 跨 BC 直接存取同一個 Data Store
- ❌ 資料流標籤使用 DTO class name（如 `OrderDTO`）
- ❌ 將技術中介（如 Message Broker）畫成 Process

## Level 2：Application Service 展開

針對關鍵 Bounded Context，展開其內部的 Application Service / Use Case。

### 展開時機

- 該 BC 邏輯複雜，包含 3 個以上的核心 Use Case
- 需要釐清 BC 內部的資料流轉路徑
- 有 CQRS 或 Event Sourcing 需要呈現

### 展開原則

1. **Process 對應 Application Service** — 每個 Process 代表一個完整的用例
2. **Domain Service 通常不獨立呈現** — 除非它協調多個 Aggregate 且有獨立的外部資料流
3. **CQRS 分離呈現**：
   - Command 端：寫入操作的 Process → 寫模型 Data Store
   - Query 端：查詢操作的 Process → 讀模型 Data Store
   - 兩者之間以投影/同步事件連接

## Level 3+：謹慎使用

僅在以下情況展開：

- Saga / Process Manager 的狀態流轉需要視覺化
- 跨 Aggregate 的複雜協調邏輯
- 團隊需要理解特定 Domain Service 的內部資料流

> ⚠️ 過度細化會讓 DFD 退化為程式碼的鏡像，失去抽象溝通的價值。
> 若 Level 3 的 Process 數量超過 7 個，考慮是否應重新劃分 Bounded Context。
