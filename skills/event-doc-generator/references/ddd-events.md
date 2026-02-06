# Domain-Driven Design 中的 Event 模式

## 概述

在 Domain-Driven Design (DDD) 中,Domain Event 是領域中發生的重要事情的記錄。Event 是一種強大的模式,用於：

1. **解耦** - 降低 Bounded Context 之間的耦合
2. **追蹤** - 記錄系統中發生的重要變化
3. **整合** - 在不同的 Bounded Context 之間傳遞資訊
4. **事件溯源** - 重建系統狀態

## Event 的特性

### 1. 不可變性 (Immutability)

Event 一旦產生就不應該被修改。使用 Java Record 或 final 欄位確保不可變性。

```java
// 使用 Record (推薦)
public record OrderPlacedEvent(
    UUID orderId,
    UUID customerId,
    Instant placedAt,
    BigDecimal totalAmount
) implements DomainEvent {}

// 使用傳統 Class
public final class OrderPlacedEvent extends ApplicationEvent {
    private final UUID orderId;
    private final UUID customerId;
    private final Instant placedAt;
    private final BigDecimal totalAmount;
    
    // Constructor, getters only
}
```

### 2. 過去式命名

Event 代表已經發生的事實,應該使用過去式命名：

✅ **正確**:
- `UserCreatedEvent`
- `OrderPlacedEvent`
- `PaymentProcessedEvent`
- `InventoryReservedEvent`

❌ **錯誤**:
- `CreateUserEvent`
- `PlaceOrderEvent`
- `ProcessPaymentEvent`

### 3. 包含足夠的資訊

Event 應該包含 Listener 處理所需的所有資訊,避免 Listener 需要回查資料庫。

```java
// ❌ 資訊不足
public record UserCreatedEvent(UUID userId) {}

// ✅ 資訊充足
public record UserCreatedEvent(
    UUID userId,
    String email,
    String username,
    Instant createdAt,
    RegistrationSource source
) {}
```

### 4. 包含時間戳記

每個 Event 都應該記錄發生的時間：

```java
public record DomainEventBase(
    UUID eventId,
    Instant occurredAt
) {}

public record OrderPlacedEvent(
    UUID orderId,
    Instant occurredAt  // 繼承或包含時間
) {}
```

## Event 的分類

### 1. Domain Event (領域事件)

發生在單一 Bounded Context 內的事件,表達領域中的重要業務變化。

```java
@DomainEvent
public record ProductPriceChangedEvent(
    UUID productId,
    BigDecimal oldPrice,
    BigDecimal newPrice,
    String reason,
    Instant changedAt
) {}
```

### 2. Integration Event (整合事件)

跨越 Bounded Context 的事件,用於不同領域之間的整合。

```java
@IntegrationEvent
public record OrderPlacedIntegrationEvent(
    UUID orderId,
    UUID customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    Instant placedAt
) implements Serializable {}  // 可能需要序列化傳輸
```

### 3. System Event (系統事件)

技術層面的事件,如快取失效、連線中斷等。

## Event 處理模式

### 1. 同步處理

適用於必須立即完成的操作：

```java
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    // 同步處理,會阻塞主流程
    inventoryService.reserveStock(event.getItems());
}
```

### 2. 異步處理

適用於耗時操作或非關鍵路徑：

```java
@Async
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    // 異步處理,不阻塞主流程
    emailService.sendOrderConfirmation(event);
}
```

### 3. 事務性處理

確保只在事務成功後才處理事件：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderPlaced(OrderPlacedEvent event) {
    // 只在事務提交後執行
    loyaltyPointsService.awardPoints(event);
}
```

### 4. 條件式處理

只在特定條件下處理事件：

```java
@EventListener(condition = "#event.totalAmount > 1000")
public void handleLargeOrder(OrderPlacedEvent event) {
    // 只處理金額超過 1000 的訂單
    fraudDetectionService.reviewOrder(event);
}
```

## 最佳實踐

### 1. Event Storming

使用 Event Storming 工作坊識別領域中的重要事件：

1. 收集領域專家和技術人員
2. 使用便利貼列出所有可能的事件（橘色）
3. 識別命令（藍色）和聚合（黃色）
4. 找出 Bounded Context 的邊界

### 2. Event 版本控制

Event 結構可能隨時間演進,需要版本控制：

```java
// v1
public record OrderPlacedEventV1(
    UUID orderId,
    BigDecimal totalAmount
) {}

// v2 - 新增欄位
public record OrderPlacedEventV2(
    UUID orderId,
    BigDecimal totalAmount,
    PaymentMethod paymentMethod  // 新增
) {}

// 版本轉換
@EventListener
public void handleV1(OrderPlacedEventV1 event) {
    // 轉換為 v2
    var v2 = new OrderPlacedEventV2(
        event.orderId(),
        event.totalAmount(),
        PaymentMethod.UNKNOWN
    );
    handleV2(v2);
}
```

### 3. Event 的顆粒度

選擇適當的 Event 顆粒度：

```java
// ❌ 太細 - 產生過多事件
UserFirstNameChangedEvent
UserLastNameChangedEvent
UserEmailChangedEvent

