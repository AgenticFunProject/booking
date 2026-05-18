# Booking Service

Cargo booking microservice built with Spring Boot 3.5.x and Java 21. Accepts booking requests and manages a shipment lifecycle (PENDING → CONFIRMED → IN_PROGRESS → COMPLETED / CANCELLED). Messaging/event streaming is out of scope for v1.

## Specifications

This project is built from sequential specification files. Before writing code, read this file, then the target spec and every file listed in that spec's `# Depends on:` header.

Before starting an implementation bead, also read `IMPLEMENTATION.md` for the bead workflow, verification expectations, delivery evidence requirements, and dispatch order.

```
specs/001_project_setup.md      → Maven project, dependencies, packages, conventions
specs/002_domain_model.md       → Entities, enums, validations, Flyway migrations
specs/003_data_access.md        → Repositories, queries, specifications, pagination
specs/004_business_rules.md     → Services, state machine, client interfaces
specs/005_api_endpoints.md      → Controllers, DTOs, mappers, OpenAPI
specs/006_security.md           → JWT auth, roles, ownership, CORS
specs/007_error_handling.md     → Global exception handler, error responses
specs/008_integrations.md       → REST clients, Resilience4j, health
specs/009_testing.md            → Unit, integration, E2E tests, WireMock, embedded PostgreSQL
specs/010_deployment.md         → Dockerfile, Docker Compose, profiles, CI, logging
```

When working across multiple layers, read the affected specs in order so dependency assumptions stay aligned.

## Build & Test

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run only unit tests
./mvnw test -Dgroups="!integration,!e2e"

# Run only integration tests
./mvnw test -Dgroups="integration"

# Run only E2E tests
./mvnw test -Dgroups="e2e"

# Run a single test class
./mvnw test -Dtest="BookingServiceCreateTest"

# Type-check / compile without running
./mvnw compile

# Run locally with stub clients
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Maven Surefire must be configured so `-Dgroups` selects JUnit 5 `@Tag` values (`integration`, `e2e`).

Always run `./mvnw compile` after editing Java files to catch errors early. Run the relevant test class after any code change, not the full suite. For documentation/spec-only changes, run `git diff --check`.

## Delivery Evidence

This repo is intended to be understandable from GitHub by another person or
agent using their own Gas Town workspace. Do not rely on local Beads databases,
local shell history, or machine-specific paths for the final work report.

Before closing an implementation bead, update the delivery evidence files:

- `docs/delivery/IMPLEMENTATION_LEDGER.md` records bead-level delivery, PR, merge commit, changed files, started/completed UTC timestamps, elapsed wall time, and blockers.
- `docs/delivery/QUALITY_LOG.md` records verification commands, results, skipped checks, and environment blockers.
- `docs/delivery/README.md` explains the evidence format and should stay aligned when new report files are added.

Record evidence in the same PR as the implementation whenever practical. If a
bead is documentation-only, still record verification in the PR body or
`QUALITY_LOG.md` when it affects the delivery workflow.

## Issue Tracking (Beads)

This project uses `bd` (beads) for issue tracking. Do not use markdown TODO lists or TASKS.md files for task tracking.

```bash
# Get workflow context and command guidance
bd prime

# View ready tasks
bd ready

# Show details of a specific task
bd show <id>

# Claim a task before working on it
bd update <id> --claim

# Close a completed task
bd close <id>

# Save a persistent insight about the project
bd remember "insight"
```

When starting work, run `bd prime` first to understand current state. Always `bd update <id> --claim` before starting a task and `bd close <id>` when done. Use `bd remember` to persist useful discoveries about the codebase — do not create MEMORY.md files.

## Project Structure

```
src/main/java/com/cargo/booking/
├── BookingServiceApplication.java       # Entry point
├── controller/                          # REST controllers
├── service/                             # Business logic (BookingService, BookingStateMachine, BookingReferenceGenerator)
├── repository/                          # Spring Data JPA repositories
├── model/
│   ├── entity/                          # JPA entities (Booking, BookingEquipmentLine)
│   └── enums/                           # BookingStatus, EquipmentType
├── dto/
│   ├── request/                         # Inbound DTOs (Java records)
│   └── response/                        # Outbound DTOs (Java records)
├── config/                              # Spring configuration beans
├── exception/                           # Custom exceptions and global handler
├── client/                              # External service clients (Schedule, Equipment, Quote)
│   └── dto/                             # DTOs for external API responses
├── mapper/                              # Entity ↔ DTO mapping
└── security/                            # JWT filter, token provider, ownership authorization helpers
    └── BookingAccessAuthorizer.java     # Customer ownership checks before service calls

src/main/resources/
├── application.yml                      # Base config
├── application-local.yml                # Local dev (stubs, console logging)
├── application-dev.yml                  # Dev environment
├── application-prod.yml                 # Production (hardened)
├── db/migration/                        # Flyway SQL scripts
└── logback-spring.xml                   # Logging configuration

src/test/java/com/cargo/booking/
├── testutil/                            # TestDataBuilder, JwtTestHelper
├── service/                             # Unit tests (Mockito)
├── controller/                          # MockMvc tests
├── repository/                          # @DataJpaTest tests
├── client/                              # WireMock tests
├── security/                            # Auth integration tests
├── exception/                           # Error handling tests
└── BookingLifecycleE2ETest.java         # Full lifecycle E2E
```

## Conventions & Patterns

