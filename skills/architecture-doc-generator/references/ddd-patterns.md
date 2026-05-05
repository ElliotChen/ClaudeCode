# Spring Boot 中的 DDD 模式

常見的領域驅動設計模式及其 Spring Boot 實作。

## Aggregate 模式

### 定義
Aggregate 是可以作為單一單元處理的領域物件叢集。Aggregate Root 是進入 aggregate 的入口點。

### 實作

```java
@Entity
@Table(name = "orders")
public class Order {  // Aggregate Root
    
    @Id
    @Column(name = "order_id")
    private UUID id;
    
    @Embedded
    private CustomerId customerId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money totalAmount;
    
    // Aggregate 邊界 - 子實體
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @Embedded
    private ShippingAddress shippingAddress;
    
    @Version  // 樂觀鎖
    private Long version;
    
    @Transient  // 不持久化，在交易內收集
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    // 工廠方法
    public static Order create(CustomerId customerId, ShippingAddress address) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.shippingAddress = address;
        order.status = OrderStatus.PENDING;
        order.totalAmount = Money.ZERO;
        order.items = new ArrayList<>();
        return order;
    }
    
    // 強制執行不變量的業務方法
    public void addLineItem(ProductId productId, int quantity, Money unitPrice) {
        if (status != OrderStatus.PENDING) {
            throw new DomainValidationException("無法將項目加入非待處理訂單");
        }
        
        if (quantity <= 0) {
            throw new DomainValidationException("數量必須為正數");
        }
        
        // 檢查產品是否已在訂單中
        items.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst()
            .ifPresentOrElse(
                item -> item.increaseQuantity(quantity),
                () -> items.add(OrderItem.create(this, productId, quantity, unitPrice))
            );
        
        recalculateTotal();
    }
    
    public void placeOrder() {
        if (items.isEmpty()) {
            throw new DomainValidationException("無法下沒有項目的訂單");
        }
        
        this.status = OrderStatus.PLACED;
        
        // 註冊 domain event
        registerEvent(new OrderPlacedEvent(
            new OrderId(id),
            customerId,
            totalAmount,
            Instant.now()
        ));
    }
    
    private void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    // 僅 getter - 無 setter 以保持封裝
}

@Entity
@Table(name = "order_items")
class OrderItem {  // aggregate 內的實體
    
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Embedded
    private ProductId productId;
    
    private int quantity;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency"))
    })
    private Money unitPrice;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency"))
    })
    private Money subtotal;
    
    static OrderItem create(Order order, ProductId productId, int quantity, Money unitPrice) {
        OrderItem item = new OrderItem();
        item.id = UUID.randomUUID();
        item.order = order;
        item.productId = productId;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        item.calculateSubtotal();
        return item;
    }
    
    void increaseQuantity(int amount) {
        this.quantity += amount;
        calculateSubtotal();
    }
    
    private void calculateSubtotal() {
        this.subtotal = unitPrice.multiply(quantity);
    }
    
    // Package-private getter
}
```

### 要點
- Aggregate Root 是唯一的入口點
- 子實體在 aggregate 外部無法存取
- 交易邊界符合 aggregate 邊界
- Root 上的樂觀鎖防止並行修改
- 透過業務方法強制執行不變量

## Value Object 模式

### 定義
沒有概念性識別的物件，僅由其屬性定義。

### 實作

```java
@Embeddable
public class Money {
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    
    public static final Money ZERO = new Money(BigDecimal.ZERO, "TWD");
    
    // JPA 需要無參數建構子
    protected Money() {}
    
    private Money(BigDecimal amount, String currency) {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("金額和幣別不可為 null");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }
    
    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }
    
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    // 業務操作回傳新實例（不可變性）
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("無法操作不同幣別：%s 與 %s",
                    this.currency, other.currency)
            );
        }
    }
    
    // 值相等性
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    // Getter（無 setter - 不可變）
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
}

@Embeddable
public class ShippingAddress {
    
    @Column(name = "street_line1", nullable = false)
    private String streetLine1;
    
    @Column(name = "street_line2")
    private String streetLine2;
    
    @Column(name = "city", nullable = false)
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;
    
    protected ShippingAddress() {}
    
    public ShippingAddress(String streetLine1, String streetLine2, String city,
                          String state, String postalCode, String countryCode) {
        this.streetLine1 = validateNotBlank(streetLine1, "街道地址 1");
        this.streetLine2 = streetLine2;
        this.city = validateNotBlank(city, "城市");
        this.state = state;
        this.postalCode = validateNotBlank(postalCode, "郵遞區號");
        this.countryCode = validateCountryCode(countryCode);
    }
    
    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不可為空");
        }
        return value;
    }
    
    private String validateCountryCode(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("國家代碼必須為 2 個字元");
        }
        return code.toUpperCase();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingAddress that = (ShippingAddress) o;
        return Objects.equals(streetLine1, that.streetLine1) &&
               Objects.equals(streetLine2, that.streetLine2) &&
               Objects.equals(city, that.city) &&
               Objects.equals(state, that.state) &&
               Objects.equals(postalCode, that.postalCode) &&
               Objects.equals(countryCode, that.countryCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(streetLine1, streetLine2, city, state, postalCode, countryCode);
    }
}
```

