# Mermaid 圖表範例

本檔案包含架構文件可重複使用的 Mermaid 圖表範本。

## 情境圖 (Context Diagram)

顯示系統邊界與外部參與者/系統。

```mermaid
graph TB
    User[終端使用者]
    Admin[管理員]
    System[您的系統名稱]
    ExtAPI[外部付款 API]
    Analytics[分析平台]
    Email[電子郵件服務]
    
    User -->|HTTPS| System
    Admin -->|HTTPS| System
    System -->|REST API| ExtAPI
    System -->|事件| Analytics
    System -->|SMTP| Email
    
    style System fill:#4A90E2,color:#fff
    style User fill:#E8F4F8
    style Admin fill:#E8F4F8
```

## 容器圖 (Container Diagram) - 微服務

顯示主要應用容器與資料儲存。

```mermaid
graph TB
    subgraph "API Gateway 層"
        Gateway[API Gateway<br/>Spring Cloud Gateway]
    end
    
    subgraph "應用服務"
        OrderSvc[訂單服務<br/>Spring Boot 3.2]
        InventorySvc[庫存服務<br/>Spring Boot 3.2]
        PaymentSvc[付款服務<br/>Spring Boot 3.2]
    end
    
    subgraph "資料層"
        OrderDB[(訂單 DB<br/>PostgreSQL)]
        InventoryDB[(庫存 DB<br/>PostgreSQL)]
        Cache[(Redis 快取)]
    end
    
    subgraph "訊息系統"
        Kafka[Kafka 叢集]
    end
    
    Gateway -->|路由| OrderSvc
    Gateway -->|路由| InventorySvc
    Gateway -->|路由| PaymentSvc
    
    OrderSvc --> OrderDB
    OrderSvc --> Cache
    OrderSvc -->|發布事件| Kafka
    
    InventorySvc --> InventoryDB
    InventorySvc --> Cache
    InventorySvc -->|訂閱事件| Kafka
    
    PaymentSvc -->|訂閱事件| Kafka
    
    style Gateway fill:#4A90E2,color:#fff
    style OrderSvc fill:#50C878,color:#fff
    style InventorySvc fill:#50C878,color:#fff
    style PaymentSvc fill:#50C878,color:#fff
    style Kafka fill:#FFB84D,color:#333
```

## Bounded Context Map

顯示 DDD Bounded Context 及其關係。

```mermaid
graph LR
    Order[訂單 Context]
    Inventory[庫存 Context]
    Shipping[出貨 Context]
    Payment[付款 Context]
    Customer[客戶 Context]
    
    Order -->|Customer-Supplier<br/>REST API| Inventory
    Order -->|Customer-Supplier<br/>REST API| Payment
    Order -->|Published Language<br/>Domain Event| Shipping
    Order -->|Conformist<br/>讀取 API| Customer
    
    Inventory -->|Shared Kernel<br/>共同模型| Shipping
    
    classDef coreContext fill:#4A90E2,color:#fff,stroke:#333,stroke-width:3px
    classDef supportingContext fill:#50C878,color:#fff,stroke:#333,stroke-width:2px
    classDef genericContext fill:#FFB84D,color:#333,stroke:#333,stroke-width:2px
    
    class Order coreContext
    class Inventory,Shipping,Payment supportingContext
    class Customer genericContext
```

## 元件圖 (Component Diagram) - 服務內部

顯示使用 DDD 層級的服務內部結構。

```mermaid
graph TB
    subgraph "訂單服務"
        subgraph "表現層"
            Controller[REST Controllers<br/>@RestController]
        end
        
        subgraph "應用層"
            AppSvc[Application Service<br/>@Service]
            Command[Command 與 DTO]
        end
        
        subgraph "領域層"
            Aggregate[Aggregate<br/>Order、OrderItem]
            ValueObj[Value Object<br/>Money、Address]
            DomainSvc[Domain Service<br/>@Component]
            RepoInterface[Repository 介面]
            Events[Domain Event]
        end
        
        subgraph "基礎設施層"
            RepoImpl[JPA Repository<br/>@Repository]
            EventPub[Event Publisher<br/>Kafka]
            External[外部 Adapter]
        end
    end
    
    Controller --> AppSvc
    AppSvc --> Command
    AppSvc --> Aggregate
    AppSvc --> DomainSvc
    AppSvc --> RepoInterface
    
    Aggregate --> ValueObj
    Aggregate --> Events
    DomainSvc --> Aggregate
    
    RepoInterface -.實作.- RepoImpl
    RepoImpl --> Aggregate
    
    Events --> EventPub
    AppSvc --> External
    
    style Controller fill:#E8F4F8
    style AppSvc fill:#B8E6B8
    style Aggregate fill:#FFE6B8
    style RepoImpl fill:#F5F5F5
```

## 循序圖 (Sequence Diagram) - 業務流程

顯示使用案例的元件間互動。

