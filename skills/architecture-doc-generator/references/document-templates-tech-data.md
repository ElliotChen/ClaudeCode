# 架構文件範本（技術與資料）

## 05-技術堆疊.md 範本

```markdown
---
title: 技術堆疊與整合
date: YYYY-MM-DD
---

# 技術堆疊

## 核心技術

### 應用框架
- **Spring Boot**：3.2.x
- **Java**：21（啟用 Virtual Threads）
- **建置工具**：Maven 3.9.x
- **相依性管理**：Spring Boot BOM

### 資料庫
- **主要**：PostgreSQL 16
- **連線池**：HikariCP（Spring Boot 設定）
- **遷移**：Flyway
- **ORM**：Spring Data JPA / Hibernate 6.4

### 快取
- **技術**：Redis 7.2
- **客戶端**：Lettuce（Spring Data Redis）
- **使用案例**：Session 儲存、查詢快取、限流
- **淘汰策略**：LRU with TTL

### 訊息系統
- **代理**：Apache Kafka 3.6
- **客戶端**：Spring Kafka
- **序列化**：JSON（簡單）、Avro（大量）
- **消費者群組**：每個微服務實例池一個

### API 文件
- **規格**：OpenAPI 3.0
- **產生**：Springdoc OpenAPI
- **UI**：Swagger UI at /swagger-ui.html
- **匯出**：JSON/YAML 可在 /v3/api-docs 取得

## Spring Boot 設定

### 關鍵相依性

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- 訊息 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- 快取 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- 監控 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

### Application Properties

```yaml
spring:
  application:
    name: order-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/order_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 第三方整合

### [外部服務名稱]

**目的**：提供的功能

**整合方法**：REST API / SDK / 訊息佇列

**設定**：
```yaml
external:
  service:
    base-url: https://api.example.com
    api-key: ${EXTERNAL_API_KEY}
    timeout: 5000
    retry:
      max-attempts: 3
      backoff: 1000
```

**斷路器**（如使用 Resilience4j）：
```yaml
resilience4j:
  circuitbreaker:
    instances:
      externalService:
        sliding-window-size: 100
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
```

## DevOps 工具

### 容器化
- **Docker**：24.0+
- **基礎映像**：eclipse-temurin:21-jre-alpine
- **多階段建置**：是

### 編排
- **Kubernetes**：1.28+
- **服務網格**：Istio 1.20（如適用）
- **Ingress Controller**：NGINX Ingress

### CI/CD
- **Pipeline**：GitHub Actions / GitLab CI / Jenkins
- **製品儲存庫**：Nexus / Artifactory
- **映像儲存庫**：Docker Hub / ECR / GCR

### 監控堆疊
- **指標**：Prometheus + Grafana
- **日誌**：ELK Stack
- **追蹤**：Jaeger / Zipkin
```

## 06-資料架構.md 範本

```markdown
---
title: 資料架構
date: YYYY-MM-DD
---

# 資料架構

## 資料模型

### [Bounded Context] 資料庫綱要

**資料庫**：order_db（PostgreSQL 16）

**資料表**：

#### orders
Order aggregate 的主要 aggregate root 資料表。

| 欄位 | 類型 | 限制 | 說明 |
|------|------|------|------|
| order_id | UUID | PRIMARY KEY | Aggregate root ID |
| customer_id | UUID | NOT NULL | 客戶參照 |
| order_status | VARCHAR(20) | NOT NULL | PENDING、CONFIRMED、SHIPPED 等 |
| total_amount | DECIMAL(10,2) | NOT NULL | 訂單總額 |
| currency | VARCHAR(3) | NOT NULL | ISO 4217 代碼 |
| placed_at | TIMESTAMP | NOT NULL | 訂單建立時間 |
| version | BIGINT | NOT NULL | 樂觀鎖 |
| created_at | TIMESTAMP | NOT NULL | 稽核欄位 |
| updated_at | TIMESTAMP | NOT NULL | 稽核欄位 |

**索引**：
- `idx_customer_id` ON customer_id - 頻繁查詢
- `idx_placed_at` ON placed_at - 日期範圍查詢
- `idx_status` ON order_status - 狀態篩選

#### order_items
Order aggregate 內的子實體。

| 欄位 | 類型 | 限制 | 說明 |
|------|------|------|------|
| item_id | UUID | PRIMARY KEY | 明細 ID |
| order_id | UUID | FOREIGN KEY → orders | 父訂單 |
| product_id | UUID | NOT NULL | 產品參照 |
| quantity | INT | NOT NULL CHECK (quantity > 0) | 訂購數量 |
| unit_price | DECIMAL(10,2) | NOT NULL | 單價 |
| subtotal | DECIMAL(10,2) | NOT NULL | quantity * unit_price |

