# Spring Boot 4.0.3 Bootstrap Project Design

## Overview

Create a Spring Boot 4.0.3 project from scratch in the `mlxtest` repository. The project serves as a foundation for future Spring Modulith / DDD development, starting with a single controller and essential infrastructure.

## Project Coordinates

- **Group**: `tw.elliot`
- **Artifact**: `cctest`
- **Java**: JDK 25
- **Build tool**: Maven
- **Spring Boot**: 4.0.3

## Dependencies

| Dependency | Notes |
|---|---|
| `spring-boot-starter` | Exclude `spring-boot-starter-logging` here (single exclusion point) |
| `spring-boot-starter-log4j2` | Replaces Logback |
| `spring-boot-starter-web` | Web/MVC support |
| `spring-boot-starter-actuator` | All endpoints exposed |
| `spring-boot-starter-opentelemetry` | Tracing support |
| `lombok` | Scope: provided |

### Maven Compiler Plugin

Configure `maven-compiler-plugin` with `annotationProcessorPaths` for Lombok to ensure annotation processing works correctly.

## Source Layout

```
src/
├── main/
│   ├── java/tw/elliot/cctest/
│   │   ├── CctestApplication.java
│   │   ├── config/
│   │   │   └── TraceIdFilter.java
│   │   └── ctrl/
│   │       └── HelloController.java
│   └── resources/
│       ├── application.yaml
│       └── log4j2-spring.xml
└── test/
    └── java/tw/elliot/cctest/
        └── CctestApplicationTests.java
```

## Configuration

### application.yaml

```yaml
spring:
  application:
    name: cctest

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
      export: enabled
    metrics:
      export:
        enabled: false
```

- All actuator endpoints exposed via `include: "*"`
- 100% trace sampling
- OTLP metrics export disabled; tracing export enabled but no endpoint configured (traces appear in logs only)
- Uses Spring Boot's `management.tracing.*` and `management.otlp.*` properties, NOT `otel.*`

### log4j2-spring.xml

- Console appender
- Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] [%X{traceId}-%X{spanId}] %-5level %logger{36} - %msg%n`
- TraceId and SpanId populated via MDC by the Micrometer/OpenTelemetry bridge
- Root logger level: INFO

## Java Components

### CctestApplication.java

Standard `@SpringBootApplication` main class.

### HelloController.java

- Package: `tw.elliot.cctest.ctrl`
- `@RestController` with `@RequestMapping("/ctrl")`
- `GET /ctrl/hello` returns `"Hello, World!"`
- Uses Lombok `@Slf4j` for logging
- Logs incoming request at INFO level

### TraceIdFilter.java

- Package: `tw.elliot.cctest.config`
- Extends `OncePerRequestFilter`, annotated with `@Component`
- Injects Micrometer's `Tracer` via constructor
- After filter chain executes, reads current traceId from the active span
- Adds `X-Trace-Id` response header with the traceId value
- If no active trace exists, the header is omitted

### CctestApplicationTests.java

- `@SpringBootTest` smoke test with `contextLoads()` method

## Design Decisions

1. **Single exclusion point for Logback**: `spring-boot-starter-logging` is excluded only from `spring-boot-starter`, avoiding repeated exclusions across multiple starters.
2. **Servlet Filter for traceId header**: `OncePerRequestFilter` ensures the traceId is added to every HTTP response globally, including error responses and actuator endpoints — broader coverage than an interceptor.
3. **Flat package structure**: Simple `config/` and `ctrl/` packages under the base package. Appropriate for bootstrap phase; can evolve into Spring Modulith modules later.
4. **log4j2-spring.xml**: Using the `-spring` suffix enables Spring Boot's log4j2 extensions and proper profile-aware logging configuration.