```mermaid
sequenceDiagram
    participant Client as 客戶端
    participant API as API Gateway
    participant OrderSvc as 訂單服務
    participant InventorySvc as 庫存服務
    participant PaymentSvc as 付款服務
    participant Kafka
    participant DB as 訂單 DB
    
    Client->>API: POST /orders
    API->>OrderSvc: placeOrder(command)
    
    activate OrderSvc
    OrderSvc->>InventorySvc: checkStock(productId, qty)
    InventorySvc-->>OrderSvc: StockAvailable
    
    OrderSvc->>PaymentSvc: authorizePayment(amount)
    PaymentSvc-->>OrderSvc: PaymentAuthorized
    
    OrderSvc->>OrderSvc: createOrder()
    OrderSvc->>DB: save(order)
    DB-->>OrderSvc: 訂單已儲存
    
    OrderSvc->>Kafka: publish(OrderPlacedEvent)
    deactivate OrderSvc
    
    OrderSvc-->>API: OrderCreated
    API-->>Client: 201 Created
    
    Kafka->>InventorySvc: OrderPlacedEvent
    activate InventorySvc
    InventorySvc->>InventorySvc: reserveStock()
    deactivate InventorySvc
    
    Kafka->>PaymentSvc: OrderPlacedEvent
    activate PaymentSvc
    PaymentSvc->>PaymentSvc: capturePayment()
    deactivate PaymentSvc
```

## 部署圖 (Deployment Diagram)

顯示基礎設施與部署拓撲。

```mermaid
graph TB
    subgraph "負載平衡器"
        LB[AWS ALB / NGINX]
    end
    
    subgraph "Kubernetes 叢集"
        subgraph "order-service namespace"
            OrderPod1[訂單 Pod 1]
            OrderPod2[訂單 Pod 2]
            OrderPod3[訂單 Pod 3]
        end
        
        subgraph "inventory-service namespace"
            InvPod1[庫存 Pod 1]
            InvPod2[庫存 Pod 2]
        end
        
        subgraph "資料服務"
            PG[(PostgreSQL<br/>RDS/Cloud SQL)]
            Redis[(Redis<br/>ElastiCache)]
        end
        
        subgraph "訊息系統"
            Kafka[Kafka 叢集<br/>MSK/Confluent]
        end
    end
    
    subgraph "監控"
        Prom[Prometheus]
        Grafana[Grafana]
        Jaeger[Jaeger 追蹤]
    end
    
    LB --> OrderPod1
    LB --> OrderPod2
    LB --> OrderPod3
    
    OrderPod1 --> PG
    OrderPod2 --> PG
    OrderPod3 --> PG
    
    OrderPod1 --> Redis
    OrderPod1 --> Kafka
    
    InvPod1 --> PG
    InvPod1 --> Kafka
    
    OrderPod1 -.指標.- Prom
    OrderPod1 -.追蹤.- Jaeger
    Prom --> Grafana
    
    style LB fill:#4A90E2,color:#fff
    style Kafka fill:#FFB84D,color:#333
```

## 實體關係圖 (Entity Relationship Diagram)

顯示資料庫綱要關係。

```mermaid
erDiagram
    ORDERS ||--o{ ORDER_ITEMS : 包含
    ORDERS ||--|| SHIPPING_ADDRESSES : 有
    ORDERS {
        uuid order_id PK
        uuid customer_id
        varchar order_status
        decimal total_amount
        varchar currency
        timestamp placed_at
        bigint version
        timestamp created_at
        timestamp updated_at
    }
    
    ORDER_ITEMS {
        uuid item_id PK
        uuid order_id FK
        uuid product_id
        int quantity
        decimal unit_price
        decimal subtotal
    }
    
    SHIPPING_ADDRESSES {
        uuid order_id PK_FK
        varchar street_line1
        varchar street_line2
        varchar city
        varchar state
        varchar postal_code
        varchar country_code
    }
```

## 狀態圖 (State Diagram) - 訂單狀態

顯示 aggregate 的狀態轉換。

```mermaid
stateDiagram-v2
    [*] --> 待處理: 建立訂單
    
    待處理 --> 已確認: 付款已授權
    待處理 --> 已取消: 取消訂單
    待處理 --> 失敗: 付款失敗
    
    已確認 --> 處理中: 開始履行
    已確認 --> 已取消: 取消訂單
    
    處理中 --> 已出貨: 出貨訂單
    處理中 --> 已取消: 取消訂單
    
    已出貨 --> 已送達: 送達確認
    已出貨 --> 已退貨: 發起退貨
    
    已送達 --> 已退貨: 發起退貨
    已送達 --> [*]: 歸檔
    
    已退貨 --> 已退款: 退款處理
    已退款 --> [*]: 歸檔
    
    已取消 --> [*]: 歸檔
    失敗 --> [*]: 歸檔
```

## 資料流程圖 (Data Flow Diagram) - 事件處理

顯示資料如何透過事件流經系統。

