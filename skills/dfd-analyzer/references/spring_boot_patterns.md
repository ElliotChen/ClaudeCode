# Spring Boot Architecture Patterns to DFD Mapping

This reference guide maps common Spring Boot architecture patterns to DFD (Data Flow Diagram) components.

## DFD Component Mapping

### External Entities (外部實體)

External entities are sources or destinations of data outside the system boundary.

**Spring Boot Patterns:**

1. **REST API Clients**
   - Any external system calling your REST endpoints
   - Frontend applications (React, Angular, Vue)
   - Mobile apps
   - Third-party integrations
   
   ```java
   @RestController  // Indicates this accepts external requests
   @RequestMapping("/api/orders")
   public class OrderController { }
   ```

2. **External Services**
   - Feign clients calling other microservices
   - RestTemplate/WebClient calls
   - Third-party APIs (payment gateways, email services)
   
   ```java
   @FeignClient(name = "inventory-service")
   public interface InventoryClient { }
   ```

3. **Message Producers/Consumers**
   - Kafka producers/consumers
   - RabbitMQ publishers/subscribers
   
   ```java
   @KafkaListener(topics = "orders")
   public void handleOrder(OrderEvent event) { }
   ```

### Processes (處理程序)

Processes transform or manipulate data.

**Spring Boot Patterns:**

1. **Controllers** - API layer processes
   ```java
   @RestController
   public class OrderController {
       @PostMapping("/orders")
       public OrderResponse createOrder(@RequestBody OrderRequest request) {
           // Process: Validate request, delegate to service
       }
   }
   ```

2. **Services** - Business logic processes
   ```java
   @Service
   public class OrderService {
       public Order processOrder(OrderRequest request) {
           // Process: Business validation, calculations, orchestration
       }
   }
   ```

3. **Components** - Utility or infrastructure processes
   ```java
   @Component
   public class PriceCalculator {
       public BigDecimal calculate(Order order) {
           // Process: Price calculation logic
       }
   }
   ```

4. **Event Handlers** - Asynchronous processes
   ```java
   @EventListener
   public void handleOrderCreated(OrderCreatedEvent event) {
       // Process: React to domain events
   }
   ```

### Data Stores (資料儲存)

Data stores are where information is held temporarily or permanently.

**Spring Boot Patterns:**

1. **Relational Databases**
   ```java
   @Entity
   @Table(name = "orders")
   public class Order { }
   
   @Repository
   public interface OrderRepository extends JpaRepository<Order, Long> { }
   ```

2. **Caches**
   ```java
   @Cacheable("products")
   public Product findProduct(String id) { }
   
   @CacheEvict("products")
   public void updateProduct(Product product) { }
   ```

3. **NoSQL Databases**
   ```java
   @Document(collection = "events")
   public class EventLog { }
   
   public interface EventLogRepository extends MongoRepository<EventLog, String> { }
   ```

4. **Message Queues**
   ```java
   @Component
   public class OrderQueue {
       @Autowired
       private RabbitTemplate rabbitTemplate;
       
       public void enqueue(Order order) {
           rabbitTemplate.convertAndSend("order-queue", order);
       }
   }
   ```

### Data Flows (資料流)

Data flows represent the movement of information between components.

**Spring Boot Patterns:**

1. **HTTP Request/Response**
   ```java
   // Flow: Client → Controller
   @PostMapping("/orders")
   public ResponseEntity<OrderDTO> create(@RequestBody OrderDTO dto) {
       // Request data flow in, response data flow out
   }
   ```

2. **Service Dependencies**
   ```java
   @Service
   public class OrderService {
       @Autowired
       private InventoryService inventoryService;  // Data flows to InventoryService
       
       public Order create(OrderRequest request) {
           Inventory inventory = inventoryService.check(request.getProductId());
           // Data flow: request → InventoryService → inventory data back
       }
   }
   ```

3. **Repository Operations**
   ```java
   // Flow: Service → Repository → Database
   orderRepository.save(order);  // Data flows to database
   Order found = orderRepository.findById(id);  // Data flows from database
   ```

4. **Event Publishing**
   ```java
   // Flow: Service → Event Bus → Event Handlers
   applicationEventPublisher.publishEvent(new OrderCreatedEvent(order));
   ```

## Common Architecture Patterns

### Layered Architecture DFD

```
[Client] → [Controller] → [Service] → [Repository] → [Database]
           ↓               ↓           ↓
         [DTO]        [Domain Model]  [Entity]
```

**Key Data Flows:**
- Client sends HTTP request with DTO
- Controller validates and passes to Service
- Service executes business logic, calls Repository
- Repository interacts with Database via JPA Entity
- Response flows back through the layers

### Microservice Architecture DFD

```
[Frontend] → [API Gateway] → [Order Service] → [Order DB]
                              ↓
                         [Feign Client] → [Inventory Service] → [Inventory DB]
                              ↓
                         [Kafka Producer] → [Event Bus] → [Notification Service]
```

**Key Data Flows:**
- Synchronous: HTTP between services via Feign
- Asynchronous: Events via Kafka/RabbitMQ
- Data persistence: Each service to its own database

### Event-Driven Architecture DFD

```
[Command Handler] → [Aggregate] → [Event Store]
                         ↓
                   [Domain Events] → [Event Bus]
                                        ↓
                              [Event Handler 1] → [Read Model 1]
                              [Event Handler 2] → [Read Model 2]
```

**Key Data Flows:**
- Commands trigger aggregates
- Aggregates emit domain events
- Events stored and published
- Multiple handlers react independently

## DFD Levels for Spring Boot

### Level 0 (Context Diagram)
- Shows: System boundary and external entities
- Spring Boot equivalent: System boundary around all services

### Level 1 (High-Level)
- Shows: Major subsystems/services
- Spring Boot equivalent: Controller → Service → Repository layers

### Level 2 (Detailed)
- Shows: Individual classes and methods
- Spring Boot equivalent: Specific service methods, DTOs, entities

## Best Practices

1. **Group Related Processes**
   - Services handling similar domain concepts should be grouped
   - Example: OrderService, OrderValidator, OrderCalculator → "Order Processing"

2. **Show Key Data Transformations**
   - DTO → Domain Model → Entity transformations
   - Request validation and enrichment

3. **Indicate Async vs Sync**
   - Use different arrow styles for synchronous (solid) vs asynchronous (dashed) flows

4. **Highlight External Boundaries**
   - Clearly mark microservice boundaries
   - Distinguish internal vs external API calls

5. **Include Error Flows**
   - Exception handling paths
   - Fallback mechanisms (Circuit Breaker, Retry)

## Anti-Patterns to Avoid

1. **Too Much Detail**: Don't include every private method or utility class
2. **Missing Layers**: Ensure you capture all architectural layers
3. **Unclear Boundaries**: Always show what's inside vs outside the system
4. **Ignoring Async**: Event-driven and message flows are crucial in modern systems
