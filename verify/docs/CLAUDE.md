# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Guidelines

### Rules

1. **Don't analyze `docs/` directory** unless explicitly requested by the user
2. **For Java library versions**, always check `central.sonatype.com` with `sort=v+desc` parameter, not `search.maven.org`
3. **For Spring Modulith design**, refer to `docs/references/spring-modulith-principles.md` and inform the user

### Testing Strategy

- **Unit Tests**: Test domain logic (Rank/Status changes, entity behavior)
- **Integration Tests**: Use `@SpringBootTest` and File name end with *IT*
- **Modularity Tests**: Verify module structure with `ApplicationModules.verify()`
- Test files should be placed in corresponding packages under `src/test/java/`
