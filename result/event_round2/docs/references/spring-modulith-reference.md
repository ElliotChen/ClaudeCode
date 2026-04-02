# Spring Modulith 實踐參考文件

> 本文件供 Claude Code 在實做 Spring Boot 模組化單體架構時，作為規格與實做建議參考。
> 基於 Spring Modulith 2.0.x（相容 Spring Boot 3.4+，Java 17+）。

---

## 1. 核心概念

Spring Modulith 讓開發者在單一 Spring Boot 應用程式中實現邏輯模組化。它提供四大能力：結構驗證（Verification）、模組整合測試（Integration Testing）、執行期可觀測性（Observability）、以及自動文件產生（Documentation）。

一個 Application Module 由以下元素組成：對外暴露的 API（由 Spring Bean 與 Application Event 構成的 Provided Interface）、以及不應被其他模組存取的內部實作元件。

---

## 2. 專案設定

### 2.1 Maven BOM 與依賴

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>2.0.5</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- 核心 API -->
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
  </dependency>

  <!-- 測試支援 -->
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
  </dependency>

  <!-- 事件持久化（依需求擇一） -->
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
  </dependency>
  <!-- 其他選項: spring-modulith-starter-jdbc, spring-modulith-starter-mongodb, spring-modulith-starter-neo4j -->

  <!-- 事件外部化（依需求擇一） -->
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-kafka</artifactId>
  </dependency>
  <!-- 其他選項: spring-modulith-events-amqp, spring-modulith-events-jms,
       spring-modulith-events-aws-sqs, spring-modulith-events-aws-sns -->

  <!-- 可觀測性 -->
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-observability</artifactId>
  </dependency>
</dependencies>
```

### 2.2 Gradle

```groovy
dependencyManagement {
    imports {
        mavenBom 'org.springframework.modulith:spring-modulith-bom:2.0.5'
    }
}

dependencies {
    implementation 'org.springframework.modulith:spring-modulith-starter-core'
    implementation 'org.springframework.modulith:spring-modulith-starter-jpa'
    testImplementation 'org.springframework.modulith:spring-modulith-starter-test'
}
```

---

## 3. 套件結構規範

### 3.1 預設模組偵測

Spring Modulith 以主應用程式類別所在套件為根套件，其直接子套件各自構成一個 Application Module。

```
com.example.app                    ← 根套件（含 @SpringBootApplication）
├── order/                         ← order 模組
│   ├── OrderService.java          ← API（根套件中的類別對外公開）
│   ├── OrderCreatedEvent.java     ← 對外事件
│   └── internal/                  ← 內部實作（其他模組不可存取）
│       ├── OrderRepository.java
│       └── OrderValidator.java
├── inventory/                     ← inventory 模組
│   ├── InventoryService.java
│   └── internal/
│       └── StockRepository.java
└── catalog/                       ← catalog 模組
    ├── ProductService.java
    └── internal/
        └── ProductRepository.java
```

**關鍵規則：**

- 模組根套件內的所有 public 類別構成該模組的 API，允許其他模組依賴。
- 子套件（如 `internal/`）中的類別為內部實作，其他模組不得引用。
- 違反此規則會在結構驗證時報錯。

### 3.2 開放模組（Open Module）

若暫時需要允許所有套件被外部存取（例如遷移過渡期），可將模組標記為 OPEN：

```java
// order/package-info.java
@org.springframework.modulith.ApplicationModule(
    type = ApplicationModule.Type.OPEN
)
package com.example.app.order;
```

**實做建議：** 僅在遷移階段使用 OPEN，最終目標應回到 CLOSED（預設值）。

---

## 4. 模組依賴控制

### 4.1 @ApplicationModule — 宣告允許的依賴

在模組的 `package-info.java` 中使用 `allowedDependencies` 明確限制該模組可以依賴哪些其他模組。

```java
// inventory/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "order", "catalog" }
)
package com.example.app.inventory;
```

若該模組的程式碼引用了未列在 `allowedDependencies` 中的模組，結構驗證將失敗。

### 4.2 @NamedInterface — 暴露額外套件

若需要將模組根套件以外的特定套件公開給其他模組（例如 DTO、SPI），使用 `@NamedInterface`：

```java
// order/dto/package-info.java
@org.springframework.modulith.NamedInterface("dto")
package com.example.app.order.dto;
```

在依賴方以雙冒號語法引用特定命名介面：

```java
// inventory/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "order::dto" }
)
package com.example.app.inventory;
```

若允許某模組的所有命名介面，使用萬用字元：

```java
allowedDependencies = { "order::*" }
```

### 4.3 結構驗證

在測試中執行結構驗證，確認所有模組依賴符合宣告：

```java
class ModularityTests {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify();
    }
}
```

**實做建議：** 將此測試加入 CI/CD Pipeline，確保每次提交都通過結構驗證。

---

## 5. 模組間通訊 — Application Events

### 5.1 設計原則

模組之間應優先使用 Application Events 進行解耦通訊，而非直接呼叫對方的 Bean。這使得模組可以獨立演進，未來也更容易拆分為微服務。

### 5.2 定義事件

```java
// order 模組的對外事件
package com.example.app.order;