## Repository 模式

### 定義
使用類似集合的介面在領域與資料對應層之間進行調解。

### 實作

```java
// 領域層 - 介面
package com.company.project.order.domain.repository;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    void delete(Order order);
    boolean existsById(OrderId id);
}

// 基礎設施層 - Spring Data JPA Interface
package com.company.project.order.infrastructure.persistence;

interface OrderJpaRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerId(UUID customerId);
    
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.placedAt >= :since")
    List<Order> findRecentByStatuses(
        @Param("statuses") List<OrderStatus> statuses,
        @Param("since") Instant since
    );
}

// 基礎設施層 - 實作
package com.company.project.order.infrastructure.persistence;

@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.jpaRepository = jpaRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Order save(Order order) {
        Order savedOrder = jpaRepository.save(order);
        
        // 持久化後發布 domain event
        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();
        
        return savedOrder;
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value());
    }
    
    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.value());
    }
    
    @Override
    public void delete(Order order) {
        jpaRepository.delete(order);
    }
    
    @Override
    public boolean existsById(OrderId id) {
        return jpaRepository.existsById(id.value());
    }
}
```

## Domain Service 模式

### 定義
不自然屬於 Entity 或 Value Object 的無狀態操作。

### 實作

```java
// 領域層
package com.company.project.order.domain.service;

@Component  // 使用 @Component，非 @Service，保持領域層純淨
public class PricingService {
    
    private final DiscountPolicy discountPolicy;
    
    public PricingService(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }
    
    public Money calculateOrderTotal(Order order) {
        Money subtotal = order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
        
        Money discount = discountPolicy.calculateDiscount(order);
        return subtotal.subtract(discount);
    }
    
    public Money applyDiscountCode(Money basePrice, DiscountCode code) {
        if (code.isExpired()) {
            throw new DomainValidationException("折扣碼已過期");
        }
        
        if (code.getDiscountType() == DiscountType.PERCENTAGE) {
            return basePrice.multiply(
                BigDecimal.ONE.subtract(code.getDiscountValue().divide(BigDecimal.valueOf(100)))
            );
        } else {
            Money discountAmount = Money.of(code.getDiscountValue(), basePrice.currency());
            return basePrice.subtract(discountAmount);
        }
    }
}

// 應用層 - 使用 domain service
package com.company.project.order.application.service;

@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final PricingService pricingService;  // Domain service
    
    public OrderId placeOrder(PlaceOrderCommand command) {
        Order order = Order.create(command.getCustomerId(), command.getShippingAddress());
        
        command.getItems().forEach(item ->
            order.addLineItem(item.productId(), item.quantity(), item.unitPrice())
        );
        
        // 使用 domain service 進行複雜計算
        Money finalTotal = pricingService.calculateOrderTotal(order);
        order.setTotalAmount(finalTotal);
        
        order.placeOrder();
        
        orderRepository.save(order);
        
        return order.getId();
    }
}
```

## Domain Event 模式

### 定義
領域中發生的、領域專家關心的事件。

### 實作

