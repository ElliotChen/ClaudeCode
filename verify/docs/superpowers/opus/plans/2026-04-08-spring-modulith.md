# Spring Modulith Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Spring Modulith 2.0.3 with Employee, Department, and HR modules — including domain logic, events, REST APIs, PostgreSQL/Flyway, and comprehensive tests.

**Architecture:** Three modules (Employee, Department, HumanResources) under `tw.elliot.cctest`. Employee is standalone; Department depends on Employee (direct call) and publishes events; HR depends on Employee+Department and listens to Department events via JDBC-backed publication. PostgreSQL 18 with Flyway migrations, Testcontainers for ITs.

**Tech Stack:** Spring Boot 4.0.3, Spring Modulith 2.0.3, JPA/Hibernate, PostgreSQL 18, Flyway, Testcontainers 2.0.3, SpringDoc OpenAPI 3.0.2, Lombok, JUnit 5, Mockito

---

## File Structure

| File | Responsibility |
|---|---|
| `pom.xml` | (modify) Add Modulith, JPA, PostgreSQL, Flyway, SpringDoc, Testcontainers dependencies + failsafe plugin |
| `src/main/resources/application.yaml` | (modify) Add datasource, JPA, Flyway, SpringDoc config |
| `src/main/resources/db/migration/V1__create_department_table.sql` | departments table |
| `src/main/resources/db/migration/V2__create_employee_table.sql` | employees table |
| `src/main/resources/db/migration/V3__create_hr_record_table.sql` | hr_records table |
| `src/main/resources/db/migration/V4__create_event_publication_table.sql` | Spring Modulith event publication table |
| `src/main/java/tw/elliot/cctest/employee/Rank.java` | Enum: STAFF, TEAM_LEAD, MANAGER |
| `src/main/java/tw/elliot/cctest/employee/Status.java` | Enum: ACTIVE, INACTIVE |
| `src/main/java/tw/elliot/cctest/employee/Employee.java` | JPA entity |
| `src/main/java/tw/elliot/cctest/employee/EmployeeRepository.java` | Spring Data JPA repository |
| `src/main/java/tw/elliot/cctest/employee/EmployeeService.java` | Business rules: rank transitions, status, one-manager constraint |
| `src/main/java/tw/elliot/cctest/employee/api/EmployeeDto.java` | Public DTO |
| `src/main/java/tw/elliot/cctest/employee/package-info.java` | Module boundary: no dependencies |
| `src/main/java/tw/elliot/cctest/department/Department.java` | JPA entity |
| `src/main/java/tw/elliot/cctest/department/DepartmentRepository.java` | Spring Data JPA repository |
| `src/main/java/tw/elliot/cctest/department/DepartmentService.java` | Transfer, promote, demote + event publishing |
| `src/main/java/tw/elliot/cctest/department/EmployeeTransferredEvent.java` | Event record |
| `src/main/java/tw/elliot/cctest/department/EmployeePromotedEvent.java` | Event record |
| `src/main/java/tw/elliot/cctest/department/EmployeeDemotedEvent.java` | Event record |
| `src/main/java/tw/elliot/cctest/department/api/DepartmentController.java` | REST API |
| `src/main/java/tw/elliot/cctest/department/api/DepartmentDto.java` | Public DTOs |
| `src/main/java/tw/elliot/cctest/department/package-info.java` | Module boundary: depends on employee |
| `src/main/java/tw/elliot/cctest/humanresources/HrRecord.java` | JPA entity |
| `src/main/java/tw/elliot/cctest/humanresources/ActionType.java` | Enum: HIRED, FIRED, PROMOTED, DEMOTED, TRANSFERRED |
| `src/main/java/tw/elliot/cctest/humanresources/HrRecordRepository.java` | Spring Data JPA repository |
| `src/main/java/tw/elliot/cctest/humanresources/HrService.java` | Hire, fire, event listeners |
| `src/main/java/tw/elliot/cctest/humanresources/EmployeeHiredEvent.java` | Event record |
| `src/main/java/tw/elliot/cctest/humanresources/EmployeeFiredEvent.java` | Event record |
| `src/main/java/tw/elliot/cctest/humanresources/api/HrController.java` | REST API |
| `src/main/java/tw/elliot/cctest/humanresources/api/HrDto.java` | Public DTOs |
| `src/main/java/tw/elliot/cctest/humanresources/package-info.java` | Module boundary: depends on employee, department |
| `src/test/java/tw/elliot/cctest/employee/EmployeeServiceTest.java` | Unit test |
| `src/test/java/tw/elliot/cctest/department/DepartmentServiceTest.java` | Unit test |
| `src/test/java/tw/elliot/cctest/humanresources/HrServiceTest.java` | Unit test |
| `src/test/java/tw/elliot/cctest/TestcontainersConfig.java` | Shared PostgreSQL container config |
| `src/test/java/tw/elliot/cctest/ModularityIT.java` | Module structure verification |
| `src/test/java/tw/elliot/cctest/department/DepartmentControllerIT.java` | Department API integration test |
| `src/test/java/tw/elliot/cctest/humanresources/HrControllerIT.java` | HR API integration test |
| `src/test/java/tw/elliot/cctest/humanresources/EventDeliveryIT.java` | Event publication integration test |

---

### Task 1: Update pom.xml with dependencies and plugins

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add dependencyManagement, new dependencies, and failsafe plugin to pom.xml**

Add `<dependencyManagement>` section before `<dependencies>` with Spring Modulith BOM and Testcontainers BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>2.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>2.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Add these dependencies inside `<dependencies>`:

```xml
<!-- Spring Modulith -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jdbc</artifactId>
</dependency>

<!-- JPA + PostgreSQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Spring Modulith Test -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Add `maven-failsafe-plugin` inside `<plugins>`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
</plugin>
```

