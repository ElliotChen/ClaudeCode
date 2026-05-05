# 架構文件範本（Spring Boot 實作與測試）

## 07-SpringBoot實作.md 範本

```markdown
---
title: Spring Boot 實作指南
date: YYYY-MM-DD
---

# Spring Boot 實作

## 專案結構

### 套件組織

**Context-First 結構**（DDD 建議）：

```
com.company.project
├── order/                          # Bounded Context
│   ├── domain/                     # 領域層
│   │   ├── model/                  # Aggregate、Entity、Value Object
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   ├── OrderId.java       # Value Object
│   │   │   └── Money.java
│   │   ├── service/                # Domain Service
│   │   │   └── PricingService.java
│   │   ├── repository/             # Repository 介面
│   │   │   └── OrderRepository.java
│   │   └── event/                  # Domain Event
│   │       ├── OrderPlaced.java
│   │       └── OrderCancelled.java
│   ├── application/                # 應用層
│   │   ├── service/                # Application Service
│   │   │   └── OrderApplicationService.java
│   │   ├── command/                # Command
│   │   │   ├── PlaceOrderCommand.java
│   │   │   └── CancelOrderCommand.java
│   │   └── dto/                    # 應用層 DTO
│   ├── infrastructure/             # 基礎設施層
│   │   ├── persistence/            # JPA 實作
│   │   │   ├── JpaOrderRepository.java
│   │   │   └── OrderJpaRepository.java (Spring Data interface)
│   │   ├── messaging/              # Kafka publisher/consumer
│   │   │   ├── OrderEventPublisher.java
│   │   │   └── KafkaOrderEventPublisher.java
│   │   └── config/                 # 基礎設施配置
│   │       └── OrderInfraConfig.java
│   └── interfaces/                 # 表現層
│       ├── rest/                   # REST Controller
│       │   ├── OrderController.java
│       │   └── dto/                # 請求/回應 DTO
│       │       ├── CreateOrderRequest.java
│       │       └── OrderResponse.java
│       └── event/                  # Event Listener（來自其他 context）
│           └── PaymentEventListener.java
└── shared/                         # Shared Kernel（如有）
    └── common/
        ├── Money.java              # 共享 value object
        └── DomainEvent.java        # 共同介面
```

## Spring Bean 設定

### Stereotype Annotation 使用

```java
// 領域層 - Domain Service 使用 @Component
@Component  // 非 @Service - 保持領域純淨
public class PricingService {
    public Money calculateTotal(Order order) {
        // 純領域邏輯
    }
}

// 應用層 - 使用 @Service
@Service
@Transactional  // 交易邊界在此
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    
    public OrderApplicationService(OrderRepository orderRepository,
                                  EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public OrderId placeOrder(PlaceOrderCommand command) {
        // 編排領域物件
    }
}

// 基礎設施層 - 使用 @Repository、@Component
@Repository
public class JpaOrderRepository implements OrderRepository {
    // Spring Data JPA 實作
}

// 表現層 - 使用 @RestController
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    // REST 端點
}
```

### 相依性注入

**建構子注入**（必要）：
```java
@Service
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    
    // 建構子注入 - 必要的相依性
    public OrderApplicationService(OrderRepository orderRepository,
                                  EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
}
```

**欄位注入**（生產程式碼中避免）：
```java
// ❌ 避免這樣做
@Service
public class BadService {
    @Autowired
    private OrderRepository repository;
}
```

## Domain Event 處理

### 發布事件

**領域層 - Event 定義**：
```java
public class OrderPlacedEvent implements DomainEvent {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money totalAmount;
    private final Instant occurredOn;
    
    // 建構子、getter
}
```

**應用層 - 發布**：
```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void placeOrder(PlaceOrderCommand command) {
        Order order = createOrder(command);
        orderRepository.save(order);
        
        // 持久化後發布
        eventPublisher.publishEvent(new OrderPlacedEvent(order));
    }
}
```

### 消費事件

**同步（同一 Context）**：
```java
@Component
public class OrderEventHandler {
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // 在獨立交易中處理
        log.info("訂單已下: {}", event.getOrderId());
        // 更新讀取模型、傳送通知等
    }
}
```

**非同步（透過 Kafka 跨 Context）**：
```java
@Component
public class ShippingEventListener {
    
