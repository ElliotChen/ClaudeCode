
When running documentation generator skills (architecture-doc-generator, entity-doc-generator, event-doc-generator, schedule-doc-generator, dfd-analyzer), prioritize writing output files incrementally rather than exploring the entire codebase first. Write each doc file as soon as its analysis is complete to avoid losing progress if rate-limited or interrupted.

This is a Spring Boot DDD (Domain-Driven Design) project using Java. Key patterns: bounded contexts, domain events, @Entity classes, application.yml for configuration, YAML-based topic definitions. Always check application*.yml for configuration values like topic names before generating docs.

When generating documentation, minimize codebase exploration time. Do NOT spend more than 2-3 tool calls on initial exploration before beginning to produce output. Use targeted Grep/Glob for specific patterns (e.g., @Entity, @DomainEvent, @EventListener) rather than broad directory traversal.

Output format for documentation: Use Obsidian-compatible markdown with Mermaid diagrams. Include YAML frontmatter where appropriate. Place docs in the docs/ subdirectory organized by type (docs/architecture/, docs/event/, docs/entity/, docs/dfd/, docs/schedule/).