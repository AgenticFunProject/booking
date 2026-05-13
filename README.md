# Booking Service — AI-Driven Specification

## What is this?

This repository contains a set of **sequential specification files** that describe a complete **Cargo Booking Service** built with Spring Boot. The specifications are written in a Gherkin-inspired format designed to be consumed by a generative AI agent, which will use them to scaffold and build the application from scratch.

The service accepts and manages cargo booking requests, coordinates a shipment lifecycle, and emits domain events for downstream systems.

## How it works

The AI agent reads the specification files **in order** (001 → 010). Each file builds on concepts introduced in earlier files, forming a dependency chain:

```
001 Project Setup
 └─▶ 002 Domain Model
      └─▶ 003 Data Access
           └─▶ 004 Business Rules
                └─▶ 005 API Endpoints
                     └─▶ 006 Security
                          └─▶ 007 Error Handling
                               └─▶ 008 Integrations
                                    └─▶ 009 Testing
                                         └─▶ 010 Deployment
```

After processing all files, the result is a fully runnable Spring Boot microservice with Docker support, tests, and CI configuration.

## Specification Files

| File | Purpose |
|------|---------|
| `001_project_setup.md` | Maven project, Java 21, dependencies, package structure, coding conventions |
| `002_domain_model.md` | JPA entities, enums, validations, state machine, Flyway migrations |
| `003_data_access.md` | Repository interfaces, custom queries, pagination, specifications |
| `004_business_rules.md` | Service layer, booking lifecycle, domain events (Kafka), external client interfaces |
| `005_api_endpoints.md` | REST controllers, request/response DTOs, mappers, OpenAPI documentation |
| `006_security.md` | JWT authentication, role-based authorization, ownership checks, CORS |
| `007_error_handling.md` | Global exception handler, error response structure, validation errors |
| `008_integrations.md` | External API clients (Schedule, Equipment, Quote), Resilience4j, Kafka producer config |
| `009_testing.md` | Unit tests, integration tests, E2E tests, WireMock, Testcontainers, test utilities |
| `010_deployment.md` | Dockerfile, Docker Compose, Spring profiles, logging, CI pipeline, README |

## File format

Each file uses a **Gherkin-inspired structure** with `Feature`, `Scenario`, `Given/When/Then`, data tables, and tags. This is not strict Cucumber-compliant Gherkin — it's a structured specification format that gives the AI agent unambiguous, sequentially readable instructions.

Key elements in each file:

- **Header block** — file name, dependencies, what it produces, and context
- **Tags** (e.g. `@domain @entity`, `@api @endpoint`) — categorize scenarios for filtering
- **Data tables** — specify fields, types, validation rules, and configuration
- **Out-of-scope section** — tells the agent what is NOT covered and where to find it

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.4.x |
| Build Tool | Maven |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka |
| Auth | JWT (stateless) |
| Resilience | Resilience4j (circuit breaker, retry) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Migrations | Flyway |
| Testing | JUnit 5, Mockito, Testcontainers, WireMock |
| Containerization | Docker, Docker Compose |
| CI | GitHub Actions |

## Booking Lifecycle

```
PENDING ──▶ CONFIRMED ──▶ IN_PROGRESS ──▶ COMPLETED
   │             │
   ▼             ▼
CANCELLED    CANCELLED
```

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/bookings | Create a new booking |
| GET | /api/v1/bookings/{id} | Get booking by ID or reference |
| GET | /api/v1/bookings?customerId= | List bookings for a customer |
| PATCH | /api/v1/bookings/{id}/cancel | Cancel a booking |
| PATCH | /api/v1/bookings/{id}/confirm | Confirm a booking |
| PATCH | /api/v1/bookings/{id}/start | Mark booking as in progress |
| PATCH | /api/v1/bookings/{id}/complete | Mark booking as completed |

## Domain Events (Kafka Topics)

| Topic | Trigger |
|-------|---------|
| `booking.created` | New booking submitted |
| `booking.confirmed` | Booking confirmed, equipment reserved |
| `booking.cancelled` | Booking cancelled |
| `booking.in_progress` | Shipment started |
| `booking.completed` | Shipment delivered |

## External Dependencies

| Service | Purpose |
|---------|---------|
| Schedules API | Validate schedule availability |
| Equipment API | Reserve/release containers |
| Quotes API | Validate quote validity |

Stub implementations are provided for local development (`@Profile("local")`).

## Getting Started

Once the AI agent has generated the codebase from these specs:

```bash
# Start infrastructure (PostgreSQL, Kafka, Kafka UI)
docker-compose up -d

# Run the application locally with stubs
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Open Swagger UI
open http://localhost:8081/swagger-ui

# Run tests
./mvnw test
```

## Contributing

1. Specification changes go through PR review like code
2. When modifying a spec file, check downstream files for impacts (follow the dependency chain)
3. Each spec file has an "Out of Scope" section — use it to understand boundaries between files
4. Keep the numbering and naming convention consistent: `NNN_description.md`

## License

[Add your license here]
