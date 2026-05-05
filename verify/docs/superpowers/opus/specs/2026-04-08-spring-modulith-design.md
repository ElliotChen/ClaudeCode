# Spring Modulith Feature Design

## Overview

Add Spring Modulith 2.0.3 to the existing Spring Boot 4.0.3 project with three domain modules: Employee, Department, and HumanResources. Modules communicate via direct calls (synchronous) and JDBC-backed events (async notifications). PostgreSQL 18 with Flyway for schema management, Testcontainers for integration testing.

## Dependencies to Add

| Dependency | Scope | Notes |
|---|---|---|
| `spring-modulith-bom:2.0.3` | dependencyManagement | BOM for all Modulith deps |
| `spring-modulith-starter-core` | compile | Core module support |
| `spring-modulith-starter-jdbc` | compile | JDBC event publication |
| `spring-modulith-starter-test` | test | Module verification |
| `spring-boot-starter-data-jpa` | compile | JPA/Hibernate |
| `org.postgresql:postgresql` | runtime | PostgreSQL driver |
| `org.flywaydb:flyway-core` | compile | Schema migration |
| `org.flywaydb:flyway-database-postgresql` | runtime | Flyway PostgreSQL support |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2` | compile | Swagger UI + OpenAPI |
| `testcontainers-bom:2.0.3` | dependencyManagement (test) | Testcontainers BOM |
| `org.testcontainers:postgresql` | test | PostgreSQL container |
| `org.testcontainers:junit-jupiter` | test | JUnit 5 integration |

**Plugins to add:**
- `maven-failsafe-plugin` for `*IT.java` integration tests

## Module Structure

```
tw.elliot.cctest/
├── employee/
│   ├── Employee.java
│   ├── Rank.java
│   ├── Status.java
│   ├── EmployeeRepository.java
│   ├── EmployeeService.java
│   ├── package-info.java             # @ApplicationModule(allowedDependencies = {})
│   └── api/
│       └── EmployeeDto.java
│
├── department/
│   ├── Department.java
│   ├── DepartmentRepository.java
│   ├── DepartmentService.java
│   ├── package-info.java             # @ApplicationModule(allowedDependencies = {"employee"})
│   └── api/
│       ├── DepartmentController.java
│       └── DepartmentDto.java
│
├── humanresources/
│   ├── HrRecord.java
│   ├── HrRecordRepository.java
│   ├── HrService.java
│   ├── package-info.java             # @ApplicationModule(allowedDependencies = {"employee", "department"})
│   └── api/
│       ├── HrController.java
│       └── HrDto.java
│
├── config/
│   └── TraceIdFilter.java            # (existing)
├── ctrl/
│   └── HelloController.java          # (existing)
└── CctestApplication.java
```

### Module Dependencies

- **Employee** → no dependencies. Standalone module owning Employee entity.
- **Department** → Employee. Calls EmployeeService for rank/status/dept mutations. Publishes events for HR notification.
- **HumanResources** → Employee + Department. Calls EmployeeService for hire/fire. Listens to Department events for record-keeping.

## Entity Details

### Employee

```
id: UUID (V7, generated via JDK 25 UUID.ofEpochMillis())
name: String (not null)
email: String (not null, unique)
phone: String (nullable)
rank: Rank enum (STAFF, TEAM_LEAD, MANAGER) — default STAFF
status: Status enum (ACTIVE, INACTIVE) — default ACTIVE
departmentId: UUID (nullable — null when INACTIVE)
hireDate: LocalDate
```

JPA table: `employees`

### Department

```
id: UUID (V7)
name: String (not null, unique)
description: String (nullable)
```

JPA table: `departments`

### HrRecord

```
id: UUID (V7)
employeeId: UUID (not null)
actionType: ActionType enum (HIRED, FIRED, PROMOTED, DEMOTED, TRANSFERRED)
detail: String (human-readable, e.g., "Promoted from STAFF to TEAM_LEAD in Engineering")
occurredAt: LocalDateTime
```

JPA table: `hr_records`

## Business Rules

### EmployeeService (enforces core domain rules):
1. **INACTIVE constraint:** INACTIVE employees must have `departmentId = null`.
2. **One manager per department:** At most one employee with `rank = MANAGER` per department. Checked on promote and transfer.
3. **Rank transitions — promote:** STAFF → TEAM_LEAD → MANAGER. Cannot promote beyond MANAGER.
4. **Rank transitions — demote:** MANAGER → TEAM_LEAD → STAFF. Cannot demote below STAFF.
5. **Transfer precondition:** Employee must be ACTIVE to transfer.

### HrService:
6. **Hire:** Creates Employee with status=ACTIVE, rank=STAFF, assigned to specified department. Department must exist.
7. **Fire:** Sets status to INACTIVE, clears departmentId. Keeps current rank.

### DepartmentService:
8. **Transfer:** Changes employee's departmentId. Delegates to EmployeeService for validation (rule 5, rule 2 if employee is MANAGER).
9. **Promote:** Increments rank by one level. Delegates to EmployeeService for validation (rules 2, 3).
10. **Demote:** Decrements rank by one level. Delegates to EmployeeService for validation (rule 4).

## Event Flow

**JDBC-backed event publication** via `spring-modulith-starter-jdbc`.

### Events published by DepartmentService → listened by HrService:

| Event | Payload | Trigger |
|---|---|---|
| `EmployeeTransferredEvent` | employeeId, fromDepartmentId, toDepartmentId | After successful transfer |
| `EmployeePromotedEvent` | employeeId, departmentId, fromRank, toRank | After successful promotion |
| `EmployeeDemotedEvent` | employeeId, departmentId, fromRank, toRank | After successful demotion |

### Events published by HrService (for audit, no listener required yet):

| Event | Payload | Trigger |
|---|---|---|
| `EmployeeHiredEvent` | employeeId, departmentId, name | After successful hire |
| `EmployeeFiredEvent` | employeeId, name | After successful fire |

**Listener:** HrService uses `@ApplicationModuleListener` to listen for Department events and creates HrRecord entries for each.

## REST API

### HR Controller (`/api/hr`)

| Method | Path | Description | Request Body |
|---|---|---|---|
| POST | `/api/hr/employees/hire` | Hire employee into department | `{ name, email, phone, departmentId }` |
| POST | `/api/hr/employees/{id}/fire` | Fire employee | — |
| GET | `/api/hr/records` | List all HR records | — |
| GET | `/api/hr/records?employeeId={id}` | HR records for an employee | — |

### Department Controller (`/api/departments`)

| Method | Path | Description | Request Body |
|---|---|---|---|
| POST | `/api/departments` | Create department | `{ name, description }` |
| GET | `/api/departments` | List all departments | — |
| GET | `/api/departments/{id}` | Get department detail | — |
| GET | `/api/departments/{id}/employees` | List employees in department | — |
| POST | `/api/departments/{id}/employees/{employeeId}/transfer` | Transfer employee to this dept | — |
| POST | `/api/departments/{id}/employees/{employeeId}/promote` | Promote employee | — |
| POST | `/api/departments/{id}/employees/{employeeId}/demote` | Demote employee | — |

All endpoints annotated with `@Operation` and `@ApiResponse` for OpenAPI docs. SpringDoc generates Swagger UI at `/swagger-ui.html`.

## Database

### PostgreSQL 18
- Docker image: `postgres:18-alpine`
- `application.yaml` config for datasource, JPA (ddl-auto: validate), Flyway

### Flyway Migrations (`src/main/resources/db/migration/`)

| File | Content |
|---|---|
| `V1__create_department_table.sql` | departments table |
| `V2__create_employee_table.sql` | employees table with FK to departments (nullable) |
| `V3__create_hr_record_table.sql` | hr_records table |
| `V4__create_event_publication_table.sql` | Spring Modulith event publication table |

UUID columns use PostgreSQL's native `uuid` type. UUID V7 values generated in Java via `UUID.ofEpochMillis(System.currentTimeMillis())`.

## Testing

### Test Split

| Type | Pattern | Runner | What it tests |
|---|---|---|---|
| Unit | `*Test.java` | maven-surefire-plugin | Domain logic: rank transitions, status rules, business validations. No Spring context. Mockito. |
| Integration | `*IT.java` | maven-failsafe-plugin | Full Spring context + Testcontainers PostgreSQL. API, events, DB. |

### Unit Tests

- `EmployeeServiceTest.java` — rank transition logic, status constraints, one-manager-per-dept validation
- `DepartmentServiceTest.java` — transfer/promote/demote delegation and event publishing
- `HrServiceTest.java` — hire/fire logic

### Integration Tests

- `ModularityIT.java` — `ApplicationModules.of(CctestApplication.class).verify()`
- `HrControllerIT.java` — hire/fire API endpoints, response codes, HR record creation
- `DepartmentControllerIT.java` — CRUD, transfer/promote/demote API endpoints, response codes
- `EventDeliveryIT.java` — verify Department events trigger HrRecord creation via JDBC event publication

### Testcontainers Setup

- `testcontainers-bom:2.0.3` in `<dependencyManagement>`
- `testcontainers-postgresql` + `testcontainers-junit-jupiter` (scope: test)
- Shared PostgreSQL container via `@ServiceConnection` and `@Container` in a base test class or test configuration
- Flyway runs automatically on container startup

## Design Decisions

1. **Employee module has no dependencies** — it is the core domain. Department and HR depend on it, not the reverse.
2. **Direct calls for synchronous operations** — HR→Employee for hire/fire, Department→Employee for mutations. These need immediate feedback (success/failure).
3. **Events for async notifications only** — Department→HR notifications for record-keeping. JDBC publication ensures reliable delivery even if HR listener fails temporarily.
4. **Rank transitions are one-step** — promote/demote moves exactly one level. This keeps the logic simple and auditable.
5. **Fire keeps rank** — a fired employee retains their rank in case of re-hire scenarios (though re-hire is not in scope).
6. **UUID V7 via JDK 25** — no external library needed. `UUID.ofEpochMillis()` provides time-ordered UUIDs for better database index performance.
7. **Flyway over JPA auto-DDL** — explicit schema control, repeatable deployments, matches production practices.
