# DDD 與 DFD 對應規則

## DFD 元素對應表

| DFD 元素 | DDD 對應概念 | 命名規範 | 範例 |
|---------|-------------|---------|------|
| 外部實體 | 外部系統 / 使用者角色 | 業務角色名稱 | 「買家」、「物流服務商」 |
| Process（L1） | Bounded Context | Context 名稱 | 「訂單管理」、「庫存管理」 |
| Process（L2） | Application Service | Use Case 動詞 + 名詞 | 「建立訂單」、「查詢庫存」 |
| Data Store | Aggregate Repository | Aggregate 名稱 + 儲存 | 「訂單聚合儲存」、「商品目錄儲存」 |
| 資料流 | Command / Event / Query | Ubiquitous Language | 「訂單已建立事件」、「庫存扣減指令」 |

## 資料流語義規則

### 同步資料流（實線箭頭）

對應 Spring Boot 中的：
- REST API 呼叫（`@RestController` → `@Service`）
- gRPC 呼叫
- 同步 Domain Service 呼叫

標註格式：`{資料內容}（{協定}）`
範例：`訂單明細（REST）`

### 非同步資料流（虛線箭頭）

對應 Spring Boot 中的：
- Spring Application Event（`ApplicationEventPublisher`）
- Kafka / RabbitMQ 訊息
- Spring Integration 通道

標註格式：`{事件名稱}（{機制}）`
範例：`訂單已建立事件（Kafka）`

### 跨 Context 資料流

- 必須經過 ACL 轉譯，標籤使用**目標 Context 的語言**
- 不可直接傳遞內部 Entity 或 Value Object
- 若使用 Published Language，標註協定格式（如 JSON Schema、Protobuf）

## Data Store 規則

### 基本原則

- 每個 Bounded Context 擁有獨立的 Data Store
- 命名使用 Aggregate Root 名稱，不使用資料庫表名
- 不同 Context 即使物理上共用資料庫，DFD 上仍畫為獨立的 Data Store

### CQRS 場景

```
寫模型儲存（Command Store）
├── 對應：Aggregate Repository
├── 存取者：Command 端 Application Service
└── 寫入方向：Process → Data Store

讀模型儲存（Query Store）
├── 對應：Read Model / Projection
├── 存取者：Query 端 Application Service
└── 讀取方向：Data Store → Process

投影同步（Projection Sync）
├── 類型：非同步資料流
├── 方向：寫模型儲存 → 讀模型儲存
└── 標註：「領域事件投影（非同步）」
```

### Event Sourcing 場景

- Event Store 畫為獨立的 Data Store
- 標註「僅追加寫入」特性
- Snapshot Store 若存在，畫為另一個 Data Store

## Spring Boot 技術元件對應

以下技術元件**不應**在 DFD 中作為獨立 Process 出現：

| 技術元件 | 處理方式 |
|---------|---------|
| `@RestController` | 歸入對應的 Application Service Process |
| `@EventListener` | 歸入接收事件的 Process |
| Message Broker（Kafka/RabbitMQ） | 以非同步資料流箭頭表示，不畫為 Process |
| API Gateway | 視為外部實體或在 Level 0 中省略 |
| Service Registry（Eureka） | 不在 DFD 中呈現 |
| Config Server | 不在 DFD 中呈現 |

以下元件**可以**作為獨立 Process：

| 技術元件 | 條件 |
|---------|------|
| Saga / Process Manager | 協調多個 BC 的狀態流轉時 |
| Scheduler（`@Scheduled`） | 觸發獨立業務流程時 |
| Batch Job（Spring Batch） | 構成獨立的資料處理流程時 |
