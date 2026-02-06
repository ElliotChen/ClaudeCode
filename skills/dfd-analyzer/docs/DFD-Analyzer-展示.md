# DFD Analyzer - 實際運作展示

## 範例專案結構

這是一個簡單的訂單管理微服務專案：

```
example-spring-project/
└── src/main/java/com/example/order/
    ├── controller/
    │   └── OrderController.java       # @RestController - 3 個 API endpoints
    ├── service/
    │   └── OrderService.java          # @Service - 業務邏輯
    ├── repository/
    │   └── OrderRepository.java       # @Repository - 資料存取
    ├── entity/
    │   └── Order.java                 # @Entity - JPA 實體
    └── client/
        ├── PaymentClient.java         # @FeignClient - 付款服務
        └── InventoryClient.java       # @FeignClient - 庫存服務
```

## 分析結果

### 檢測到的元件

#### 外部實體 (3 個)
1. **API Client** - 呼叫 OrderController 的前端或其他服務
2. **payment-service** - 外部付款處理服務
3. **inventory-service** - 外部庫存管理服務

#### 處理程序 (3 個)
1. **OrderController**
   - 類型: controller
   - 方法: createOrder, getOrder, cancelOrder
   - 依賴: OrderService

2. **OrderService**
   - 類型: service
   - 方法: processOrder, findById, cancelOrder
   - 依賴: OrderRepository, PaymentClient, InventoryClient

3. **OrderRepository**
   - 類型: repository
   - 方法: findById
   - 依賴: (無，直接連接資料庫)

#### 資料儲存 (1 個)
1. **orders 資料表**
   - 類型: database
   - 對應實體: Order
   - 欄位: id, customerId, productId, status

#### 資料流 (2 個主要流程)
1. OrderController → OrderService (request)
2. OrderService → OrderRepository (query)

---

## Level 0: Context Diagram (系統邊界圖)

這個圖展示了系統與外部實體的互動關係：

```mermaid
graph TB
    %% Context Diagram - Level 0

    system["System<br/>(Application)"]
    node1[["API Client (OrderController)"]]
    node1 -->|Requests| system
    system -->|Responses| node1
    node2[["payment-service"]]
    system -->|API Calls| node2
    node3[["inventory-service"]]
    system -->|API Calls| node3
    node4[[("orders")]]
    system <-->|Data| node4

    %% Styling
    classDef systemNode fill:#ffecb3,stroke:#ff6f00,stroke-width:3px
    classDef externalEntity fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    classDef dataStore fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    class system systemNode
    class node1 externalEntity
    class node2 externalEntity
    class node3 externalEntity
    class node4 dataStore
```

### Context Diagram 說明

- **系統邊界**: 訂單管理系統 (橘色方框)
- **外部實體**:
  - API Client: 發送 HTTP 請求並接收回應
  - payment-service: 處理付款交易
  - inventory-service: 檢查商品庫存
- **資料儲存**: orders 資料表，存放訂單資料

---

## Level 1: High-Level DFD (高階資料流程圖)

這個圖展示了系統內部的主要元件及其互動：

```mermaid
graph TB
    %% External Entities
    node1[["API Client (OrderController)<br/>(client)"]]
    node2[["payment-service<br/>(external_service)"]]
    node3[["inventory-service<br/>(external_service)"]]

    %% Processes
    node4["OrderController<br/>(controller)<br/>3 methods"]
    node5["OrderService<br/>(service)<br/>3 methods"]
    node6["OrderRepository<br/>(repository)<br/>1 methods"]

    %% Data Stores
    node7[[("orders<br/>(Order)")]]

    %% Data Flows
    node4 -->|request| node5
    node5 -->|query| node6
    node1 -->|HTTP Request| node4
    node6 <-->|CRUD| node7

    %% Styling
    classDef externalEntity fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    classDef process fill:#fff9c4,stroke:#f57f17,stroke-width:2px
    classDef dataStore fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    class node1 externalEntity
    class node2 externalEntity
    class node3 externalEntity
    class node4 process
    class node5 process
    class node6 process
    class node7 dataStore
```

