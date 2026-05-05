# Spring Modulith 實作原則

## 核心規則

### 1. 模組依賴必須透過事件解耦

**錯誤**：模組 A 的 Service 直接注入模組 B 的 Repository
```java
// ❌ 錯誤：違反模組邊界
@Service
public class DepartmentService {
    private final EmployeeRepository employeeRepository;  // 直接依賴他模組內部實現
}
```

**正確**：模組間透過領域事件通訊
```java
// ✅ 正確：透過事件解耦
@Service
public class DepartmentService {
    private final ApplicationEventPublisher events;

    public void hireEmployee(UUID employeeId, UUID departmentId) {
        // 發布事件，由 employee 模組自己處理
        events.publish(new EmployeeHiredEvent(employeeId, departmentId));
    }
}
```

### 2. 每個模組必須有 package-info.java 定義邊界

```java
// src/main/java/tw/elliot/cctest/department/package-info.java
@ApplicationModule(
    allowedDependencies = {"employee"},
    name = "department"
)
package tw.elliot.cctest.department;

// src/main/java/tw/elliot/cctest/employee/package-info.java
@ApplicationModule(
    allowedDependencies = {},  // employee 不依賴任何其他模組
    name = "employee"
)
package tw.elliot.cctest.employee;
```

### 3. 模組內部實現必須隱藏

```
module-name/
├── api/              # 公共 API (RestController, DTO)
├── Entity.java       # 實體 (可被其他模組透過 Repository 訪問)
├── ModuleService.java # 內部服務 (不應該被其他模組直接訪問)
└── package-info.java # 模組邊界定義
```

### 4. 使用 BOM 管理版本

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>${spring-modulith.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 依賴不需要指定版本 -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
```

### 5. 必須有架構驗證測試

```java
@Test
void verifyModularStructure() {
    ApplicationModules modules = ApplicationModules.of(CctestApplication.class);
    modules.verify();  // 驗證模組邊界
}
```

## 檢查清單

在實作 Spring Modulith 專案時，必須確認：

- [ ] 使用 Spring Modulith BOM 管理版本
- [ ] 每個模組有 `package-info.java` 定義邊界
- [ ] 模組間不直接注入對方的 Service/Repository
- [ ] 跨模組操作使用 `ApplicationEventPublisher` 發布事件
- [ ] 事件處理使用 `@ApplicationModuleListener`
- [ ] 有架構驗證測試
- [ ] `api` package 放置公共 API（Controller, DTO）
- [ ] 實體放在模組根目錄（可被 JPA 掃描）

## 常見錯誤

| 錯誤 | 正確做法 |
|------|---------|
| Service 注入他模組 Repository | 發布事件，由他模組自己監聽處理 |
| 沒有 package-info.java | 每個模組必須定義邊界 |
| 直接使用 `@EventListener` | 使用 `@ApplicationModuleListener` |
| 手動管理 Modulith 版本 | 使用 BOM |
| 沒有驗證測試 | 必須有 `modules.verify()` 測試 |