// ✅ 適中 - 合併相關變更
UserProfileUpdatedEvent(
    userId,
    changedFields: Map<String, Object>
)

// ❌ 太粗 - 失去語意
UserChangedEvent
```

### 4. 錯誤處理

```java
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    try {
        processOrder(event);
    } catch (Exception e) {
        // 記錄錯誤
        log.error("Failed to process order event: {}", event, e);
        
        // 發送補償事件或死信佇列
        deadLetterQueue.send(event, e);
        
        // 不要重新拋出異常,避免影響其他 Listener
    }
}
```

### 5. 冪等性

確保 Listener 可以安全地重複執行：

```java
@EventListener
public void handleUserCreated(UserCreatedEvent event) {
    // 檢查是否已處理過
    if (processedEventRepository.exists(event.eventId())) {
        log.info("Event already processed: {}", event.eventId());
        return;
    }
    
    // 處理事件
    sendWelcomeEmail(event);
    
    // 記錄已處理
    processedEventRepository.save(event.eventId());
}
```

## 進階模式

### 1. Event Sourcing

使用事件序列重建聚合狀態：

```java
public class Order {
    private final List<DomainEvent> changes = new ArrayList<>();
    
    public void place(OrderPlacedEvent event) {
        // 應用事件
        this.status = OrderStatus.PLACED;
        this.placedAt = event.occurredAt();
        
        // 記錄變更
        changes.add(event);
    }
    
    public List<DomainEvent> getUncommittedChanges() {
        return new ArrayList<>(changes);
    }
}
```

### 2. CQRS (Command Query Responsibility Segregation)

使用事件同步讀寫模型：

```java
// Command Side - 發送事件
@Service
public class OrderCommandService {
    public void placeOrder(PlaceOrderCommand cmd) {
        var order = new Order(cmd);
        orderRepository.save(order);
        
        eventPublisher.publish(new OrderPlacedEvent(...));
    }
}

// Query Side - 監聽事件更新讀模型
@EventListener
public void updateReadModel(OrderPlacedEvent event) {
    var readModel = new OrderReadModel(event);
    orderReadModelRepository.save(readModel);
}
```

### 3. Saga Pattern

使用事件協調跨服務的長時間交易：

```java
@Component
public class OrderSaga {
    
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        // 步驟 1: 預留庫存
        eventPublisher.publish(new ReserveInventoryCommand(event));
    }
    
    @EventListener
    public void onInventoryReserved(InventoryReservedEvent event) {
        // 步驟 2: 處理付款
        eventPublisher.publish(new ProcessPaymentCommand(event));
    }
    
    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        // 補償: 釋放庫存
        eventPublisher.publish(new ReleaseInventoryCommand(event));
    }
}
```

## 常見陷阱

### 1. 過度使用同步 Listener

❌ **問題**: 太多同步 Listener 拖慢主流程

```java
// 這些都是同步的,會阻塞主流程
@EventListener void sendEmail(OrderPlacedEvent e) {}
@EventListener void updateAnalytics(OrderPlacedEvent e) {}
@EventListener void notifyWarehouse(OrderPlacedEvent e) {}
@EventListener void updateRecommendations(OrderPlacedEvent e) {}
```

✅ **解決**: 只有關鍵操作使用同步,其他改為異步

```java
@TransactionalEventListener(phase = AFTER_COMMIT)
void reserveInventory(OrderPlacedEvent e) {}  // 關鍵操作

@Async @EventListener
void sendEmail(OrderPlacedEvent e) {}  // 非關鍵,異步

@Async @EventListener
void updateAnalytics(OrderPlacedEvent e) {}  // 非關鍵,異步
```

### 2. Event 包含可變物件

❌ **問題**: Event 中的物件可以被修改

```java
public record OrderPlacedEvent(
    List<OrderItem> items  // List 是可變的!
) {}
```

✅ **解決**: 使用不可變集合

```java
public record OrderPlacedEvent(
    List<OrderItem> items
) {
    public OrderPlacedEvent {
        items = List.copyOf(items);  // 建立不可變副本
    }
}
```

### 3. 忘記處理事件失敗

❌ **問題**: Listener 拋出異常影響其他 Listener

✅ **解決**: 妥善處理異常

```java
@EventListener
public void handle(OrderPlacedEvent event) {
    try {
        processOrder(event);
    } catch (Exception e) {
        log.error("Failed to process: {}", event, e);
        // 不重新拋出異常
    }
}
```

## 參考資源

- Martin Fowler: Domain Event
- Vaughn Vernon: Implementing Domain-Driven Design
- Spring Framework Events Documentation
- Event Sourcing Pattern
- CQRS Pattern