**限制**：
- UNIQUE(order_id, product_id) - 每個訂單的產品只有一個明細

### JPA Entity 對應

**Order Aggregate Root**：
```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_placed_at", columnList = "placed_at")
})
public class Order {
    @Id
    @Column(name = "order_id")
    private UUID id;
    
    @Embedded
    private CustomerId customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus status;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money totalAmount;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // 領域方法，非 getters/setters
    public void addLineItem(ProductId productId, int quantity, Money unitPrice) {
        // 業務邏輯
    }
}
```

**Value Object 對應**：
```java
@Embeddable
public class Money {
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    
    // 不可變，值語義
}
```

## 資料存取模式

### Repository 實作

**領域層介面**：
```java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    void delete(Order order);
}
```

**基礎設施層實作**：
```java
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    
    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value());
    }
}

// Spring Data JPA Interface（基礎設施）
interface OrderJpaRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerId(UUID customerId);
}
```

### 查詢模式

**簡單查詢**：Spring Data JPA 查詢方法
**複雜查詢**：JPQL 或 Criteria API
**報表**：原生 SQL 或讀取模型

```java
// 複雜篩選的自訂查詢
@Query("SELECT o FROM Order o WHERE o.status IN :statuses " +
       "AND o.placedAt BETWEEN :startDate AND :endDate")
List<Order> findOrdersByStatusAndDateRange(
    @Param("statuses") List<OrderStatus> statuses,
    @Param("startDate") Instant startDate,
    @Param("endDate") Instant endDate
);
```

## 資料一致性

### Aggregate 內
- **交易邊界**：單一 aggregate 修改
- **鎖定**：使用 `@Version` 的樂觀鎖
- **不變量保護**：在領域方法中強制執行

### 跨 Aggregate
- **最終一致性**：Domain event + 事件處理器
- **Saga 模式**：分散式工作流程的補償交易
- **無外鍵**：aggregate 之間（僅透過 ID 參照）

### 交易管理

```java
@Service
public class OrderApplicationService {
    
    @Transactional  // 僅在應用服務層
    public OrderId placeOrder(PlaceOrderCommand command) {
        // 載入 aggregate
        Order order = Order.create(/*...*/);
        
        // 修改 aggregate（檢查所有不變量）
        order.addLineItem(/*...*/);
        order.placeOrder();
        
        // 儲存（發布 domain event）
        orderRepository.save(order);
        
        // 交易提交後處理事件
        return order.getId();
    }
}
```

## 快取策略

### 快取層級

**L1 - 應用快取**（Caffeine）：
- 範圍：記憶體內，每個實例
- 使用：頻繁存取、很少變更的資料
- TTL：5 分鐘
- 淘汰：基於大小（10,000 項目）

**L2 - 分散式快取**（Redis）：
- 範圍：跨實例共享
- 使用：Session 資料、限流、頻繁查詢的 aggregate
- TTL：查詢結果 1 小時
- 淘汰：LRU

### 快取實作

```java
@Service
public class OrderQueryService {
    
    @Cacheable(value = "orders", key = "#orderId", unless = "#result == null")
    public Optional<OrderDTO> findOrder(UUID orderId) {
        return orderRepository.findById(new OrderId(orderId))
            .map(OrderDTO::from);
    }
    
    @CacheEvict(value = "orders", key = "#event.orderId")
    @EventListener
    public void handleOrderUpdated(OrderUpdatedEvent event) {
        // 快取自動失效
    }
}
```

### 快取失效

- **Write-Through**：一起更新資料庫和快取
- **事件驅動**：在 domain event 上失效
- **TTL**：過時資料容忍度的自動過期

## 資料遷移

### 綱要演進

**工具**：Flyway

**遷移檔案**：`src/main/resources/db/migration/`

```sql
-- V001__create_orders_table.sql
CREATE TABLE orders (
    order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    order_status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    placed_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_id ON orders(customer_id);
CREATE INDEX idx_placed_at ON orders(placed_at);
```

**部署策略**：
- 先進行向後相容的變更
- 部署應用程式
- 在後續版本中移除棄用的欄位

## 資料備份與復原

### 備份策略
- **頻率**：每日完整備份，每小時增量
- **保留**：30 天
- **儲存**：S3 / 雲端儲存（加密）
- **測試**：每月復原演練

### 災難復原
- **RTO**：4 小時
- **RPO**：1 小時
- **程序**：復原程序的文件化執行手冊
```