public record OrderCompletedEvent(String orderId, String customerId) {
}
```

**實做建議：** 事件類別應為不可變（使用 record 或加上 final field）。事件放在模組根套件中，使其成為模組公開 API 的一部分。

### 5.3 發布事件

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationEventPublisher events;

    @Transactional
    public void completeOrder(String orderId) {
        // 業務邏輯...
        events.publishEvent(new OrderCompletedEvent(orderId, customerId));
    }
}
```

### 5.4 監聽事件 — @ApplicationModuleListener

`@ApplicationModuleListener` 是 Spring Modulith 提供的組合註解，等同於同時使用 `@Async` + `@Transactional` + `@TransactionalEventListener`。這是模組間整合的推薦預設方式。

```java
// inventory 模組
@Service
public class InventoryEventHandler {

    @ApplicationModuleListener
    void on(OrderCompletedEvent event) {
        // 在獨立交易中非同步處理
        // 扣減庫存...
    }
}
```

**關鍵行為：**

- 事件發布方的交易完成後，監聽方才會被觸發。
- 監聽方在獨立交易中執行。
- 若監聽方失敗，不影響發布方的交易。

### 5.5 Event Publication Registry — 事件發布登錄

Spring Modulith 的 Event Publication Registry 在事件發布時，為每一個交易式事件監聽器寫入一筆登錄記錄。當監聽器成功執行完畢，該記錄才會被標記為完成。若監聽器失敗，記錄保持未完成狀態，可用於重試。

**Spring Modulith 2.0 事件生命週期狀態：**

| 狀態 | 說明 |
|------|------|
| PUBLISHED | 已儲存，等待處理 |
| PROCESSING | 監聽器已認領，正在執行中 |
| COMPLETED | 監聽器執行成功 |
| FAILED | 執行失敗（透過 staleness 機制判定） |

**Staleness 監控配置：**

```yaml
spring:
  modulith:
    events:
      staleness:
        # 控制各狀態在多久後被視為失敗
        published: 5m
        processing: 10m
```

**持久化後端選擇（擇一加入依賴）：**

- `spring-modulith-starter-jpa` — 使用 JPA 儲存事件發布記錄
- `spring-modulith-starter-jdbc` — 使用 JDBC
- `spring-modulith-starter-mongodb` — 使用 MongoDB
- `spring-modulith-starter-neo4j` — 使用 Neo4j

**實做建議：** 正式環境務必啟用持久化 Event Publication Registry，以確保事件不會因應用程式重啟而遺失。

---

## 6. 事件外部化（Event Externalization）

### 6.1 @Externalized — 將事件發布至外部訊息代理

透過 `@Externalized` 註解，將應用程式事件自動發布至 Kafka、RabbitMQ 等外部訊息代理。

```java
@org.springframework.modulith.events.Externalized("order-events.OrderCompleted")
public record OrderCompletedEvent(String orderId, String customerId) {
}
```

### 6.2 路由目標與路由鍵

註解值的格式為 `target::key`：

```java
// 靜態 topic
@Externalized("order-events")

// 帶動態路由鍵（SpEL 表達式）
@Externalized("order-events::#{orderId()}")
```