- [ ] **Step 2: Verify dependencies resolve**

Run: `mvn dependency:resolve`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add Spring Modulith, JPA, PostgreSQL, Flyway, OpenAPI, Testcontainers dependencies"
```

---

### Task 2: Flyway migrations and application.yaml update

**Files:**
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/resources/db/migration/V1__create_department_table.sql`
- Create: `src/main/resources/db/migration/V2__create_employee_table.sql`
- Create: `src/main/resources/db/migration/V3__create_hr_record_table.sql`
- Create: `src/main/resources/db/migration/V4__create_event_publication_table.sql`

- [ ] **Step 1: Update application.yaml**

Replace the entire file with:

```yaml
spring:
  application:
    name: cctest
  datasource:
    url: jdbc:postgresql://localhost:5432/cctest
    username: cctest
    password: cctest
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      export:
        enabled: true
    metrics:
      export:
        enabled: false

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 2: Create V1__create_department_table.sql**

```sql
CREATE TABLE departments (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500)
);
```

- [ ] **Step 3: Create V2__create_employee_table.sql**

```sql
CREATE TABLE employees (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(50),
    rank          VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    department_id UUID REFERENCES departments(id),
    hire_date     DATE NOT NULL
);

CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_status ON employees(status);
```

- [ ] **Step 4: Create V3__create_hr_record_table.sql**

```sql
CREATE TABLE hr_records (
    id          UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    detail      VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_hr_records_employee_id ON hr_records(employee_id);
```

- [ ] **Step 5: Create V4__create_event_publication_table.sql**

```sql
CREATE TABLE event_publication (
    id               UUID NOT NULL,
    listener_id      TEXT NOT NULL,
    event_type       TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_event_publication_by_completion_date ON event_publication(completion_date);
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.yaml src/main/resources/db/migration/
git commit -m "feat: add Flyway migrations and update application.yaml for PostgreSQL"
```

---

### Task 3: Employee module — enums, entity, repository, DTO, package-info

**Files:**
- Create: `src/main/java/tw/elliot/cctest/employee/Rank.java`
- Create: `src/main/java/tw/elliot/cctest/employee/Status.java`
- Create: `src/main/java/tw/elliot/cctest/employee/Employee.java`
- Create: `src/main/java/tw/elliot/cctest/employee/EmployeeRepository.java`
- Create: `src/main/java/tw/elliot/cctest/employee/api/EmployeeDto.java`
- Create: `src/main/java/tw/elliot/cctest/employee/package-info.java`

- [ ] **Step 1: Create Rank.java**

```java
package tw.elliot.cctest.employee;

public enum Rank {
    STAFF,
    TEAM_LEAD,
    MANAGER;

    public Rank promote() {
        return switch (this) {
            case STAFF -> TEAM_LEAD;
            case TEAM_LEAD -> MANAGER;
            case MANAGER -> throw new IllegalStateException("Cannot promote beyond MANAGER");
        };
    }

    public Rank demote() {
        return switch (this) {
            case MANAGER -> TEAM_LEAD;
            case TEAM_LEAD -> STAFF;
            case STAFF -> throw new IllegalStateException("Cannot demote below STAFF");
        };
    }
}
```

- [ ] **Step 2: Create Status.java**

```java
package tw.elliot.cctest.employee;

public enum Status {
    ACTIVE,
    INACTIVE
}
```

- [ ] **Step 3: Create Employee.java**

```java
package tw.elliot.cctest.employee;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Rank rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Status status;

    @Setter
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    public Employee(String name, String email, String phone, UUID departmentId) {
        this.id = UUID.ofEpochMillis(Instant.now().toEpochMilli());
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.rank = Rank.STAFF;
        this.status = Status.ACTIVE;
        this.departmentId = departmentId;
        this.hireDate = LocalDate.now();
    }
}
```

- [ ] **Step 4: Create EmployeeRepository.java**

```java
package tw.elliot.cctest.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByDepartmentId(UUID departmentId);

    boolean existsByDepartmentIdAndRank(UUID departmentId, Rank rank);
}
```

- [ ] **Step 5: Create EmployeeDto.java**

```java
package tw.elliot.cctest.employee.api;

import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.Rank;
import tw.elliot.cctest.employee.Status;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDto(
        UUID id,
        String name,
        String email,
        String phone,
        Rank rank,
        Status status,
        UUID departmentId,
        LocalDate hireDate
) {
    public static EmployeeDto from(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRank(),
                employee.getStatus(),
                employee.getDepartmentId(),
                employee.getHireDate()
        );
    }
}
```

- [ ] **Step 6: Create package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {}
)
package tw.elliot.cctest.employee;
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/tw/elliot/cctest/employee/
git commit -m "feat: add Employee module — entity, enums, repository, DTO"
```

---

### Task 4: Employee module — EmployeeService

**Files:**
- Create: `src/main/java/tw/elliot/cctest/employee/EmployeeService.java`

- [ ] **Step 1: Create EmployeeService.java**