### High-Level DFD 說明

#### 資料流程

1. **API 請求處理**
   - API Client 發送 HTTP 請求到 OrderController
   - OrderController 接收並驗證請求

2. **業務邏輯執行**
   - OrderController 將請求委派給 OrderService
   - OrderService 執行業務邏輯：
     - 呼叫 InventoryClient 檢查庫存
     - 呼叫 PaymentClient 處理付款

3. **資料持久化**
   - OrderService 通過 OrderRepository 存取資料庫
   - OrderRepository 執行 CRUD 操作到 orders 資料表

4. **回應返回**
   - 資料流向反向傳遞回 API Client

---

## 典型訂單處理流程

### 建立訂單的完整資料流

```
1. [API Client] 
   ↓ POST /api/orders (OrderRequest)
   
2. [OrderController.createOrder()]
   ↓ Validate request
   ↓ Delegate to service
   
3. [OrderService.processOrder()]
   ↓ Check inventory
   → [InventoryClient.checkStock()] → [inventory-service]
   ↓ Process payment
   → [PaymentClient.processPayment()] → [payment-service]
   ↓ Create order entity
   
4. [OrderRepository.save()]
   ↓ Persist data
   
5. [orders 資料表]
   ← INSERT operation
   
6. [Response flows back]
   ↑ OrderResponse
   
7. [API Client]
   ← Receives OrderResponse
```

---

## 架構特點分析

### 優點

1. **清晰的分層架構**
   - Controller 層：處理 HTTP 請求/回應
   - Service 層：業務邏輯和外部整合
   - Repository 層：資料存取

2. **服務解耦**
   - 使用 Feign Client 與外部服務通訊
   - 各服務職責明確

3. **資料隔離**
   - 單一資料儲存 (orders 表)
   - 遵循微服務資料獨立原則

### 可改進之處

1. **錯誤處理**
   - 可加入斷路器 (Circuit Breaker) 處理外部服務失敗
   - 實作重試機制

2. **非同步處理**
   - 考慮使用訊息佇列處理訂單
   - 提升系統可靠性和擴展性

3. **快取機制**
   - 庫存查詢可加入快取
   - 減少對外部服務的呼叫

---

## 如何使用這個 Skill

### 步驟 1: 安裝

上傳 `dfd-analyzer.skill` 到 Claude

### 步驟 2: 分析你的專案

```
請分析我的 Spring Boot 專案並產生 DFD
```

### 步驟 3: 選擇層級

- **Level 0**: 用於高階簡報
- **Level 1**: 用於開發文件
- **Level 2**: 用於深入技術分析

### 步驟 4: 整合到文件

將生成的 Mermaid 圖表加入：
- README.md
- Architecture Decision Records (ADR)
- API 文件
- 技術設計文件

---

## 實用命令

### 本地使用

```bash
# 分析專案
python scripts/analyze_project.py /path/to/project > analysis.json

# 產生詳細 DFD
python scripts/generate_mermaid.py analysis.json > dfd.mmd

# 產生 Context Diagram
python scripts/generate_mermaid.py analysis.json --context > context.mmd
```

### 在 Claude 中使用

```
# 完整分析
請分析這個專案並產生完整的 DFD 文件

# 只要 Context Diagram
請產生 Level 0 Context Diagram

# 聚焦特定模組
請分析 OrderService 的資料流程
```

---

## 總結

這個 DFD Analyzer Skill 能夠：

✅ 自動掃描 Spring Boot 專案結構  
✅ 識別各種元件類型 (Controllers, Services, Repositories)  
✅ 偵測外部依賴 (Feign Clients, 資料庫)  
✅ 追蹤資料流向  
✅ 產生多層級的 DFD 圖表  
✅ 輸出 Mermaid 格式，可直接在文件中使用  

**立即開始使用，讓架構文件自動化！** 🚀
