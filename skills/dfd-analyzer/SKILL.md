---
name: dfd-analyzer
description: "Analyze Java/Spring Boot projects to generate Data Flow Diagrams (DFD). Use when the user requests: (1) DFD generation from code, (2) Architecture visualization for Spring Boot applications, (3) Understanding data flows in microservices, (4) Documentation of system architecture with diagrams, or (5) Analysis of how data moves through controllers, services, and repositories. Supports Level 0 (context), Level 1 (high-level), and Level 2 (detailed) DFD generation with Mermaid syntax."
---

# DFD Analyzer

Generate comprehensive Data Flow Diagrams (DFD) from Java/Spring Boot projects to visualize architecture, understand data flows, and document system design.

## Overview

This skill analyzes Spring Boot project structure and generates DFD at multiple levels:
- **Level 0 (Context Diagram)**: System boundary with external entities
- **Level 1 (High-Level DFD)**: Major components (controllers, services, repositories)
- **Level 2 (Detailed DFD)**: Fine-grained processes and data flows

## Quick Start

```bash
# 1. Analyze project structure
python scripts/analyze_project.py /path/to/spring-boot-project > analysis.json

# 2. Generate detailed DFD (Level 1/2)
python scripts/generate_mermaid.py analysis.json > dfd.mmd

# 3. Generate context diagram (Level 0)
python scripts/generate_mermaid.py analysis.json --context > context.mmd
```

## Workflow

### Step 1: Identify Project Structure

Locate the Spring Boot project root directory. The analyzer works best with:
- Standard Maven/Gradle structure (`src/main/java`)
- Spring Boot annotations (`@RestController`, `@Service`, `@Repository`, `@Entity`)
- Clear package organization

### Step 2: Run Project Analysis

Execute the analyzer to extract DFD components:

```bash
python scripts/analyze_project.py /path/to/project > analysis.json
```

**What gets detected:**

- **External Entities**: `@RestController`, `@FeignClient`, Kafka consumers
- **Processes**: `@Service`, `@Component`, `@Repository` classes
- **Data Stores**: `@Entity` classes, `@Cacheable` methods
- **Data Flows**: Autowired dependencies, method calls

The output is JSON containing all identified components.

### Step 3: Generate DFD Diagrams

**For detailed component-level DFD (Level 1/2):**
```bash
python scripts/generate_mermaid.py analysis.json > dfd.mmd
```

**For system context diagram (Level 0):**
```bash
python scripts/generate_mermaid.py analysis.json --context > context.mmd
```

### Step 4: Create Documentation

Create a Markdown artifact containing:
1. System overview
2. Context diagram (Level 0)
3. High-level DFD (Level 1)
4. Component descriptions
5. Key data flows explanation

## Understanding DFD Components

### External Entities (外部實體)

Sources or destinations of data outside the system. In Spring Boot:
- API clients calling REST endpoints
- External microservices (Feign clients)
- Message queues (Kafka, RabbitMQ)

**Diagram representation**: Double brackets `[[ ]]`

### Processes (處理程序)

Components that transform data:
- Controllers: Handle HTTP requests
- Services: Business logic
- Repositories: Data access

**Diagram representation**: Rectangles `[ ]`

### Data Stores (資料儲存)

Where data is stored:
- Databases: JPA entities
- Caches: `@Cacheable` annotations

**Diagram representation**: Cylinders `[(( ))]`

### Data Flows (資料流)

Movement of data between components via arrows `-->` with labels.

## Reference Materials

### Spring Boot Patterns Guide

Load `references/spring_boot_patterns.md` when:
- Mapping Spring annotations to DFD components
- Understanding different architecture patterns
- Identifying appropriate DFD levels

### DFD Examples and Best Practices

Load `references/dfd_examples.md` when:
- Creating comprehensive DFD
- Need examples of well-structured diagrams
- Learning naming conventions

## Scripts

### analyze_project.py

Scans Java project and identifies DFD components.

**Usage:** `python scripts/analyze_project.py <project-path>`

### generate_mermaid.py

Converts analysis JSON to Mermaid diagram.

**Usage:** `python scripts/generate_mermaid.py <analysis.json> [--context]`

## Tips for Best Results

1. **Organize Code Well**: Clear package structure improves accuracy
2. **Standard Annotations**: Use Spring Boot standard annotations
3. **Iterative Process**: Review output and refine
4. **Multiple Levels**: Start with Level 0, drill down as needed

## When to Use Each Level

**Level 0**: Stakeholder presentations, system boundary
**Level 1**: Developer onboarding, architecture reviews
**Level 2**: Deep technical analysis, debugging
