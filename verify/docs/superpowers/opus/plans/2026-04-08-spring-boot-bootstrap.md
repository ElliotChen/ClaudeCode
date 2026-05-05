# Spring Boot 4.0.3 Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a working Spring Boot 4.0.3 project with Log4j2, OpenTelemetry tracing, actuator, a hello controller, and traceId response headers.

**Architecture:** Flat package structure under `tw.elliot.cctest` with `config/` for infrastructure (filter) and `ctrl/` for REST controllers. Logging uses Log4j2 with MDC-based traceId/spanId. A servlet filter adds traceId to all response headers.

**Tech Stack:** Spring Boot 4.0.3, JDK 25, Maven, Lombok 1.18.44, Log4j2, OpenTelemetry (via spring-boot-starter-opentelemetry), Micrometer Tracing

---

## File Structure

| File | Responsibility |
|---|---|
| `pom.xml` | Maven build, dependencies, compiler plugin config |
| `src/main/resources/application.yaml` | App config: actuator, tracing, OTLP settings |
| `src/main/resources/log4j2-spring.xml` | Log4j2 console appender with traceId/spanId in pattern |
| `src/main/java/tw/elliot/cctest/CctestApplication.java` | Spring Boot main class |
| `src/main/java/tw/elliot/cctest/config/TraceIdFilter.java` | Servlet filter adding X-Trace-Id header |
| `src/main/java/tw/elliot/cctest/ctrl/HelloController.java` | GET /ctrl/hello endpoint |
| `src/test/java/tw/elliot/cctest/CctestApplicationTests.java` | Context loads smoke test |

---

### Task 1: Create Maven pom.xml

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: Create pom.xml with all dependencies and plugin config**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.3</version>
        <relativePath/>
    </parent>

    <groupId>tw.elliot</groupId>
    <artifactId>cctest</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>cctest</name>
    <description>Spring Boot bootstrap project</description>

    <properties>
        <java.version>25</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-opentelemetry</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.44</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.44</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Verify Maven resolves dependencies**

Run: `./mvnw dependency:resolve` (or `mvn dependency:resolve` if no wrapper yet)
Expected: BUILD SUCCESS with all dependencies downloaded

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add Maven pom.xml with Spring Boot 4.0.3 dependencies"
```

---

### Task 2: Create resource files (application.yaml and log4j2-spring.xml)

**Files:**
- Create: `src/main/resources/application.yaml`
- Create: `src/main/resources/log4j2-spring.xml`

- [ ] **Step 1: Create application.yaml**

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

- [ ] **Step 2: Create log4j2-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] [%X{traceId}-%X{spanId}] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yaml src/main/resources/log4j2-spring.xml
git commit -m "feat: add application.yaml and log4j2-spring.xml with tracing config"
```

---

### Task 3: Create Spring Boot main application class

**Files:**
- Create: `src/main/java/tw/elliot/cctest/CctestApplication.java`

- [ ] **Step 1: Create CctestApplication.java**

```java
package tw.elliot.cctest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CctestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CctestApplication.class, args);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/CctestApplication.java
git commit -m "feat: add Spring Boot application main class"
```

---

### Task 4: Create TraceIdFilter

**Files:**
- Create: `src/main/java/tw/elliot/cctest/config/TraceIdFilter.java`

- [ ] **Step 1: Create TraceIdFilter.java**

```java
package tw.elliot.cctest.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TraceIdFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            String traceId = currentSpan.context().traceId();
            response.setHeader("X-Trace-Id", traceId);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/config/TraceIdFilter.java
git commit -m "feat: add TraceIdFilter for X-Trace-Id response header"
```

---

### Task 5: Create HelloController

**Files:**
- Create: `src/main/java/tw/elliot/cctest/ctrl/HelloController.java`

- [ ] **Step 1: Create HelloController.java**

```java
package tw.elliot.cctest.ctrl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ctrl")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        log.info("Received hello request");
        return "Hello, World!";
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tw/elliot/cctest/ctrl/HelloController.java
git commit -m "feat: add HelloController with GET /ctrl/hello endpoint"
```

---

### Task 6: Create smoke test and verify

**Files:**
- Create: `src/test/java/tw/elliot/cctest/CctestApplicationTests.java`

- [ ] **Step 1: Create CctestApplicationTests.java**

```java
package tw.elliot.cctest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CctestApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run the test**

Run: `mvn test -Dtest=CctestApplicationTests`
Expected: BUILD SUCCESS, 1 test passed

- [ ] **Step 3: Commit**

```bash
git add src/test/java/tw/elliot/cctest/CctestApplicationTests.java
git commit -m "test: add context loads smoke test"
```

---

### Task 7: Manual verification

- [ ] **Step 1: Start the application**

Run: `mvn spring-boot:run`
Expected: Application starts on port 8080. Console logs show the log4j2 pattern with traceId/spanId placeholders.

- [ ] **Step 2: Test the hello endpoint**

Run: `curl -v http://localhost:8080/ctrl/hello`
Expected:
- Response body: `Hello, World!`
- Response header `X-Trace-Id` is present with a 32-character hex trace ID
- Application log shows: `[<traceId>-<spanId>] INFO  tw.elliot.cctest.ctrl.HelloController - Received hello request`

- [ ] **Step 3: Test actuator endpoints**

Run: `curl http://localhost:8080/actuator`
Expected: JSON listing all available actuator endpoints

- [ ] **Step 4: Stop the application and commit any fixes if needed**
