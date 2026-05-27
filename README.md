# Booking Service

Cargo Booking Service is a Spring Boot 3.5 microservice for creating booking
requests and managing the shipment lifecycle:

```text
PENDING -> CONFIRMED -> IN_PROGRESS -> COMPLETED
   |           |
   v           v
CANCELLED   CANCELLED
```

The service exposes REST endpoints under `/api/v1`, persists bookings in
PostgreSQL, validates schema with Flyway, supports optional JWT security, and
uses local stub clients for schedule, equipment, and quote integrations. Real
external client implementations are intentionally deferred until those API
contracts are finalized.

## Prerequisites

- Java 21
- Maven wrapper from this repository (`./mvnw`)
- Docker and Docker Compose for containerized local runs
- PostgreSQL 16 when running outside Docker Compose

## Quick Start

```bash
# Build the application jar without running tests
make build

# Run all tests
make test

# Start PostgreSQL and the app with the local profile
make docker-up

# Follow application logs
make docker-logs
```

The app listens on port `8081` by default. Docker Compose starts PostgreSQL on
`5432`, runs the application with `SPRING_PROFILES_ACTIVE=local`, disables
security, and uses the local stub clients.

To run the app directly from the Maven wrapper:

```bash
make run
```

## Local Development

Useful Make targets:

| Target | Command |
| --- | --- |
| `make build` | `./mvnw clean package -DskipTests` |
| `make test` | `./mvnw test` |
| `make test-unit` | `./mvnw test -Dgroups="!integration,!e2e"` |
| `make test-integration` | `./mvnw test -Dgroups="integration"` |
| `make test-e2e` | `./mvnw test -Dgroups="e2e"` |
| `make run` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| `make docker-build` | `docker build -t booking-service .` |
| `make docker-up` | `docker-compose up -d` |
| `make docker-down` | `docker-compose down` |
| `make docker-logs` | `docker-compose logs -f booking-service` |
| `make clean` | `./mvnw clean` and `docker-compose down -v` |
| `make swagger` | Opens or prints the Swagger UI URL |

Equivalent direct commands are documented in `AGENTS.md` for implementation
agents and CI debugging.

## API Docs

When the service is running:

- Swagger UI: `http://localhost:8081/swagger-ui`
- OpenAPI JSON: `http://localhost:8081/api-docs`
- Health: `http://localhost:8081/actuator/health`
- Info: `http://localhost:8081/actuator/info`

`/actuator/health`, `/actuator/info`, Swagger UI, and API docs are public.
`/actuator/metrics` requires `ADMIN` when security is enabled.