    @KafkaListener(topics = "order-events", groupId = "shipping-service")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // 在 Shipping context 中處理訂單
        shippingService.prepareShipment(event);
    }
}
```

## Repository 模式實作

**領域介面**（純粹，無 Spring）：
```java
package com.company.project.order.domain.repository;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    void delete(Order order);
}
```

**基礎設施實作**：
```java
package com.company.project.order.infrastructure.persistence;

@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value());
    }
}

// Spring Data JPA Interface
interface OrderJpaRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerId(UUID customerId);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.placedAt > :since")
    List<Order> findRecentByStatus(@Param("status") OrderStatus status,
                                   @Param("since") Instant since);
}
```

## 交易管理

### @Transactional 規則

**僅在應用服務層**：
```java
@Service
@Transactional  // 類別層級適用所有方法
public class OrderApplicationService {
    
    public void placeOrder(PlaceOrderCommand command) {
        // 交易在此開始
        Order order = Order.create(/*...*/);
        orderRepository.save(order);
        // 方法後交易提交
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditOrder(OrderId orderId) {
        // 新交易，獨立提交/回滾
    }
    
    @Transactional(readOnly = true)
    public OrderDTO getOrder(OrderId orderId) {
        // 唯讀最佳化
        return orderRepository.findById(orderId)
            .map(OrderDTO::from)
            .orElseThrow();
    }
}
```

**不在 Domain Service 中**：
```java
@Component  // 此處無 @Transactional
public class PricingService {
    
    public Money calculateTotal(Order order) {
        // 純領域邏輯，不需要交易
        return order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

## Value Object 持久化

### 嵌入式 Value Object

```java
@Entity
public class Order {
    
    @Id
    private UUID id;
    
    // 簡單 Value Object
    @Embedded
    private CustomerId customerId;
    
    // 有欄位覆寫的 Value Object
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money totalAmount;
    
    // 複雜 Value Object（Address）
    @Embedded
    private ShippingAddress shippingAddress;
}

@Embeddable
public class Money {
    private BigDecimal amount;
    private String currency;
    
    // 不可變，有業務方法
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("幣別不符");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

## REST API 實作

### Controller 層

```java
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
    
    private final OrderApplicationService orderService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        PlaceOrderCommand command = toCommand(request);
        OrderId orderId = orderService.placeOrder(command);
        
        OrderDTO dto = orderService.getOrder(orderId);
        OrderResponse response = OrderResponse.from(dto);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(orderId.value())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
    
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        OrderDTO dto = orderService.getOrder(new OrderId(id));
        return OrderResponse.from(dto);
    }
}
```

### 全域例外處理

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setTitle("實體未找到");
        problem.setType(URI.create("/errors/not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
```
```

## 08-橫切關注點.md 範本

```markdown
---
title: 橫切關注點
date: YYYY-MM-DD
---

# 橫切關注點

## 安全性

### 身份驗證

**機制**：透過 Spring Security 的 JWT token

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
}
```

### 授權

**方法層級安全**：
```java
@Service
public class OrderApplicationService {
    
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderId placeOrder(PlaceOrderCommand command) {
        // 僅客戶可下訂單
    }
    
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#orderId, authentication)")
    public void cancelOrder(OrderId orderId) {
        // 管理員或訂單擁有者可取消
    }
}
```

## 可觀測性

### 日誌記錄

**設定**：
```yaml
logging:
  level:
    root: INFO
    com.company.project: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: /var/log/order-service/application.log
```

**結構化日誌**：
```java
@Slf4j
@Service
public class OrderApplicationService {
    