```mermaid
graph LR
    subgraph "訂單服務"
        OrderAPI[訂單 API]
        OrderDB[(訂單 DB)]
    end
    
    subgraph "事件匯流排"
        Kafka[Kafka Topic]
    end
    
    subgraph "庫存服務"
        InvListener[事件監聽器]
        InvDB[(庫存 DB)]
    end
    
    subgraph "出貨服務"
        ShipListener[事件監聽器]
        ShipDB[(出貨 DB)]
    end
    
    subgraph "通知服務"
        NotifListener[事件監聽器]
        Email[電子郵件閘道]
    end
    
    OrderAPI -->|1. 建立訂單| OrderDB
    OrderDB -->|2. OrderPlacedEvent| Kafka
    
    Kafka -->|3. 訂閱| InvListener
    InvListener -->|4. 保留庫存| InvDB
    InvDB -->|5. StockReservedEvent| Kafka
    
    Kafka -->|6. 訂閱| ShipListener
    ShipListener -->|7. 建立出貨| ShipDB
    
    Kafka -->|8. 訂閱| NotifListener
    NotifListener -->|9. 傳送確認| Email
    
    style Kafka fill:#FFB84D,color:#333
```

## Saga 模式圖 (分散式交易)

顯示補償交易流程。

```mermaid
sequenceDiagram
    participant Orchestrator as 編排器
    participant OrderSvc as 訂單服務
    participant PaymentSvc as 付款服務
    participant InventorySvc as 庫存服務
    participant ShippingSvc as 出貨服務
    
    Note over Orchestrator: 開始訂單 Saga
    
    Orchestrator->>OrderSvc: 1. 建立訂單
    OrderSvc-->>Orchestrator: 訂單已建立
    
    Orchestrator->>PaymentSvc: 2. 授權付款
    PaymentSvc-->>Orchestrator: 付款已授權
    
    Orchestrator->>InventorySvc: 3. 保留庫存
    InventorySvc-->>Orchestrator: 庫存已保留
    
    Orchestrator->>ShippingSvc: 4. 建立出貨
    ShippingSvc--xOrchestrator: 出貨失敗 ❌
    
    Note over Orchestrator: 補償流程
    
    Orchestrator->>InventorySvc: 5. 釋放庫存（補償）
    InventorySvc-->>Orchestrator: 庫存已釋放
    
    Orchestrator->>PaymentSvc: 6. 退款（補償）
    PaymentSvc-->>Orchestrator: 付款已退款
    
    Orchestrator->>OrderSvc: 7. 取消訂單（補償）
    OrderSvc-->>Orchestrator: 訂單已取消
    
    Note over Orchestrator: Saga 失敗 ❌
```

## 架構決策圖

視覺化關鍵決策及其關係。

```mermaid
graph TB
    Start[選擇架構]
    
    Start --> A{系統複雜度?}
    A -->|低| Monolith[模組化單體]
    A -->|高| Microservices[微服務]
    
    Microservices --> B{通訊方式?}
    B -->|同步| REST[REST API]
    B -->|非同步| Events[事件驅動]
    B -->|兩者| Hybrid[混合方法]
    
    Hybrid --> C{一致性?}
    C -->|強一致| Saga[Saga 模式]
    C -->|最終一致| AsyncEvents[僅非同步事件]
    
    Saga --> D{編排?}
    D -->|集中式| Orchestrator[編排器服務]
    D -->|分散式| Choreography[編舞]
    
    style Start fill:#4A90E2,color:#fff
    style Microservices fill:#50C878,color:#fff
    style Hybrid fill:#FFB84D,color:#333
    style Orchestrator fill:#E8F4F8
```

## 類別圖 (Class Diagram) - 領域模型

顯示 DDD 領域模型結構。

```mermaid
classDiagram
    class Order {
        <<Aggregate Root>>
        -OrderId id
        -CustomerId customerId
        -OrderStatus status
        -Money totalAmount
        -List~OrderItem~ items
        -ShippingAddress address
        -Long version
        +addLineItem(productId, quantity, price)
        +placeOrder()
        +cancel()
        +calculateTotal() Money
    }
    
    class OrderItem {
        <<Entity>>
        -OrderItemId id
        -ProductId productId
        -int quantity
        -Money unitPrice
        -Money subtotal
        +calculateSubtotal() Money
    }
    
    class Money {
        <<Value Object>>
        -BigDecimal amount
        -String currency
        +add(Money) Money
        +multiply(int) Money
        +equals(Object) boolean
    }
    
    class OrderId {
        <<Value Object>>
        -UUID value
        +equals(Object) boolean
    }
    
    class ShippingAddress {
        <<Value Object>>
        -String streetLine1
        -String city
        -String postalCode
        -String countryCode
    }
    
    class OrderStatus {
        <<Enumeration>>
        PENDING
        CONFIRMED
        SHIPPED
        DELIVERED
        CANCELLED
    }
    
    Order "1" *-- "many" OrderItem : 包含
    Order *-- "1" OrderId : 有
    Order *-- "1" Money : totalAmount
    Order *-- "1" ShippingAddress : 有
    Order --> OrderStatus : 使用
    OrderItem *-- "1" Money : unitPrice
```