- **Java 21 LTS**, Spring Boot 3.5.x, Maven
- **Layered architecture**: Controller → Service → Repository. No skipping layers, except security authorizers may query repositories for ownership checks only.
- **DTOs are Java records** (immutable). Never expose JPA entities in API responses.
- **Constructor injection** only. No `@Autowired` on fields.
- **Lombok**: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities. `@RequiredArgsConstructor` on services/controllers.
- **Entity IDs**: Long with `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- **Timestamps**: `Instant` in UTC, serialized as ISO-8601. Use `@CreationTimestamp` / `@UpdateTimestamp`.
- **Table names**: `lowercase_snake_case`
- **API prefix**: `/api/v1`
- **Logging**: SLF4J. INFO for business actions, WARN for client errors, ERROR for system failures. Never log sensitive data (email, phone, tokens).
- **Tests**: Method names use `should...()` pattern. One behavior per test. Arrange-Act-Assert structure. Integration tests use embedded PostgreSQL.
- **Spring Boot 3.5 tests**: Use `@MockitoBean` for MVC slice mocks, not the older `@MockBean`.

## Key Domain Rules

The booking state machine is strict. Only these transitions are legal:

```
PENDING     → CONFIRMED, CANCELLED
CONFIRMED   → IN_PROGRESS, CANCELLED
IN_PROGRESS → COMPLETED
```

All other transitions are rejected by `BookingStateMachine`, which throws `IllegalStateTransitionException` before any status change.

Equipment release failures during cancellation must NOT block the cancel — log a warning and proceed.

## External Service Clients

Three external APIs will be called synchronously. **Only interfaces and local stubs exist today.** The real services are owned by other teams and may not be implemented yet.

| Client          | Purpose                              | Failure behavior                         |
|-----------------|--------------------------------------|------------------------------------------|
| ScheduleClient  | Validate schedule exists and is open | Throw `ScheduleNotAvailableException`    |
| EquipmentClient | Reserve/release containers           | Throw `EquipmentReservationException`    |
| QuoteClient     | Validate quote matches booking       | Throw `QuoteNotValidException`           |

Each client is defined as a **Java interface** with a **stub implementation** (`@Profile("local")`) that returns dummy data. Real implementations will be added later when the external services are available. See `specs/004_business_rules.md` for the interface contracts and `specs/008_integrations.md` for the planned real implementation design.

Do not implement real external client behavior until the external API contracts are available.

## Security

JWT-based stateless auth can be enabled for protected deployments. Local/unsecured mode can disable security and rely on request data such as `customerId`. Four roles apply when security is enabled:

| Role          | Can do                                                      |
|---------------|-------------------------------------------------------------|
| CUSTOMER      | Direct customer token; create/view/cancel only when token has matching customerId claim |
| SERVICE       | Trusted service token; create/read/cancel on behalf of request customerId |
| OPERATOR      | View all bookings, confirm/start/complete                   |
| ADMIN         | Everything                                                  |

JWT subject identifies the requester, which may be a service, not the booking customer. Customers have ownership checks when security is enabled only via an explicit `customerId` / `customer_id` token claim. Swagger UI, API docs, `/actuator/health`, and `/actuator/info` are public; `/actuator/metrics` requires ADMIN.

`BookingAccessAuthorizer` owns customer authorization checks before `BookingService` calls. It validates create/list request customer IDs with `authorizeCreateCustomer(Long)` and `authorizeListCustomer(Long)`, and validates existing booking ownership for get/cancel operations with `authorizeBookingAccess(Long)` and `authorizeBookingAccess(String)`. The current specs use explicit controller calls to this authorizer; do not replace them with `@PreAuthorize` unless the specs are changed consistently.

For `ROLE_CUSTOMER`, a missing JWT `customerId` / `customer_id` claim must return HTTP 403 before comparing request body or query parameter customer IDs. For list requests, a missing `customerId` query parameter is HTTP 400 only when the CUSTOMER token contains a customer identity claim.

When security is disabled, JWT validation and ownership checks are skipped. The service still uses `customerId` from request bodies or query parameters.

## Error Response Format

All errors follow this structure:

```json
{
  "timestamp": "ISO-8601",
  "status": 404,
  "error": "Not Found",
  "message": "Booking not found with reference BKG-2026-00042",
  "path": "/api/v1/bookings/BKG-2026-00042",
  "requestId": "req-123"
}
```

`requestId` is optional and omitted when absent. Validation errors (400) add a `violations` array with field-level details.

## Infrastructure

```bash
# Start PostgreSQL and the application
docker-compose up -d

# Build and run the service container
docker build -t booking-service .
docker-compose up -d booking-service

# View logs
docker-compose logs -f booking-service
```

| Service    | Port  |
|------------|-------|
| App        | 8081  |
| PostgreSQL | 5432  |
| Swagger UI | 8081/swagger-ui |

## Git Workflow

- Branch from the repository's default branch. This is normally `main` or `develop`; use `master` if that is the configured default branch.
- Run `./mvnw compile` and relevant tests before committing
- Commit messages: concise, imperative mood (e.g. "Add booking cancellation endpoint")
- One logical change per commit

## Do NOT

- Do not use field injection (`@Autowired` on fields)
- Do not expose JPA entities directly in REST responses
- Do not catch generic `Exception` in service methods — let it bubble to the global handler
- Do not hardcode configuration values — use application.yml with environment variable fallbacks
- Do not log sensitive customer data (email, phone, JWT tokens)
- Do not skip writing tests — every service method and controller endpoint needs test coverage
- Do not modify Flyway migration files once they have been applied, shared, or merged — create new migration files instead
- Do not add Kafka, asynchronous messaging, or event streaming in v1