```java
// 領域層 - Event 定義
package com.company.project.order.domain.event;

public interface DomainEvent {
    Instant occurredOn();
}

public class OrderPlacedEvent implements DomainEvent {
    
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money totalAmount;
    private final Instant occurredOn;
    
    public OrderPlacedEvent(OrderId orderId, CustomerId customerId, Money totalAmount, Instant occurredOn) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.occurredOn = occurredOn;
    }
    
    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
    
    // Getter
}

// Aggregate 中發布
public class Order {
    
    public void placeOrder() {
        validateCanBePlaced();
        
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
        
        registerEvent(new OrderPlacedEvent(
            new OrderId(this.id),
            this.customerId,
            this.totalAmount,
            this.placedAt
        ));
    }
}

// 基礎設施 - Event Handler（同一 Context，同步）
package com.company.project.order.infrastructure.event;

@Component
public class OrderEventHandler {
    
    private final NotificationService notificationService;
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("訂單已下: {}", event.getOrderId());
        
        // 更新讀取模型、傳送通知等
        notificationService.sendOrderConfirmation(
            event.getCustomerId(),
            event.getOrderId()
        );
    }
}

// 基礎設施 - Kafka Publisher（跨 Context，非同步）
package com.company.project.order.infrastructure.messaging;

@Component
public class KafkaOrderEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @EventListener
    @Async
    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send("order-events", event.getOrderId().value().toString(), event);
    }
}

// 其他 Context - Event Consumer
package com.company.project.inventory.infrastructure.messaging;

@Component
public class InventoryEventListener {
    
    private final InventoryService inventoryService;
    
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // 在 Inventory context 中處理
        inventoryService.reserveStock(event.getOrderId(), event.getItems());
    }
}
```

## Anticorruption Layer (ACL)

### 定義
在您的領域模型與外部系統之間進行轉譯，以防止污染。

### 實作

```java
// 外部系統的模型（我們無法控制）
package com.external.payment;

public class PaymentResponse {
    private String transactionId;
    private String status;  // "SUCCESS", "FAILED", "PENDING"
    private double amount;
    private String curr;  // 不同的命名
    // 扁平結構，沒有 value object
}

// 我們的領域模型
package com.company.project.order.domain.model;

public class PaymentResult {
    private final TransactionId transactionId;
    private final PaymentStatus status;
    private final Money amount;
    
    // 豐富的領域模型
}

// Anticorruption Layer - Adapter
package com.company.project.order.infrastructure.adapter;

@Component
public class PaymentServiceAdapter {
    
    private final PaymentServiceClient externalClient;
    
    public PaymentServiceAdapter(PaymentServiceClient externalClient) {
        this.externalClient = externalClient;
    }
    
    public PaymentResult processPayment(OrderId orderId, Money amount) {
        // 從領域轉譯為外部
        PaymentRequest externalRequest = toExternalRequest(orderId, amount);
        
        // 呼叫外部系統
        PaymentResponse externalResponse = externalClient.charge(externalRequest);
        
        // 從外部轉譯為領域
        return toDomainModel(externalResponse);
    }
    
    private PaymentRequest toExternalRequest(OrderId orderId, Money amount) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderReference(orderId.value().toString());
        request.setAmount(amount.amount().doubleValue());
        request.setCurr(amount.currency());
        return request;
    }
    
    private PaymentResult toDomainModel(PaymentResponse response) {
        return new PaymentResult(
            new TransactionId(response.getTransactionId()),
            mapStatus(response.getStatus()),
            Money.of(response.getAmount(), response.getCurr())
        );
    }
    
    private PaymentStatus mapStatus(String externalStatus) {
        return switch (externalStatus) {
            case "SUCCESS" -> PaymentStatus.COMPLETED;
            case "FAILED" -> PaymentStatus.FAILED;
            case "PENDING" -> PaymentStatus.PENDING;
            default -> throw new IllegalArgumentException("未知狀態: " + externalStatus);
        };
    }
}

// Application Service 使用 adapter（非直接使用外部 client）
@Service
@Transactional
public class OrderApplicationService {
    
    private final PaymentServiceAdapter paymentAdapter;  // ACL，非直接 client
    
    public void processPayment(OrderId orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        // 使用 adapter 而非外部 client
        PaymentResult result = paymentAdapter.processPayment(orderId, order.getTotalAmount());
        
        if (result.getStatus() == PaymentStatus.COMPLETED) {
            order.confirmPayment(result.getTransactionId());
        } else {
            order.failPayment(result.getMessage());
        }
        
        orderRepository.save(order);
    }
}
```

這些模式共同運作，在保持基礎設施關注點分離的同時，創建豐富、可維護的領域模型。