若未指定目標，Spring Modulith 會使用應用程式本地型別名稱作為預設目標（例如 `order.OrderCompletedEvent`）。

### 6.3 支援的訊息代理

| 依賴 | 訊息代理 |
|------|----------|
| `spring-modulith-events-kafka` | Apache Kafka |
| `spring-modulith-events-amqp` | RabbitMQ (AMQP) |
| `spring-modulith-events-jms` | JMS |
| `spring-modulith-events-aws-sqs` | AWS SQS |
| `spring-modulith-events-aws-sns` | AWS SNS |

也可搭配 Spring Cloud Stream 同時支援多個訊息代理。

**實做建議：** Event Externalization 與 Event Publication Registry 搭配使用，確保訊息發布的可靠性。即使訊息代理暫時不可用，事件記錄也會保留以供重試。

---

## 7. 整合測試

### 7.1 @ApplicationModuleTest

此註解啟動模組隔離的整合測試，僅載入被測模組及其相依模組的 Spring 元件。

```java
package com.example.app.order;

@ApplicationModuleTest
class OrderModuleIntegrationTests {

    @Test
    void contextLoads() {
        // 僅載入 order 模組的 bean
    }
}
```

**測試類別必須放在對應模組的套件中。**

### 7.2 Bootstrap Modes

```java
// 僅載入當前模組（預設）
@ApplicationModuleTest

// 載入當前模組及其所有依賴模組
@ApplicationModuleTest(BootstrapMode.ALL_DEPENDENCIES)

// 載入當前模組及額外指定的模組
@ApplicationModuleTest(extraIncludes = "catalog")
```

### 7.3 處理跨模組依賴

若被測模組依賴其他模組的 Bean，但那些模組未被載入，啟動會失敗。使用 `@MockitoBean` 建立模擬物件：

```java
@ApplicationModuleTest
class OrderModuleIntegrationTests {

    @MockitoBean
    InventoryService inventoryService;  // mock 掉未載入的模組

    @Test
    void createsOrder() {
        // ...
    }
}
```

### 7.4 Scenario API — 整合測試 DSL

Scenario API 提供流暢的 DSL 來定義模組整合測試場景。一個 Scenario 由刺激（stimulus）和預期結果（expected outcome）組成。

```java
@ApplicationModuleTest
class OrderModuleIntegrationTests {

    @Test
    void completingOrderPublishesEvent(Scenario scenario) {
        scenario.stimulate(() -> orderService.completeOrder("order-1"))
                .andWaitForEventOfType(OrderCompletedEvent.class)
                .matchingMappedValue(OrderCompletedEvent::orderId, "order-1")
                .toArrive();
    }

    @Test
    void orderCompletionTriggersInventoryUpdate(Scenario scenario) {
        scenario.publish(new OrderCompletedEvent("order-1", "customer-1"))
                .andWaitForStateChange(() -> inventoryRepository.findByOrderId("order-1"))
                .andVerify(inventory -> assertThat(inventory).isNotNull());
    }
}
```

**實做建議：** 使用 Scenario API 驗證模組間的事件驅動互動，而非直接測試監聽器方法。

---

## 8. 可觀測性（Observability）

加入 `spring-modulith-observability` 依賴後，Spring Modulith 會自動為模組間的互動添加追蹤（Tracing）資訊。

### 8.1 依賴

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-observability</artifactId>
</dependency>
```

需搭配 Micrometer Tracing（已包含在 Spring Boot Actuator 中）。

### 8.2 功能

- 自動在分散式追蹤 Span 上標記來源與目標模組名稱。
- 追蹤上下文（Tracing Context）自動跨事件驅動的模組邊界傳播。
- 產生模組層級的 Micrometer 指標。

---

## 9. 文件產生（Documentation）

### 9.1 Documenter API

Spring Modulith 可自動產生兩種文件：模組間關係的 C4/UML 元件圖、以及 Application Module Canvas（模組概覽表）。

```java
class DocumentationTests {