## API Overview

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/bookings` | Create a booking in `PENDING` status |
| `GET` | `/api/v1/bookings` | List bookings with optional `customerId` and `status` filters |
| `GET` | `/api/v1/bookings/{id}` | Get by numeric ID or `BKG-YYYY-NNNNN` reference |
| `PATCH` | `/api/v1/bookings/{id}/cancel` | Cancel a booking |
| `PATCH` | `/api/v1/bookings/{id}/confirm` | Confirm a booking and reserve equipment |
| `PATCH` | `/api/v1/bookings/{id}/start` | Mark a confirmed booking as in progress |
| `PATCH` | `/api/v1/bookings/{id}/complete` | Complete an in-progress booking |

Errors use the shared response shape with `timestamp`, `status`, `error`,
`message`, `path`, and optional `requestId`. Validation errors add field-level
violations.

## Environment Variables

Use `.env.example` as the safe template for local or deployed environment
values. Real `.env` files are ignored by Git.

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile, normally `local`, `dev`, or `prod` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `SECURITY_ENABLED` | Enables JWT authentication and authorization |
| `JWT_ISSUER` / `AUTH_JWT_ISSUER` | Expected JWT issuer |
| `JWT_AUDIENCE` / `AUTH_JWT_AUDIENCE` | Expected JWT audience |
| `JWT_SECRET` / `AUTH_JWT_SECRET` | HMAC signing secret; use a real 256-bit-or-stronger secret outside local development |
| `JWT_EXPIRATION` | Token lifetime in milliseconds |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins |
| `SCHEDULE_API_URL` / `SCHEDULE_API_TIMEOUT` | Schedule API placeholder URL and timeout |
| `EQUIPMENT_API_URL` / `EQUIPMENT_API_TIMEOUT` | Equipment API placeholder URL and timeout |
| `QUOTE_API_URL` / `QUOTE_API_TIMEOUT` | Quote API placeholder URL and timeout |

Profile notes:

- `local`: stub clients, readable console logs, security disabled by default in
  Compose.
- `dev`: deployed-environment placeholders, JSON logs, security enabled.
- `prod`: strict security, hidden error and health details, reduced actuator
  exposure, JSON logs.
- `test`: embedded PostgreSQL support and test JWT defaults.

## Testing And CI

Run focused checks locally:

```bash
./mvnw compile
./mvnw test -Dgroups="!integration,!e2e"
./mvnw test -Dgroups="integration"
./mvnw test -Dgroups="e2e"
./mvnw test -Pcontract
./mvnw test -Dtest=BookingServiceCreateTest
```

GitHub Actions runs on pushes and pull requests targeting `master` and
`develop`. The CI workflow:

1. Sets up Java 21.
2. Runs unit, integration, and E2E Maven group selectors.
3. Runs the Gherkin contract suite with `./mvnw test -Pcontract`.
4. Uploads Surefire reports.
5. Packages the jar.
6. Builds the Docker image.

## Project Structure

```text
src/main/java/com/cargo/booking/
├── BookingServiceApplication.java
├── client/          # External service client interfaces and local stubs
├── config/          # Spring, security, integration, logging, and tracing config
├── controller/      # REST controllers
├── dto/             # Request and response records
├── exception/       # Business exceptions and structured error handling
├── mapper/          # Entity to DTO mapping
├── model/           # JPA entities and enums
├── repository/      # Spring Data JPA repositories and specifications
├── security/        # JWT, requester context, and access authorization helpers
└── service/         # Booking lifecycle and business orchestration

src/main/resources/
├── application.yml
├── application-local.yml
├── application-dev.yml
├── application-prod.yml
├── db/migration/
└── logback-spring.xml

src/test/java/com/cargo/booking/
├── client/
├── config/
├── controller/
├── exception/
├── repository/
├── security/
├── service/
├── testutil/
└── BookingLifecycleE2ETest.java
```

## Architecture

The application follows a layered Spring architecture:

```text
Controller -> BookingAccessAuthorizer -> BookingService -> Repository
                                      -> ScheduleClient / QuoteClient / EquipmentClient
```

- Controllers accept and return DTO records; JPA entities are not exposed in API
  responses.
- `BookingService` owns business rules and state transitions.
- `BookingStateMachine` rejects invalid lifecycle transitions before status
  changes.
- `BookingAccessAuthorizer` enforces customer ownership checks when security is
  enabled.
- Local integration stubs are activated with the `local` profile.
- Request tracing uses MDC keys for `requestId`, authenticated `principal`,
  `customerId`, and `bookingRef`; dev/prod logs emit MDC in JSON.

## Deployment Assets

- `Dockerfile`: multi-stage Java 21 Alpine build/runtime image with non-root
  execution and actuator healthcheck.
- `docker-compose.yml`: local PostgreSQL plus booking-service stack.
- `.env.example`: safe environment variable template.
- `.github/workflows/ci.yml`: Maven and Docker build workflow.
- `Makefile`: common local build, test, Docker, and Swagger commands.

## Specifications And Delivery Evidence

The implementation is built from sequential specs in `specs/001_project_setup.md`
through `specs/010_deployment.md`. Delivery evidence is tracked in:

- `docs/delivery/IMPLEMENTATION_LEDGER.md`
- `docs/delivery/QUALITY_LOG.md`
- `docs/delivery/README.md`

## Contributing

- Work on a branch, not `master`.
- Open a GitHub pull request for repository changes.
- Run `./mvnw compile` after Java changes and the relevant focused tests for
  the area touched.
- For docs-only changes, run `git diff --check`.
- Keep changes scoped to the bead or issue; file separate work for unrelated
  findings.
- Update delivery evidence for implementation beads.