```java
package tw.elliot.cctest.employee;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public Employee create(String name, String email, String phone, UUID departmentId) {
        var employee = new Employee(name, email, phone, departmentId);
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public Employee findById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Employee> findByDepartmentId(UUID departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    public Employee promote(UUID employeeId) {
        var employee = findById(employeeId);
        Rank newRank = employee.getRank().promote();
        if (newRank == Rank.MANAGER && employee.getDepartmentId() != null) {
            if (employeeRepository.existsByDepartmentIdAndRank(employee.getDepartmentId(), Rank.MANAGER)) {
                throw new IllegalStateException("Department already has a MANAGER");
            }
        }
        employee.setRank(newRank);
        return employeeRepository.save(employee);
    }

    public Employee demote(UUID employeeId) {
        var employee = findById(employeeId);
        Rank newRank = employee.getRank().demote();
        employee.setRank(newRank);
        return employeeRepository.save(employee);
    }

    public Employee transfer(UUID employeeId, UUID toDepartmentId) {
        var employee = findById(employeeId);
        if (employee.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Cannot transfer INACTIVE employee");
        }
        if (employee.getRank() == Rank.MANAGER) {
            if (employeeRepository.existsByDepartmentIdAndRank(toDepartmentId, Rank.MANAGER)) {
                throw new IllegalStateException("Target department already has a MANAGER");
            }
        }
        employee.setDepartmentId(toDepartmentId);
        return employeeRepository.save(employee);
    }

    public Employee fire(UUID employeeId) {
        var employee = findById(employeeId);
        employee.setStatus(Status.INACTIVE);
        employee.setDepartmentId(null);
        return employeeRepository.save(employee);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/employee/EmployeeService.java
git commit -m "feat: add EmployeeService with business rules"
```

---

### Task 5: Employee module — unit tests

**Files:**
- Create: `src/test/java/tw/elliot/cctest/employee/EmployeeServiceTest.java`

- [ ] **Step 1: Create EmployeeServiceTest.java**

```java
package tw.elliot.cctest.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private UUID departmentId;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void promote_fromStaff_shouldBecomeTeamLead() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        var result = employeeService.promote(employee.getId());

        assertThat(result.getRank()).isEqualTo(Rank.TEAM_LEAD);
    }

    @Test
    void promote_fromTeamLead_shouldBecomeManager() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.TEAM_LEAD);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndRank(departmentId, Rank.MANAGER)).thenReturn(false);

        var result = employeeService.promote(employee.getId());

        assertThat(result.getRank()).isEqualTo(Rank.MANAGER);
    }

    @Test
    void promote_fromManager_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.MANAGER);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.promote(employee.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot promote beyond MANAGER");
    }

    @Test
    void promote_toManager_whenDeptAlreadyHasManager_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.TEAM_LEAD);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndRank(departmentId, Rank.MANAGER)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.promote(employee.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Department already has a MANAGER");
    }

    @Test
    void demote_fromManager_shouldBecomeTeamLead() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.MANAGER);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        var result = employeeService.demote(employee.getId());

        assertThat(result.getRank()).isEqualTo(Rank.TEAM_LEAD);
    }

    @Test
    void demote_fromStaff_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.demote(employee.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot demote below STAFF");
    }

    @Test
    void transfer_activeEmployee_shouldChangeDepartment() {
        var oldDeptId = UUID.randomUUID();
        var newDeptId = UUID.randomUUID();
        var employee = new Employee("John", "john@test.com", null, oldDeptId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        var result = employeeService.transfer(employee.getId(), newDeptId);

        assertThat(result.getDepartmentId()).isEqualTo(newDeptId);
    }

    @Test
    void transfer_inactiveEmployee_shouldThrow() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setStatus(Status.INACTIVE);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.transfer(employee.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transfer INACTIVE employee");
    }

    @Test
    void transfer_managerToDeptWithManager_shouldThrow() {
        var newDeptId = UUID.randomUUID();
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employee.setRank(Rank.MANAGER);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndRank(newDeptId, Rank.MANAGER)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.transfer(employee.getId(), newDeptId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Target department already has a MANAGER");
    }

    @Test
    void fire_shouldSetInactiveAndClearDepartment() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        var result = employeeService.fire(employee.getId());

        assertThat(result.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getRank()).isEqualTo(Rank.STAFF); // rank preserved
    }
}
```

- [ ] **Step 2: Run unit tests**

Run: `mvn test -Dtest=EmployeeServiceTest`
Expected: BUILD SUCCESS, 10 tests passed

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/employee/EmployeeServiceTest.java
git commit -m "test: add EmployeeService unit tests"
```

---

### Task 6: Department module — entity, repository, events, DTO, package-info

**Files:**
- Create: `src/main/java/tw/elliot/cctest/department/Department.java`
- Create: `src/main/java/tw/elliot/cctest/department/DepartmentRepository.java`
- Create: `src/main/java/tw/elliot/cctest/department/EmployeeTransferredEvent.java`
- Create: `src/main/java/tw/elliot/cctest/department/EmployeePromotedEvent.java`
- Create: `src/main/java/tw/elliot/cctest/department/EmployeeDemotedEvent.java`
- Create: `src/main/java/tw/elliot/cctest/department/api/DepartmentDto.java`
- Create: `src/main/java/tw/elliot/cctest/department/package-info.java`

- [ ] **Step 1: Create Department.java**

```java
package tw.elliot.cctest.department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    public Department(String name, String description) {
        this.id = UUID.ofEpochMillis(Instant.now().toEpochMilli());
        this.name = name;
        this.description = description;
    }
}
```

- [ ] **Step 2: Create DepartmentRepository.java**

```java
package tw.elliot.cctest.department;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
}
```

- [ ] **Step 3: Create event records**

`EmployeeTransferredEvent.java`:

```java
package tw.elliot.cctest.department;

import java.util.UUID;

public record EmployeeTransferredEvent(UUID employeeId, UUID fromDepartmentId, UUID toDepartmentId) {
}
```

`EmployeePromotedEvent.java`:

```java
package tw.elliot.cctest.department;

import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

public record EmployeePromotedEvent(UUID employeeId, UUID departmentId, Rank fromRank, Rank toRank) {
}
```

`EmployeeDemotedEvent.java`:

```java
package tw.elliot.cctest.department;