    @Test
    void generateDocumentation() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        new Documenter(modules).writeDocumentation();
    }
}
```

### 9.2 產生內容

| 方法 | 產出 |
|------|------|
| `writeDocumentation()` | 完整文件（包含以下所有內容） |
| `writeModulesAsPlantUml()` | 全域模組關係的 PlantUML 圖（支援 UML 或 C4 風格） |
| `writeIndividualModulesAsPlantUml()` | 各模組的上游依賴 PlantUML 圖 |
| `writeModuleCanvases()` | 各模組的 Canvas 表 |

Application Module Canvas 包含：模組內的 Spring Bean、Aggregate Root、發布與監聽的事件、以及 Configuration Properties。

**預設輸出位置：** 建置系統 build 資料夾下的 `spring-modulith-docs/` 目錄。

**實做建議：** 將文件產生整合進 CI/CD，每次建置自動更新架構文件。

---

## 10. 模組專屬 Flyway 遷移（2.0+）

Spring Modulith 2.0 支援各模組擁有獨立的 Flyway 資料庫遷移腳本，使資料庫 Schema 也能按模組管理。

```
src/main/resources/
└── db/migration/
    ├── order/
    │   ├── V1__create_orders_table.sql
    │   └── V2__add_order_status.sql
    └── inventory/
        └── V1__create_inventory_table.sql
```

---

## 11. 實做檢查清單

在實做 Spring Modulith 專案時，請依序確認以下要點：

**專案結構：**

- 主應用程式類別放在根套件（如 `com.example.app`）。
- 每個業務領域對應一個直接子套件作為模組。
- 模組根套件只放對外 API（Service interface、Event class、DTO）。
- 內部實作放在模組子套件中（慣例命名 `internal`）。

**依賴管理：**

- 在每個模組的 `package-info.java` 使用 `@ApplicationModule(allowedDependencies = {...})` 明確宣告依賴。
- 需要暴露額外套件時，使用 `@NamedInterface`。
- 加入結構驗證測試並納入 CI。

**模組間通訊：**

- 優先使用 Application Events 解耦模組。
- 事件監聽器使用 `@ApplicationModuleListener`。
- 啟用持久化 Event Publication Registry。
- 需要發布至外部訊息代理時，使用 `@Externalized`。

**測試：**

- 使用 `@ApplicationModuleTest` 進行模組隔離測試。
- 使用 Scenario API 驗證事件驅動流程。
- 跨模組 Bean 依賴使用 `@MockitoBean`。

**營運：**

- 加入 `spring-modulith-observability` 啟用模組層級追蹤。
- 使用 Documenter 自動產生並維護架構文件。

---

## 12. 常見反模式與避免方式

**反模式 1：模組間直接 Bean 注入**
避免在模組 A 直接注入模組 B 的 Repository 或 Internal Service。應透過模組 B 公開的 API Service 或事件通訊。

**反模式 2：循環依賴**
若模組 A 依賴模組 B，同時 B 又依賴 A，應引入事件機制打破循環。例如 A 呼叫 B 的 Service，B 發布事件通知 A。

**反模式 3：忽略 Event Publication Registry**
不啟用持久化的 Event Publication Registry 在正式環境中可能導致事件遺失。務必加入 JPA/JDBC/MongoDB 之一的 starter。

**反模式 4：濫用 Open Module**
`ApplicationModule.Type.OPEN` 應僅用於遷移過渡期。長期維持 OPEN 會喪失模組封裝的保護效果。

---

## 13. 版本對應

| Spring Modulith | Spring Boot | Java |
|-----------------|-------------|------|
| 2.0.x | 3.4.x | 17+ |
| 1.4.x | 3.4.x | 17+ |
| 1.3.x | 3.3.x | 17+ |

---

## 附錄：關鍵 Annotation 速查

| Annotation | 用途 | 位置 |
|------------|------|------|
| `@ApplicationModule` | 宣告模組依賴與類型 | `package-info.java` |
| `@NamedInterface` | 暴露額外套件為命名介面 | 子套件的 `package-info.java` |
| `@ApplicationModuleListener` | 非同步交易式事件監聽（推薦預設） | 監聽方法 |
| `@Externalized` | 標記事件外部化至訊息代理 | 事件類別 |
| `@ApplicationModuleTest` | 模組隔離整合測試 | 測試類別 |
