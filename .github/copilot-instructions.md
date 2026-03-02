# AI Copilot Instructions - idpay-wallet (tailored)

This repository-specific guidance is for AI agents (Copilot-style assistants) working on `idpay-wallet`. It codifies the project's architecture, coding conventions, and enforced toolchain constraints (Java 25, Spring Boot 4, Jackson 3).

## Project Context & Architecture
- Domain: IDPay — Wallet microservice.
- Primary stack: Java 25, Spring Boot 4.x, Jackson 3, MongoDB, Kafka, Azure Blob Storage (where used).
- Architecture: Layered Architecture (Controller → Service → Repository/Connector) with Hexagonal/adapter influences: connectors and `connector/` packages act as adapters to external systems. Treat boundaries as ports (interfaces) + adapters (impls).

Repository mapping (common locations):
- Business logic: `src/main/java/.../service/`
- REST controllers: `src/main/java/.../controller/`
- Persistence / repositories: `src/main/java/.../repository/`
- External adapters / REST clients: `src/main/java/.../connector/`
- Config: `src/main/java/.../config/`

If the codebase contains `*Service` interfaces and `*ServiceImpl` implementations, prefer adding behavior in the `Impl` and keep interfaces as stable contracts.

## Architectural Rules
1. Interface-First: Keep clear interface ↔ implementation separation at primary boundaries (controllers, services, repositories, connectors). Treat interfaces as stable ports; implementations are adapters.
2. Dependency Injection: Use constructor injection only. Annotate implementation classes with `@Service`, `@Component`, or `@RestController` as appropriate.
3. No unnecessary refactor of package structure: preserve existing boundaries unless a clear improvement is proposed and tested.

## Tech-Stack Enforcement (mandatory)
- Java: Projects use Java 25. Do NOT suggest or use Java 17 or 21-only syntax or language features. Prefer Java 25 features where appropriate (see Performance section).
- Spring: Use Spring Boot 4.x starters and dependencies only. Do NOT add or suggest Spring Boot 3.x specific starters, BOMs, or versioned dependencies.
- Jackson: Enforce Jackson 3 usage. All JSON mapping must use `JsonMapper.builder()` style configuration (examples below). Use `com.fasterxml.jackson.databind.json.JsonMapper` and never the deprecated `ObjectMapper` constructors.
- Jakarta: Use `jakarta.*` (Jakarta EE) namespaces throughout (e.g., `jakarta.validation`, `jakarta.ws.rs` if present). Do NOT use `javax.*`.

Forbidden suggestions (agents must not propose):
- Downgrading to or recommending Java 17/21.
- Suggesting Spring Boot 3-specific artifacts or starters.
- Using legacy Jackson 2 configuration or `new ObjectMapper()` patterns.

## Jackson 3 Protocol and Example
- Always configure mapper via `JsonMapper.builder()` and register modules explicitly (e.g., `JavaTimeModule`, `ParameterNamesModule`) via builder.
- Example pattern to follow project-wide:

```java
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JacksonConfig {
    public static final JsonMapper MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .findAndAddModules() // optional, prefer explicit registration
        .build();
}
```

- For Spring configuration, expose a `@Bean` returning `JsonMapper` or configure `Jackson2ObjectMapperBuilder` to produce Jackson 3 `JsonMapper` when required by Spring Boot 4 autoconfig.

## JSON / Serialization Rules
- Use Jackson 3 `JsonMapper` for all serialization and deserialization.
- Prefer explicit DTO classes; avoid ad-hoc Map-based JSON unless absolutely necessary.
- Adhere to Jakarta JSON namespaces where applicable.

## Language & Style Guidelines
- Use Java 25 idioms when appropriate: pattern matching enhancements, record improvements, scoped type inference improvements, and structured concurrency APIs for short-lived concurrency tasks.
- Prefer self-documenting code: expressive method and variable names, small functions, and coherent types. Minimize Javadoc; reserve Javadoc for Public APIs that are consumed across modules or by external teams.
- Signal-to-Noise rule for comments:
  - Keep inline comments to < 15 words and focused on intent, not on what the code does.
  - Preface design-decisions with `[DECISION]` and TODOs with `[TODO]` followed by a concise rationale.
  - Long explanations belong in design docs, not as inline code comments.

## Reactive vs Virtual Threads (Performance)
- This repository contains adapters (connectors) to external systems. Choose the right concurrency model per module:
  - If the module is reactive (WebFlux, Reactor-based), keep it fully non-blocking. Do not introduce blocking calls or thread-per-request constructs into reactive flows. Avoid `.block()` in library code.
  - If the module is Servlet/blocking (thread-per-request), prefer Java 25 Virtual Threads (Project Loom) for high-concurrency I/O-bound workloads. Use structured concurrency (`java.util.concurrent.StructuredTaskScope`) for fault isolation and lifecycle control when spawning short-lived tasks.

Guideline: Prefer non-blocking/reactive for streaming pipelines; for legacy blocking code or synchronous adapters, prefer virtual threads instead of a large fixed thread pool.

## Testing & TDD
- Tests are required for new behavior. Use JUnit 5 + Mockito (or the project's existing test patterns).
- For reactive code use `StepVerifier` for `Flux`/`Mono` assertions.
- For virtual-thread-related concurrency tests, use deterministic scopes and timeouts; avoid tests that rely on timing assumptions.

## Logging & Security
- Sanitize any user-originated strings before logging. Use centralized sanitizers if present (e.g., `Utilities.sanitizeString(...)`).
- Avoid logging sensitive values (PII, tokens). If logging is necessary for debugging, ensure redact logic and feature toggle exist.

## Error Handling
- Surface typed exceptions for API boundaries. Use the project's existing exception types where applicable.
- Include error codes/messages in a central `ExceptionConstants` or equivalent.

## CI / Linting / Formatting
- Preserve project's existing formatting rules. If none exist, prefer `spotless` or `google-java-format` configured in the shared Maven build.
- Add linters or static analyzers only if they are compatible with Java 25 and Spring Boot 4.

## Pull Request Guidance for AI-generated changes
- Small, focused diffs preferred: 1 feature/fix per PR.
- Include unit tests that demonstrate the behavior change.
- Document any migration steps (e.g., Jackson 2 → Jackson 3 differences) in the PR description.

## Quick Examples
- Structured concurrency (Java 25) minimal pattern:

```java
try (var scope = new java.util.concurrent.StructuredTaskScope.ShutdownOnFailure()) {
    var future = scope.fork(() -> blockingCallOnVirtualThread());
    scope.join();
    scope.throwIfFailed();
    var result = future.resultNow();
}
```

- Jackson 3 builder usage (recommended): see `JacksonConfig` snippet above.

## Code Style Enforcement:

Strictly follow the .editorconfig defined in the root.

Use Google Java Style (2-space indents).

For Java 25 features: Ensure Record components and Case patterns in switch expressions are formatted for maximum readability (split lines for >3 components).

Import Ordering: Group jakarta.* imports above third-party libraries but below java.*.

## When to Ask the Human
- Complex architecture changes (package splits, adapter rework) require an explicit design decision from the maintainers.
- When introducing non-backward-compatible changes to serialization formats, request release coordination.

## Final Notes
- This file is authoritative for AI assistants working in this repository. If you propose deviations, include a very short rationale and tests that validate the change.

--
Repository assistant: follow these rules strictly and prefer small, test-backed changes.