import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

public record EmployeeDemotedEvent(UUID employeeId, UUID departmentId, Rank fromRank, Rank toRank) {
}
```

- [ ] **Step 4: Create DepartmentDto.java**

```java
package tw.elliot.cctest.department.api;

import tw.elliot.cctest.department.Department;

import java.util.UUID;

public record DepartmentDto(UUID id, String name, String description) {

    public static DepartmentDto from(Department department) {
        return new DepartmentDto(department.getId(), department.getName(), department.getDescription());
    }
}

```

- [ ] **Step 5: Create package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"employee"}
)
package tw.elliot.cctest.department;
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tw/elliot/cctest/department/
git commit -m "feat: add Department module — entity, repository, events, DTO"
```

---

### Task 7: Department module — DepartmentService

**Files:**
- Create: `src/main/java/tw/elliot/cctest/department/DepartmentService.java`

- [ ] **Step 1: Create DepartmentService.java**

```java
package tw.elliot.cctest.department;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;
import tw.elliot.cctest.employee.api.EmployeeDto;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeService employeeService;
    private final ApplicationEventPublisher eventPublisher;

    public Department create(String name, String description) {
        var department = new Department(name, description);
        return departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public Department findById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> findEmployees(UUID departmentId) {
        findById(departmentId); // verify department exists
        return employeeService.findByDepartmentId(departmentId).stream()
                .map(EmployeeDto::from)
                .toList();
    }

    public EmployeeDto transfer(UUID departmentId, UUID employeeId) {
        findById(departmentId); // verify department exists
        Employee employee = employeeService.findById(employeeId);
        UUID fromDepartmentId = employee.getDepartmentId();
        Employee updated = employeeService.transfer(employeeId, departmentId);
        eventPublisher.publishEvent(new EmployeeTransferredEvent(employeeId, fromDepartmentId, departmentId));
        return EmployeeDto.from(updated);
    }

    public EmployeeDto promote(UUID departmentId, UUID employeeId) {
        findById(departmentId); // verify department exists
        Employee employee = employeeService.findById(employeeId);
        Rank fromRank = employee.getRank();
        Employee updated = employeeService.promote(employeeId);
        eventPublisher.publishEvent(new EmployeePromotedEvent(employeeId, departmentId, fromRank, updated.getRank()));
        return EmployeeDto.from(updated);
    }

    public EmployeeDto demote(UUID departmentId, UUID employeeId) {
        findById(departmentId); // verify department exists
        Employee employee = employeeService.findById(employeeId);
        Rank fromRank = employee.getRank();
        Employee updated = employeeService.demote(employeeId);
        eventPublisher.publishEvent(new EmployeeDemotedEvent(employeeId, departmentId, fromRank, updated.getRank()));
        return EmployeeDto.from(updated);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/department/DepartmentService.java
git commit -m "feat: add DepartmentService with transfer, promote, demote and event publishing"
```

---

### Task 8: Department module — DepartmentController

**Files:**
- Create: `src/main/java/tw/elliot/cctest/department/api/DepartmentController.java`

- [ ] **Step 1: Create DepartmentController.java**

```java
package tw.elliot.cctest.department.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.employee.api.EmployeeDto;

import java.util.List;
import java.util.UUID;

@Tag(name = "Department", description = "Department management")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Create a department")
    @ApiResponse(responseCode = "201", description = "Department created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentDto create(@RequestBody CreateDepartmentRequest request) {
        var department = departmentService.create(request.name(), request.description());
        return DepartmentDto.from(department);
    }

    @Operation(summary = "List all departments")
    @GetMapping
    public List<DepartmentDto> findAll() {
        return departmentService.findAll().stream()
                .map(DepartmentDto::from)
                .toList();
    }

    @Operation(summary = "Get department by ID")
    @ApiResponse(responseCode = "200", description = "Department found")
    @GetMapping("/{id}")
    public DepartmentDto findById(@PathVariable UUID id) {
        return DepartmentDto.from(departmentService.findById(id));
    }

    @Operation(summary = "List employees in a department")
    @GetMapping("/{id}/employees")
    public List<EmployeeDto> findEmployees(@PathVariable UUID id) {
        return departmentService.findEmployees(id);
    }

    @Operation(summary = "Transfer employee to this department")
    @ApiResponse(responseCode = "200", description = "Employee transferred")
    @PostMapping("/{id}/employees/{employeeId}/transfer")
    public EmployeeDto transfer(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return departmentService.transfer(id, employeeId);
    }

    @Operation(summary = "Promote employee")
    @ApiResponse(responseCode = "200", description = "Employee promoted")
    @PostMapping("/{id}/employees/{employeeId}/promote")
    public EmployeeDto promote(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return departmentService.promote(id, employeeId);
    }

    @Operation(summary = "Demote employee")
    @ApiResponse(responseCode = "200", description = "Employee demoted")
    @PostMapping("/{id}/employees/{employeeId}/demote")
    public EmployeeDto demote(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return departmentService.demote(id, employeeId);
    }

    public record CreateDepartmentRequest(String name, String description) {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/department/api/DepartmentController.java
git commit -m "feat: add DepartmentController REST API with OpenAPI annotations"
```

---

### Task 9: Department module — unit tests

**Files:**
- Create: `src/test/java/tw/elliot/cctest/department/DepartmentServiceTest.java`

- [ ] **Step 1: Create DepartmentServiceTest.java**

