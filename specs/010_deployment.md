# File: 010_deployment.md
# Depends on: 001_project_setup.txt, 006_security.md, 008_integrations.md, 009_testing.md
# Produces: Dockerfile, Docker Compose, environment-specific profiles, logging configuration,
#           CI pipeline definition, health check scripts, README
# Context: Defines how the Cargo Booking Service is built, containerized, and deployed.
#          The AI agent must have processed 001–009 so that all dependencies, configuration
#          properties, and external service details are known.

Feature: Deployment and Infrastructure
  As an AI code generator
  I need to define the containerization, environment configuration, and deployment pipeline
  So that the Booking Service can be built, tested, and deployed consistently across environments

  Background:
    Given the application name is "booking-service"
    And the base package is "com.cargo.booking"
    And the build tool is Maven
    And Java version is 21 (from 001_project_setup.txt)
    And the application port is 8081 (from 001_project_setup.txt)

  # ---------------------------------------------------------------------------
  # Dockerfile
  # ---------------------------------------------------------------------------

  @deployment @docker
  Scenario: Multi-stage Dockerfile
    Given a file "Dockerfile" in the project root
    Then it must use a multi-stage build with the following stages:

    And Stage 1 (build) must:
      | step | instruction                                                          |
      | 1    | FROM eclipse-temurin:21-jdk-alpine AS build                          |
      | 2    | WORKDIR /app                                                         |
      | 3    | COPY pom.xml .                                                       |
      | 4    | COPY .mvn .mvn                                                       |
      | 5    | COPY mvnw .                                                          |
      | 6    | RUN ./mvnw dependency:go-offline -B (cache dependencies)             |
      | 7    | COPY src ./src                                                       |
      | 8    | RUN ./mvnw package -DskipTests -B                                    |

    And Stage 2 (runtime) must:
      | step | instruction                                                          |
      | 1    | FROM eclipse-temurin:21-jre-alpine AS runtime                        |
      | 2    | RUN addgroup -S appgroup && adduser -S appuser -G appgroup           |
      | 3    | WORKDIR /app                                                         |
      | 4    | COPY --from=build /app/target/booking-service-*.jar app.jar          |
      | 5    | USER appuser                                                         |
      | 6    | EXPOSE 8081                                                          |
      | 7    | HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8081/actuator/health \|\| exit 1 |
      | 8    | ENTRYPOINT ["java", "-jar", "app.jar"]                               |

  @deployment @docker
  Scenario: Dockerfile best practices
    Given the Dockerfile
    Then the following rules must apply:
      | rule                                                                               |
      | Use Alpine-based images to minimize image size                                     |
      | Run the application as a non-root user (appuser)                                   |
      | Dependencies are cached in a separate layer before copying source code             |
      | No secrets or credentials are baked into the image                                 |
      | The .dockerignore file must exclude target/, .git/, .idea/, *.iml, .env            |

  @deployment @docker
  Scenario: .dockerignore file
    Given a file ".dockerignore" in the project root
    Then it must contain:
      """
      target/
      .git/
      .gitignore
      .idea/
      *.iml
      .env
      .env.*
      docker-compose*.yml
      README.md
      docs/
      *.log
      """

  # ---------------------------------------------------------------------------
  # Docker Compose — Local Development
  # ---------------------------------------------------------------------------

  @deployment @compose
  Scenario: Docker Compose for local development
    Given a file "docker-compose.yml" in the project root
    Then it must define the following services:

  @deployment @compose
  Scenario: PostgreSQL service
    Given the docker-compose.yml file
    Then it must define a "postgres" service:
      | setting       | value                                |
      | image         | postgres:16-alpine                   |
      | container_name| booking-postgres                     |
      | ports         | 5432:5432                            |
      | environment   | POSTGRES_DB=booking_db               |
      | environment   | POSTGRES_USER=booking_user           |
      | environment   | POSTGRES_PASSWORD=booking_pass       |
      | volumes       | postgres_data:/var/lib/postgresql/data|
      | healthcheck   | pg_isready -U booking_user -d booking_db |
      | restart       | unless-stopped                       |

  @deployment @compose
  Scenario: Kafka and Zookeeper services
    Given the docker-compose.yml file
    Then it must define a "zookeeper" service:
      | setting       | value                                |
      | image         | confluentinc/cp-zookeeper:7.6.0      |
      | container_name| booking-zookeeper                    |
      | ports         | 2181:2181                            |
      | environment   | ZOOKEEPER_CLIENT_PORT=2181           |
    And it must define a "kafka" service:
      | setting       | value                                |
      | image         | confluentinc/cp-kafka:7.6.0          |
      | container_name| booking-kafka                        |
      | ports         | 9092:9092                            |
      | depends_on    | zookeeper                            |
      | environment   | KAFKA_BROKER_ID=1                    |
      | environment   | KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 |
      | environment   | KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 |
      | environment   | KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 |
      | healthcheck   | kafka-topics --bootstrap-server localhost:9092 --list |
      | restart       | unless-stopped                       |

  @deployment @compose
  Scenario: Kafka UI service (optional developer tool)
    Given the docker-compose.yml file
    Then it must define a "kafka-ui" service:
      | setting       | value                                |
      | image         | provectuslabs/kafka-ui:latest        |
      | container_name| booking-kafka-ui                     |
      | ports         | 8080:8080                            |
      | depends_on    | kafka                                |
      | environment   | KAFKA_CLUSTERS_0_NAME=local          |
      | environment   | KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:9092 |
      | restart       | unless-stopped                       |

  @deployment @compose
  Scenario: Booking Service application container
    Given the docker-compose.yml file
    Then it must define a "booking-service" service:
      | setting       | value                                |
      | build         | . (current directory Dockerfile)     |
      | container_name| booking-service                      |
      | ports         | 8081:8081                            |
      | depends_on    | postgres (condition: service_healthy), kafka (condition: service_healthy) |
      | environment   | SPRING_PROFILES_ACTIVE=local         |
      | environment   | DB_USERNAME=booking_user             |
      | environment   | DB_PASSWORD=booking_pass             |
      | environment   | SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/booking_db |
      | environment   | KAFKA_BOOTSTRAP_SERVERS=kafka:9092   |
      | environment   | JWT_SECRET=${JWT_SECRET:-dev-secret-key-that-is-at-least-256-bits-long-for-hs256} |
      | restart       | unless-stopped                       |
    And it must use the "local" profile so stub clients are activated

  @deployment @compose
  Scenario: Docker Compose volumes
    Given the docker-compose.yml file
    Then it must define a named volume:
      | volume         | purpose                              |
      | postgres_data  | Persist PostgreSQL data across restarts |

  # ---------------------------------------------------------------------------
  # Environment Profiles
  # ---------------------------------------------------------------------------

  @deployment @profiles
  Scenario: Spring profiles strategy
    Given the Booking Service
    Then the following Spring profiles must be supported:
      | profile     | purpose                                           | configuration                                |
      | local       | Local development with stubs                      | Stub clients, local DB, console logging       |
      | dev         | Development/staging environment                   | Real clients, dev DB, JSON logging            |
      | prod        | Production environment                            | Real clients, prod DB, JSON logging, strict   |
      | test        | Automated testing                                 | H2 or Testcontainers, embedded Kafka          |

  @deployment @profiles
  Scenario: Profile-specific application properties
    Given the following profile-specific config files must exist:
      | file                                   | key overrides                                          |
      | application.yml                        | Base config (defaults to local-friendly values)        |
      | application-local.yml                  | Explicit local settings, verbose logging               |
      | application-dev.yml                    | Dev environment URLs, JSON logging                     |
      | application-prod.yml                   | Prod URLs, JSON logging, stricter security             |
      | application-test.yml                   | H2 DB, embedded Kafka, test JWT secret                 |

  @deployment @profiles
  Scenario: Production profile hardening
    Given the file "src/main/resources/application-prod.yml"
    Then it must include:
      | property                                      | value                  | purpose                              |
      | spring.jpa.show-sql                           | false                  | No SQL logging in prod               |
      | spring.jpa.open-in-view                       | false                  | Already set in base, reinforced here |
      | management.endpoints.web.exposure.include      | health,info            | Minimize exposed actuator endpoints  |
      | management.endpoint.health.show-details        | never                  | Hide health details from external    |
      | server.error.include-message                   | never                  | Never leak error details             |
      | server.error.include-stacktrace                | never                  | Never leak stack traces              |
      | logging.level.root                             | WARN                   | Less verbose in production           |
      | logging.level.com.cargo.booking                | INFO                   | App-level info logging               |

  # ---------------------------------------------------------------------------
  # Logging Configuration
  # ---------------------------------------------------------------------------

  @deployment @logging
  Scenario: Logback configuration for local profile
    Given a file "src/main/resources/logback-spring.xml"
    Then it must define profile-specific logging:

    And for the "local" profile:
      | setting              | value                                      |
      | appender             | ConsoleAppender                            |
      | pattern              | %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n |
      | root level           | INFO                                       |
      | com.cargo.booking    | DEBUG                                      |

    And for the "dev" and "prod" profiles:
      | setting              | value                                      |
      | appender             | ConsoleAppender with JSON layout            |
      | JSON fields          | timestamp, level, logger, message, thread, mdc |
      | root level           | WARN (prod), INFO (dev)                    |
      | com.cargo.booking    | INFO                                       |

  @deployment @logging
  Scenario: Structured logging dependency
    Given the pom.xml dependency section
    Then it must include a JSON logging encoder:
      | groupId                  | artifactId                        | purpose                     |
      | net.logstash.logback     | logstash-logback-encoder          | JSON structured logging     |

  @deployment @logging
  Scenario: MDC context for request tracing
    Given the Booking Service request handling
    Then a filter or interceptor must add the following to the MDC:
      | mdc key        | source                                           |
      | requestId      | X-Request-ID header, or generated UUID if absent |
      | userId         | Extracted from JWT (authenticated requests only) |
      | bookingRef     | Set by the service layer when available           |
    And the MDC must be cleared after each request
    And the filter must be registered with the highest priority (runs first)

  # ---------------------------------------------------------------------------
  # Environment Variables Reference
  # ---------------------------------------------------------------------------

  @deployment @env
  Scenario: Required environment variables
    Given the Booking Service deployment
    Then the following environment variables must be documented:
      | variable                  | required | default                          | description                    |
      | DB_USERNAME               | yes      | booking_user                     | Database username              |
      | DB_PASSWORD               | yes      | booking_pass                     | Database password              |
      | SPRING_DATASOURCE_URL     | yes      | jdbc:postgresql://localhost:5432/booking_db | JDBC URL          |
      | KAFKA_BOOTSTRAP_SERVERS   | yes      | localhost:9092                   | Kafka bootstrap servers        |
      | JWT_SECRET                | yes      | (dev default in yml)             | JWT signing key (min 256 bits) |
      | JWT_ISSUER                | no       | cargo-platform                   | Expected JWT issuer            |
      | SCHEDULE_API_URL          | no       | http://localhost:8082            | Schedules API base URL         |
      | EQUIPMENT_API_URL         | no       | http://localhost:8083            | Equipment API base URL         |
      | QUOTE_API_URL             | no       | http://localhost:8084            | Quotes API base URL            |
      | CORS_ALLOWED_ORIGINS      | no       | http://localhost:3000            | CORS allowed origins           |
      | SPRING_PROFILES_ACTIVE    | no       | (none, defaults apply)           | Active Spring profile          |

  @deployment @env
  Scenario: .env.example file
    Given a file ".env.example" in the project root
    Then it must list all environment variables with placeholder values
    And a comment at the top must say "Copy this file to .env and fill in your values"
    And .env must be listed in .gitignore

  # ---------------------------------------------------------------------------
  # CI Pipeline
  # ---------------------------------------------------------------------------

  @deployment @ci
  Scenario: GitHub Actions CI pipeline
    Given a file ".github/workflows/ci.yml"
    Then it must define a workflow named "CI" triggered on:
      | trigger          | branches       |
      | push             | main, develop  |
      | pull_request     | main, develop  |

  @deployment @ci
  Scenario: CI pipeline jobs
    Given the CI workflow
    Then it must define the following jobs:

    And a "build-and-test" job:
      | step | name                    | action                                              |
      | 1    | Checkout code           | actions/checkout@v4                                 |
      | 2    | Set up Java 21          | actions/setup-java@v4 with temurin distribution     |
      | 3    | Cache Maven deps        | actions/cache@v4 for ~/.m2/repository               |
      | 4    | Run unit tests          | ./mvnw test -Dgroups="!integration,!e2e"            |
      | 5    | Run integration tests   | ./mvnw test -Dgroups="integration"                  |
      | 6    | Run E2E tests           | ./mvnw test -Dgroups="e2e"                          |
      | 7    | Generate test report    | Upload test results as artifact                     |
      | 8    | Build JAR               | ./mvnw package -DskipTests                          |
      | 9    | Upload JAR artifact     | actions/upload-artifact@v4                          |

    And a "docker-build" job (depends on build-and-test):
      | step | name                    | action                                              |
      | 1    | Checkout code           | actions/checkout@v4                                 |
      | 2    | Set up Docker Buildx    | docker/setup-buildx-action@v3                       |
      | 3    | Build Docker image      | docker build -t booking-service:${{ github.sha }}   |
      | 4    | Tag as latest           | Only on main branch                                 |

  @deployment @ci
  Scenario: CI pipeline services
    Given the CI "build-and-test" job
    Then it must define the following services for integration tests:
      | service     | image                        | ports     | env                              |
      | postgres    | postgres:16-alpine           | 5432:5432 | POSTGRES_DB=booking_test_db      |
      | kafka       | confluentinc/cp-kafka:7.6.0  | 9092:9092 | Standard single-node config      |
    And test environment variables must point to these CI services

  # ---------------------------------------------------------------------------
  # JVM Tuning
  # ---------------------------------------------------------------------------

  @deployment @jvm
  Scenario: JVM configuration for containers
    Given the Docker ENTRYPOINT
    Then the JVM must be configured with container-aware settings:
      """
      ENTRYPOINT ["java",
        "-XX:+UseContainerSupport",
        "-XX:MaxRAMPercentage=75.0",
        "-XX:InitialRAMPercentage=50.0",
        "-Djava.security.egd=file:/dev/./urandom",
        "-jar", "app.jar"]
      """
    And these settings ensure the JVM respects container memory limits
    And -Djava.security.egd speeds up random number generation for UUID and JWT

  # ---------------------------------------------------------------------------
  # Graceful Shutdown
  # ---------------------------------------------------------------------------

  @deployment @shutdown
  Scenario: Graceful shutdown configuration
    Given the file "src/main/resources/application.yml"
    Then it must include:
      | property                                      | value    | purpose                                     |
      | server.shutdown                               | graceful | Wait for in-flight requests to complete     |
      | spring.lifecycle.timeout-per-shutdown-phase    | 30s      | Max wait time before forced shutdown        |
    And the Kafka producer must flush pending messages during shutdown
    And the application must handle SIGTERM gracefully in the Docker container

  # ---------------------------------------------------------------------------
  # README
  # ---------------------------------------------------------------------------

  @deployment @docs
  Scenario: Project README
    Given a file "README.md" in the project root
    Then it must contain the following sections:
      | section              | content                                                       |
      | Title                | Booking Service                                               |
      | Description          | Brief description of the service and its responsibilities     |
      | Prerequisites        | Java 21, Maven, Docker, Docker Compose                        |
      | Quick Start          | docker-compose up -d, then access Swagger UI                  |
      | Local Development    | How to run with local profile, stub clients                   |
      | API Documentation    | Link to Swagger UI at http://localhost:8081/swagger-ui         |
      | Environment Variables| Table from the env vars scenario above                        |
      | Running Tests        | mvnw test, mvnw test -Dgroups="integration"                  |
      | Project Structure    | Package layout from 001_project_setup.txt                     |
      | Architecture         | Brief description of layers and external dependencies         |
      | Kafka Topics         | Table of events from 004_business_rules.md                    |
      | Contributing         | Branch naming, PR process, test requirements                  |

  # ---------------------------------------------------------------------------
  # Git Configuration
  # ---------------------------------------------------------------------------

  @deployment @git
  Scenario: .gitignore file
    Given a file ".gitignore" in the project root
    Then it must contain:
      """
      # Build
      target/
      *.jar
      *.war

      # IDE
      .idea/
      *.iml
      .vscode/
      .settings/
      .project
      .classpath

      # Environment
      .env
      .env.*
      !.env.example

      # OS
      .DS_Store
      Thumbs.db

      # Logs
      *.log
      logs/

      # Docker
      docker-compose.override.yml
      """

  # ---------------------------------------------------------------------------
  # Makefile (Developer Convenience)
  # ---------------------------------------------------------------------------

  @deployment @developer
  Scenario: Makefile for common tasks
    Given a file "Makefile" in the project root
    Then it must define the following targets:
      | target          | command                                                  | description                    |
      | build           | ./mvnw clean package -DskipTests                         | Build the JAR                  |
      | test            | ./mvnw test                                              | Run all tests                  |
      | test-unit       | ./mvnw test -Dgroups="!integration,!e2e"                 | Run unit tests only            |
      | test-integration| ./mvnw test -Dgroups="integration"                       | Run integration tests          |
      | test-e2e        | ./mvnw test -Dgroups="e2e"                               | Run E2E tests                  |
      | run             | ./mvnw spring-boot:run -Dspring-boot.run.profiles=local  | Run locally with stubs         |
      | docker-build    | docker build -t booking-service .                        | Build Docker image             |
      | docker-up       | docker-compose up -d                                     | Start all services             |
      | docker-down     | docker-compose down                                      | Stop all services              |
      | docker-logs     | docker-compose logs -f booking-service                   | Tail application logs          |
      | clean           | ./mvnw clean && docker-compose down -v                   | Clean build and volumes        |
      | swagger         | open http://localhost:8081/swagger-ui                     | Open Swagger UI in browser     |

  # ---------------------------------------------------------------------------
  # Summary: Full File Sequence
  # ---------------------------------------------------------------------------

  @deployment @summary
  Scenario: Complete file sequence reference
    Given all specification files have been processed
    Then the AI agent has built the Booking Service from the following sequence:
      | file                      | produces                                              |
      | 001_project_setup.txt     | Maven project, dependencies, packages, conventions    |
      | 002_domain_model.txt      | Entities, enums, validations, migrations              |
      | 003_data_access.md        | Repositories, queries, specifications                 |
      | 004_business_rules.md     | Services, events, clients, exceptions                 |
      | 005_api_endpoints.md      | Controllers, DTOs, mappers, OpenAPI                   |
      | 006_security.md           | JWT auth, roles, ownership, CORS                      |
      | 007_error_handling.md     | Global exception handler, error responses             |
      | 008_integrations.md       | Real clients, resilience, Kafka config, health        |
      | 009_testing.md            | Unit, integration, E2E tests, test utilities          |
      | 010_deployment.md         | Docker, Compose, CI, profiles, logging, README        |
    And the application should be fully runnable with "docker-compose up -d"
    And the API should be explorable at http://localhost:8081/swagger-ui
    And all tests should pass with "make test"