    @Transactional
    public OrderId placeOrder(PlaceOrderCommand command) {
        log.info("為客戶下訂單: {}", 
            command.getCustomerId(),
            kv("customerId", command.getCustomerId()),
            kv("itemCount", command.getItems().size()));
        
        return order.getId();
    }
}
```

### 指標

**Spring Boot Actuator + Micrometer**：
```yaml
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

**自訂指標**：
```java
@Component
public class OrderMetrics {
    
    private final Counter ordersPlaced;
    private final Timer orderProcessingTime;
    
    public OrderMetrics(MeterRegistry registry) {
        this.ordersPlaced = Counter.builder("orders.placed")
            .description("已下訂單總數")
            .register(registry);
        
        this.orderProcessingTime = Timer.builder("order.processing.time")
            .description("處理訂單時間")
            .register(registry);
    }
    
    public void recordOrderPlaced(Order order) {
        ordersPlaced.increment();
    }
}
```

### 分散式追蹤

**Micrometer Tracing**：
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 開發環境 100%，生產環境降低
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

## 錯誤處理

### Domain Exception

```java
// 基底 domain exception
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}

// 特定 domain exception
public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(OrderId orderId) {
        super("訂單未找到: " + orderId);
    }
}
```

### RFC 9457 Problem Details

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFound(OrderNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        detail.setTitle("訂單未找到");
        detail.setType(URI.create("/errors/order-not-found"));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }
}
```
```

## 10-測試策略.md 範本

```markdown
---
title: 測試策略
date: YYYY-MM-DD
---

# 測試策略

## 測試金字塔

```
        /\
       /  \    E2E 測試 (5%)
      /    \   少數高價值情境
     /------\
    /        \  整合測試 (20%)
   /          \ API、Repository、訊息
  /------------\
 /              \ 單元測試 (75%)
/________________\ 領域邏輯、工具
```

## 單元測試

### Domain Model 測試

**純領域邏輯，無 Spring 相依性**：

```java
class OrderTest {
    
    @Test
    void shouldCalculateTotalAmountCorrectly() {
        // Given
        Order order = Order.create(
            new CustomerId(UUID.randomUUID()),
            new ShippingAddress(/*...*/)
        );
        
        Money unitPrice1 = Money.of(10.00, "USD");
        Money unitPrice2 = Money.of(20.00, "USD");
        
        // When
        order.addLineItem(new ProductId(UUID.randomUUID()), 2, unitPrice1);
        order.addLineItem(new ProductId(UUID.randomUUID()), 1, unitPrice2);
        
        // Then
        Money expected = Money.of(40.00, "USD");
        assertThat(order.getTotalAmount()).isEqualTo(expected);
    }
    
    @Test
    void shouldEnforceInvariantWhenPlacingOrder() {
        // Given
        Order order = Order.create(/*...*/);
        
        // When & Then
        assertThatThrownBy(() -> order.placeOrder())
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("無法下沒有項目的訂單");
    }
}
```

## 整合測試

### Repository 測試

**使用 @DataJpaTest 進行資料庫整合**：

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaOrderRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
    
    @Autowired
    private OrderJpaRepository jpaRepository;
    
    @Test
    void shouldSaveAndRetrieveOrder() {
        // Given
        Order order = Order.create(/*...*/);
        order.addLineItem(/*...*/);
        
        // When
        Order saved = repository.save(order);
        Optional<Order> retrieved = repository.findById(saved.getId());
        
        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getItems()).hasSize(1);
    }
}
```

### API 整合測試

**使用 @SpringBootTest 測試 REST 端點**：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(/*...*/);
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/v1/orders",
            request,
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().orderId()).isNotNull();
    }
}
```

## 測試涵蓋率目標

- **單元測試**：> 80% 程式碼覆蓋率
- **整合測試**：所有 repository 方法、關鍵 API 端點
- **E2E 測試**：主要使用者旅程
- **效能測試**：預期負載情境

## 持續測試

- 每次提交都執行單元和整合測試
- Pull request 時執行 E2E 測試
- 每晚執行效能測試
- 測試結果發布至 CI/CD 儀表板
```