```java
package tw.elliot.cctest.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private UUID departmentId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Engineering dept");
        departmentId = department.getId();
        employeeId = UUID.randomUUID();
    }

    @Test
    void transfer_shouldPublishTransferredEvent() {
        var oldDeptId = UUID.randomUUID();
        var employee = new Employee("John", "john@test.com", null, oldDeptId);
        employeeId = employee.getId();
        var updatedEmployee = new Employee("John", "john@test.com", null, departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeService.findById(employeeId)).thenReturn(employee);
        when(employeeService.transfer(employeeId, departmentId)).thenReturn(updatedEmployee);

        departmentService.transfer(departmentId, employeeId);

        var captor = ArgumentCaptor.forClass(EmployeeTransferredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.employeeId()).isEqualTo(employeeId);
        assertThat(event.fromDepartmentId()).isEqualTo(oldDeptId);
        assertThat(event.toDepartmentId()).isEqualTo(departmentId);
    }

    @Test
    void promote_shouldPublishPromotedEvent() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employeeId = employee.getId();
        var promotedEmployee = new Employee("John", "john@test.com", null, departmentId);
        promotedEmployee.setRank(Rank.TEAM_LEAD);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeService.findById(employeeId)).thenReturn(employee);
        when(employeeService.promote(employeeId)).thenReturn(promotedEmployee);

        departmentService.promote(departmentId, employeeId);

        var captor = ArgumentCaptor.forClass(EmployeePromotedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.employeeId()).isEqualTo(employeeId);
        assertThat(event.fromRank()).isEqualTo(Rank.STAFF);
        assertThat(event.toRank()).isEqualTo(Rank.TEAM_LEAD);
    }

    @Test
    void demote_shouldPublishDemotedEvent() {
        var employee = new Employee("John", "john@test.com", null, departmentId);
        employeeId = employee.getId();
        employee.setRank(Rank.TEAM_LEAD);
        var demotedEmployee = new Employee("John", "john@test.com", null, departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeService.findById(employeeId)).thenReturn(employee);
        when(employeeService.demote(employeeId)).thenReturn(demotedEmployee);

        departmentService.demote(departmentId, employeeId);

        var captor = ArgumentCaptor.forClass(EmployeeDemotedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.fromRank()).isEqualTo(Rank.TEAM_LEAD);
        assertThat(event.toRank()).isEqualTo(Rank.STAFF);
    }

    @Test
    void create_shouldSaveDepartment() {
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = departmentService.create("Sales", "Sales dept");

        assertThat(result.getName()).isEqualTo("Sales");
        verify(departmentRepository).save(any(Department.class));
    }
}
```

- [ ] **Step 2: Run unit tests**

Run: `mvn test -Dtest=DepartmentServiceTest`
Expected: BUILD SUCCESS, 4 tests passed

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/department/DepartmentServiceTest.java
git commit -m "test: add DepartmentService unit tests"
```

---

### Task 10: HR module — entity, enums, repository, events, DTO, package-info

**Files:**
- Create: `src/main/java/tw/elliot/cctest/humanresources/ActionType.java`
- Create: `src/main/java/tw/elliot/cctest/humanresources/HrRecord.java`
- Create: `src/main/java/tw/elliot/cctest/humanresources/HrRecordRepository.java`
- Create: `src/main/java/tw/elliot/cctest/humanresources/EmployeeHiredEvent.java`
- Create: `src/main/java/tw/elliot/cctest/humanresources/EmployeeFiredEvent.java`
- Create: `src/main/java/tw/elliot/cctest/humanresources/api/HrDto.java`
- Create: `src/main/java/tw/elliot/cctest/humanresources/package-info.java`

- [ ] **Step 1: Create ActionType.java**

```java
package tw.elliot.cctest.humanresources;

