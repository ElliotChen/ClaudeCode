---
name: event-doc-generator
description: >
  Analyze Spring Boot + DDD + Axon Framework + Saga projects to generate comprehensive event documentation in Traditional Chinese.
  Use when the user wants to document, catalog, or understand events in an Axon-based project.
  Triggers include: requests to generate event docs, analyze Axon events, document domain events,
  create event catalogs, or produce event-driven architecture documentation for Axon/Saga projects.
  Outputs individual markdown files per event plus a README summary, all under docs/event/.
---

# Axon Event Documentation Generator

Generate per-event documentation files in Traditional Chinese for Spring Boot + DDD + Axon + Saga projects.

## Workflow

1. **Scan** the project for all event classes
2. **Classify** each event as Inbound or Outbound
3. **Generate** individual event doc files
4. **Generate** README.md summary

## Step 1: Scan Project for Events

Recursively search the project source tree for event classes. Use these heuristics:

- Classes in packages matching `**/event/**`, `**/events/**`, `**/domain/event/**`
- Classes with names ending in `Event` (e.g., `OrderCreatedEvent`, `PaymentCompletedEvent`)
- Classes annotated with Axon-related annotations or referenced in `@EventHandler`, `@SagaEventHandler`, `@EventSourcingHandler`
- Classes used in `AggregateLifecycle.apply()`, `EventGateway.publish()`, `SagaLifecycle.associateWith()`

Use bash to scan:

```bash
# Find candidate event files
find <project-root>/src -type f -name "*.java" -o -name "*.kt" | xargs grep -l "Event\b" | head -200

# Find event handler references to discover events
grep -rn "@EventHandler\|@SagaEventHandler\|@EventSourcingHandler\|AggregateLifecycle.apply\|EventGateway.publish" <project-root>/src --include="*.java" --include="*.kt"
```

Read each discovered event class to extract: class name, package, fields (with types), and any Javadoc/KDoc.

## Step 2: Classify Events

Determine direction for each event based on context:

**Inbound（入站事件）**: Events consumed/handled by this service.
- Found in `@EventHandler`, `@SagaEventHandler`, `@EventSourcingHandler` methods
- Events from external services arriving via message broker

**Outbound（出站事件）**: Events published/emitted by this service.
- Published via `AggregateLifecycle.apply()` within Aggregates
- Published via `EventGateway.publish()` or `CommandGateway` side effects
- Events dispatched to external services

An event can be BOTH Inbound and Outbound if the same service publishes and consumes it (common in Event Sourcing with Axon). Mark these as `雙向（Inbound / Outbound）`.

## Step 3: Generate Individual Event Files

Create `docs/event/` directory. For each event, generate a markdown file named `<EventClassName>.md`.

Follow the template in [references/event-doc-template.md](references/event-doc-template.md).

Key rules:
- All content in **繁體中文**
- File name uses the original Java/Kotlin class name (English)
- Include all fields with types and descriptions
- Include Axon manual dispatch examples using `EventGateway` or `GenericEventMessage`
- Include the Aggregate or Saga context where the event participates

## Step 4: Generate README.md

Create `docs/event/README.md` summarizing all events. Structure:

```markdown
# 事件文件總覽

> 本文件由工具自動產生，記錄專案中所有 Axon Event 的摘要資訊。

## 統計

| 類別 | 數量 |
|------|------|
| 入站事件 (Inbound) | N |
| 出站事件 (Outbound) | N |
| 雙向事件 (Both) | N |
| **合計** | **N** |

## 事件清單

| 事件名稱 | 方向 | 所屬 Bounded Context | 說明 | 文件連結 |
|----------|------|---------------------|------|----------|
| OrderCreatedEvent | 出站 | Order | 訂單建立時發布 | [查看](./OrderCreatedEvent.md) |
...
```

## Important Notes

- If event direction cannot be determined from code analysis alone, mark as `⚠️ 待確認` and add a note asking the developer to verify.
- For Saga events, document which Saga consumes/produces them and the association property.
- Preserve the original field names from the source code; add Chinese descriptions alongside.
- When generating manual dispatch examples, use the actual field types and provide realistic sample values.