public enum ActionType {
    HIRED,
    FIRED,
    PROMOTED,
    DEMOTED,
    TRANSFERRED
}
```

- [ ] **Step 2: Create HrRecord.java**

```java
package tw.elliot.cctest.humanresources;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hr_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HrRecord {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public HrRecord(UUID employeeId, ActionType actionType, String detail) {
        this.id = UUID.ofEpochMillis(Instant.now().toEpochMilli());
        this.employeeId = employeeId;
        this.actionType = actionType;
        this.detail = detail;
        this.occurredAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create HrRecordRepository.java**

```java
package tw.elliot.cctest.humanresources;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HrRecordRepository extends JpaRepository<HrRecord, UUID> {

    List<HrRecord> findByEmployeeId(UUID employeeId);
}
```

- [ ] **Step 4: Create event records**

`EmployeeHiredEvent.java`:

```java
package tw.elliot.cctest.humanresources;

import java.util.UUID;

public record EmployeeHiredEvent(UUID employeeId, UUID departmentId, String name) {
}
```

`EmployeeFiredEvent.java`:

```java
package tw.elliot.cctest.humanresources;

import java.util.UUID;

public record EmployeeFiredEvent(UUID employeeId, String name) {
}
```

- [ ] **Step 5: Create HrDto.java**

```java
package tw.elliot.cctest.humanresources.api;

import tw.elliot.cctest.humanresources.ActionType;
import tw.elliot.cctest.humanresources.HrRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public record HrRecordDto(
        UUID id,
        UUID employeeId,
        ActionType actionType,
        String detail,
        LocalDateTime occurredAt
) {
    public static HrRecordDto from(HrRecord record) {
        return new HrRecordDto(
                record.getId(),
                record.getEmployeeId(),
                record.getActionType(),
                record.getDetail(),
                record.getOccurredAt()
        );
    }
}

```

- [ ] **Step 6: Create package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"employee", "department"}
)
package tw.elliot.cctest.humanresources;
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/tw/elliot/cctest/humanresources/
git commit -m "feat: add HR module — entity, enums, repository, events, DTO"
```

---

### Task 11: HR module — HrService

**Files:**
- Create: `src/main/java/tw/elliot/cctest/humanresources/HrService.java`

- [ ] **Step 1: Create HrService.java**

```java
package tw.elliot.cctest.humanresources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.department.EmployeeDemotedEvent;
import tw.elliot.cctest.department.EmployeePromotedEvent;
import tw.elliot.cctest.department.EmployeeTransferredEvent;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.api.EmployeeDto;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HrService {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final HrRecordRepository hrRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeDto hire(String name, String email, String phone, UUID departmentId) {
        departmentService.findById(departmentId); // verify department exists
        Employee employee = employeeService.create(name, email, phone, departmentId);
        hrRecordRepository.save(new HrRecord(employee.getId(), ActionType.HIRED,
                "Hired " + name + " into department " + departmentId));
        eventPublisher.publishEvent(new EmployeeHiredEvent(employee.getId(), departmentId, name));
        log.info("Hired employee {} into department {}", employee.getId(), departmentId);
        return EmployeeDto.from(employee);
    }

    public EmployeeDto fire(UUID employeeId) {
        Employee employee = employeeService.fire(employeeId);
        hrRecordRepository.save(new HrRecord(employeeId, ActionType.FIRED,
                "Fired " + employee.getName()));
        eventPublisher.publishEvent(new EmployeeFiredEvent(employeeId, employee.getName()));
        log.info("Fired employee {}", employeeId);
        return EmployeeDto.from(employee);
    }

    @Transactional(readOnly = true)
    public List<HrRecord> findAllRecords() {
        return hrRecordRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<HrRecord> findRecordsByEmployeeId(UUID employeeId) {
        return hrRecordRepository.findByEmployeeId(employeeId);
    }

    @ApplicationModuleListener
    void on(EmployeeTransferredEvent event) {
        log.info("Recording transfer for employee {}", event.employeeId());
        hrRecordRepository.save(new HrRecord(event.employeeId(), ActionType.TRANSFERRED,
                "Transferred from department " + event.fromDepartmentId() + " to " + event.toDepartmentId()));
    }

    @ApplicationModuleListener
    void on(EmployeePromotedEvent event) {
        log.info("Recording promotion for employee {}", event.employeeId());
        hrRecordRepository.save(new HrRecord(event.employeeId(), ActionType.PROMOTED,
                "Promoted from " + event.fromRank() + " to " + event.toRank() + " in department " + event.departmentId()));
    }

    @ApplicationModuleListener
    void on(EmployeeDemotedEvent event) {
        log.info("Recording demotion for employee {}", event.employeeId());
        hrRecordRepository.save(new HrRecord(event.employeeId(), ActionType.DEMOTED,
                "Demoted from " + event.fromRank() + " to " + event.toRank() + " in department " + event.departmentId()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/humanresources/HrService.java
git commit -m "feat: add HrService with hire, fire, and event listeners"
```

---

### Task 12: HR module — HrController

**Files:**
- Create: `src/main/java/tw/elliot/cctest/humanresources/api/HrController.java`

- [ ] **Step 1: Create HrController.java**

```java
package tw.elliot.cctest.humanresources.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tw.elliot.cctest.employee.api.EmployeeDto;
import tw.elliot.cctest.humanresources.HrService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Human Resources", description = "HR management — hire, fire, records")
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    @Operation(summary = "Hire an employee into a department")
    @ApiResponse(responseCode = "201", description = "Employee hired")
    @PostMapping("/employees/hire")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDto hire(@RequestBody HireRequest request) {
        return hrService.hire(request.name(), request.email(), request.phone(), request.departmentId());
    }

    @Operation(summary = "Fire an employee")
    @ApiResponse(responseCode = "200", description = "Employee fired")
    @PostMapping("/employees/{id}/fire")
    public EmployeeDto fire(@PathVariable UUID id) {
        return hrService.fire(id);
    }

    @Operation(summary = "List HR records, optionally filtered by employeeId")
    @GetMapping("/records")
    public List<HrRecordDto> findRecords(@RequestParam(required = false) UUID employeeId) {
        if (employeeId != null) {
            return hrService.findRecordsByEmployeeId(employeeId).stream()
                    .map(HrRecordDto::from)
                    .toList();
        }
        return hrService.findAllRecords().stream()
                .map(HrRecordDto::from)
                .toList();
    }

    public record HireRequest(String name, String email, String phone, UUID departmentId) {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/humanresources/api/HrController.java
git commit -m "feat: add HrController REST API with OpenAPI annotations"
```

---

### Task 13: HR module — unit tests

**Files:**
- Create: `src/test/java/tw/elliot/cctest/humanresources/HrServiceTest.java`

- [ ] **Step 1: Create HrServiceTest.java**

```java
package tw.elliot.cctest.humanresources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tw.elliot.cctest.department.Department;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.department.EmployeePromotedEvent;
import tw.elliot.cctest.department.EmployeeTransferredEvent;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrServiceTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private HrRecordRepository hrRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private HrService hrService;

    private UUID departmentId;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        when(hrRecordRepository.save(any(HrRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void hire_shouldCreateEmployeeAndRecord() {
        var department = new Department("Engineering", "Eng");
        var employee = new Employee("Jane", "jane@test.com", "123", departmentId);

        when(departmentService.findById(departmentId)).thenReturn(department);
        when(employeeService.create("Jane", "jane@test.com", "123", departmentId)).thenReturn(employee);

        var result = hrService.hire("Jane", "jane@test.com", "123", departmentId);

        assertThat(result.name()).isEqualTo("Jane");
        assertThat(result.rank()).isEqualTo(Rank.STAFF);

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.HIRED);

        verify(eventPublisher).publishEvent(any(EmployeeHiredEvent.class));
    }

    @Test
    void fire_shouldSetInactiveAndCreateRecord() {
        var employee = new Employee("Jane", "jane@test.com", null, departmentId);
        employee.setStatus(tw.elliot.cctest.employee.Status.INACTIVE);
        employee.setDepartmentId(null);

        when(employeeService.fire(employee.getId())).thenReturn(employee);

        var result = hrService.fire(employee.getId());

        assertThat(result.status()).isEqualTo(tw.elliot.cctest.employee.Status.INACTIVE);
        assertThat(result.departmentId()).isNull();

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.FIRED);

        verify(eventPublisher).publishEvent(any(EmployeeFiredEvent.class));
    }

    @Test
    void onTransferredEvent_shouldCreateRecord() {
        var event = new EmployeeTransferredEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        hrService.on(event);

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.TRANSFERRED);
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(event.employeeId());
    }

    @Test
    void onPromotedEvent_shouldCreateRecord() {
        var event = new EmployeePromotedEvent(UUID.randomUUID(), departmentId, Rank.STAFF, Rank.TEAM_LEAD);

        hrService.on(event);

        var captor = ArgumentCaptor.forClass(HrRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ActionType.PROMOTED);
        assertThat(captor.getValue().getDetail()).contains("STAFF").contains("TEAM_LEAD");
    }
}
```

- [ ] **Step 2: Run all unit tests**

Run: `mvn test`
Expected: BUILD SUCCESS, all unit tests pass (EmployeeServiceTest + DepartmentServiceTest + HrServiceTest)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/humanresources/HrServiceTest.java
git commit -m "test: add HrService unit tests"
```

---

### Task 14: Testcontainers configuration and update existing test

**Files:**
- Create: `src/test/java/tw/elliot/cctest/TestcontainersConfig.java`
- Modify: `src/test/java/tw/elliot/cctest/CctestApplicationTests.java`

- [ ] **Step 1: Create TestcontainersConfig.java**

```java
package tw.elliot.cctest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:18-alpine");
    }
}
```

- [ ] **Step 2: Update CctestApplicationTests.java**

Replace the entire file:

```java
package tw.elliot.cctest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class CctestApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 3: Run the smoke test**

Run: `mvn test -Dtest=CctestApplicationTests`
Expected: BUILD SUCCESS — context loads with Testcontainers PostgreSQL, Flyway runs migrations

- [ ] **Step 4: Commit**

```bash
git add src/test/java/tw/elliot/cctest/TestcontainersConfig.java src/test/java/tw/elliot/cctest/CctestApplicationTests.java
git commit -m "feat: add Testcontainers config and update smoke test for PostgreSQL"
```

---

### Task 15: Modularity integration test

**Files:**
- Create: `src/test/java/tw/elliot/cctest/ModularityIT.java`

- [ ] **Step 1: Create ModularityIT.java**

```java
package tw.elliot.cctest;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityIT {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(CctestApplication.class);
        modules.verify();
    }
}
```

- [ ] **Step 2: Run the modularity test**

Run: `mvn failsafe:integration-test -Dit.test=ModularityIT`
Expected: BUILD SUCCESS — module boundaries verified

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/ModularityIT.java
git commit -m "test: add modularity verification integration test"
```

---

### Task 16: Department controller integration test

**Files:**
- Create: `src/test/java/tw/elliot/cctest/department/DepartmentControllerIT.java`

- [ ] **Step 1: Create DepartmentControllerIT.java**

```java
package tw.elliot.cctest.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tw.elliot.cctest.TestcontainersConfig;
import tw.elliot.cctest.employee.Employee;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class DepartmentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Test
    void createDepartment_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Engineering", "description": "Engineering department"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void listDepartments_shouldReturnAll() throws Exception {
        departmentService.create("Sales-list", "Sales dept");

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void promoteEmployee_shouldReturnUpdatedRank() throws Exception {
        var dept = departmentService.create("Promo-dept", "For promotion test");
        var employee = employeeService.create("PromoTest", "promo@test.com", null, dept.getId());

        mockMvc.perform(post("/api/departments/{id}/employees/{empId}/promote",
                        dept.getId(), employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value("TEAM_LEAD"));
    }

    @Test
    void demoteEmployee_shouldReturnUpdatedRank() throws Exception {
        var dept = departmentService.create("Demote-dept", "For demotion test");
        var employee = employeeService.create("DemoteTest", "demote@test.com", null, dept.getId());
        employeeService.promote(employee.getId()); // STAFF -> TEAM_LEAD

        mockMvc.perform(post("/api/departments/{id}/employees/{empId}/demote",
                        dept.getId(), employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value("STAFF"));
    }

    @Test
    void transferEmployee_shouldChangeDepartment() throws Exception {
        var deptA = departmentService.create("DeptA-transfer", "Dept A");
        var deptB = departmentService.create("DeptB-transfer", "Dept B");
        var employee = employeeService.create("TransferTest", "transfer@test.com", null, deptA.getId());

        mockMvc.perform(post("/api/departments/{id}/employees/{empId}/transfer",
                        deptB.getId(), employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(deptB.getId().toString()));
    }

    @Test
    void listEmployeesInDepartment_shouldReturnEmployees() throws Exception {
        var dept = departmentService.create("ListEmp-dept", "For listing");
        employeeService.create("EmpInDept", "empindept@test.com", null, dept.getId());

        mockMvc.perform(get("/api/departments/{id}/employees", dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("EmpInDept"));
    }
}
```

- [ ] **Step 2: Run the integration test**

Run: `mvn failsafe:integration-test -Dit.test=DepartmentControllerIT`
Expected: BUILD SUCCESS, 6 tests passed

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/department/DepartmentControllerIT.java
git commit -m "test: add DepartmentController integration tests"
```

---

### Task 17: HR controller integration test

**Files:**
- Create: `src/test/java/tw/elliot/cctest/humanresources/HrControllerIT.java`

- [ ] **Step 1: Create HrControllerIT.java**

```java
package tw.elliot.cctest.humanresources;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tw.elliot.cctest.TestcontainersConfig;
import tw.elliot.cctest.department.DepartmentService;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class HrControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentService departmentService;

    @Test
    void hireEmployee_shouldReturn201() throws Exception {
        var dept = departmentService.create("HR-hire-dept", "For hire test");

        mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "email": "alice-hire@test.com",
                                  "phone": "0912345678",
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.rank").value("STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.departmentId").value(dept.getId().toString()));
    }

    @Test
    void fireEmployee_shouldSetInactive() throws Exception {
        var dept = departmentService.create("HR-fire-dept", "For fire test");

        // Hire first
        var hireResult = mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bob",
                                  "email": "bob-fire@test.com",
                                  "phone": null,
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String employeeId = com.jayway.jsonpath.JsonPath.read(
                hireResult.getResponse().getContentAsString(), "$.id");

        // Fire
        mockMvc.perform(post("/api/hr/employees/{id}/fire", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.departmentId").isEmpty());
    }

    @Test
    void listRecords_shouldReturnHireRecord() throws Exception {
        var dept = departmentService.create("HR-records-dept", "For records test");

        // Hire to create a record
        mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Carol",
                                  "email": "carol-records@test.com",
                                  "phone": null,
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated());

        // List records
        mockMvc.perform(get("/api/hr/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listRecordsByEmployeeId_shouldFilterCorrectly() throws Exception {
        var dept = departmentService.create("HR-filter-dept", "For filter test");

        var hireResult = mockMvc.perform(post("/api/hr/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dave",
                                  "email": "dave-filter@test.com",
                                  "phone": null,
                                  "departmentId": "%s"
                                }
                                """.formatted(dept.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String employeeId = com.jayway.jsonpath.JsonPath.read(
                hireResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/hr/records").param("employeeId", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actionType").value("HIRED"));
    }
}
```

- [ ] **Step 2: Run the integration test**

Run: `mvn failsafe:integration-test -Dit.test=HrControllerIT`
Expected: BUILD SUCCESS, 4 tests passed

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/humanresources/HrControllerIT.java
git commit -m "test: add HrController integration tests"
```

---

### Task 18: Event delivery integration test

**Files:**
- Create: `src/test/java/tw/elliot/cctest/humanresources/EventDeliveryIT.java`

- [ ] **Step 1: Create EventDeliveryIT.java**

```java
package tw.elliot.cctest.humanresources;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tw.elliot.cctest.TestcontainersConfig;
import tw.elliot.cctest.department.DepartmentService;
import tw.elliot.cctest.employee.EmployeeService;
import tw.elliot.cctest.employee.Rank;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfig.class)
class EventDeliveryIT {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private HrService hrService;

    @Autowired
    private HrRecordRepository hrRecordRepository;

    @Test
    void promote_shouldCreateHrRecord() {
        var dept = departmentService.create("Event-promo-dept", "For event test");
        var employee = employeeService.create("EventPromo", "eventpromo@test.com", null, dept.getId());

        departmentService.promote(dept.getId(), employee.getId());

        var records = hrRecordRepository.findByEmployeeId(employee.getId());
        assertThat(records).anyMatch(r ->
                r.getActionType() == ActionType.PROMOTED
                        && r.getDetail().contains(Rank.STAFF.name())
                        && r.getDetail().contains(Rank.TEAM_LEAD.name()));
    }

    @Test
    void transfer_shouldCreateHrRecord() {
        var deptA = departmentService.create("Event-deptA", "Dept A");
        var deptB = departmentService.create("Event-deptB", "Dept B");
        var employee = employeeService.create("EventTransfer", "eventtransfer@test.com", null, deptA.getId());

        departmentService.transfer(deptB.getId(), employee.getId());

        var records = hrRecordRepository.findByEmployeeId(employee.getId());
        assertThat(records).anyMatch(r ->
                r.getActionType() == ActionType.TRANSFERRED
                        && r.getDetail().contains(deptA.getId().toString())
                        && r.getDetail().contains(deptB.getId().toString()));
    }

    @Test
    void demote_shouldCreateHrRecord() {
        var dept = departmentService.create("Event-demote-dept", "For demote event test");
        var employee = employeeService.create("EventDemote", "eventdemote@test.com", null, dept.getId());
        employeeService.promote(employee.getId()); // STAFF -> TEAM_LEAD

        departmentService.demote(dept.getId(), employee.getId());

        var records = hrRecordRepository.findByEmployeeId(employee.getId());
        assertThat(records).anyMatch(r ->
                r.getActionType() == ActionType.DEMOTED
                        && r.getDetail().contains(Rank.TEAM_LEAD.name())
                        && r.getDetail().contains(Rank.STAFF.name()));
    }
}
```

- [ ] **Step 2: Run all integration tests**

Run: `mvn failsafe:integration-test`
Expected: BUILD SUCCESS — ModularityIT, DepartmentControllerIT, HrControllerIT, EventDeliveryIT all pass

- [ ] **Step 3: Run all tests (unit + integration)**

Run: `mvn verify`
Expected: BUILD SUCCESS — all unit tests (surefire) and integration tests (failsafe) pass

- [ ] **Step 4: Commit**

```bash
git add src/test/java/tw/elliot/cctest/humanresources/EventDeliveryIT.java
git commit -m "test: add event delivery integration test"
```
