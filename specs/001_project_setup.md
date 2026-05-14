# File: 001_project_setup.md
# Depends on: None (this is the foundation file)
# Produces: Maven project structure, pom.xml, application configuration, base packages
# Context: Cargo Booking Service — a microservice that manages cargo booking requests,
#          lifecycle state transitions, and emits domain events for downstream consumers.

Feature: Project Setup and Configuration
  As an AI code generator
  I need to scaffold a Spring Boot microservice for the Booking Service
  So that all subsequent features have a consistent foundation to build on

  Background:
    Given the application name is "booking-service"
    And the group ID is "com.cargo"
    And the artifact ID is "booking-service"
    And the base package is "com.cargo.booking"

  # ---------------------------------------------------------------------------
  # Build Tool & Language
  # ---------------------------------------------------------------------------

  @setup @build
  Scenario: Maven project with Java 21
    Given I am creating a new Spring Boot project
    Then the build tool must be Maven
    And the Java version must be 21 (latest LTS)
    And the Spring Boot version must be 4.0.6
    And the pom.xml must set <maven.compiler.source> and <maven.compiler.target> to 21
    And the packaging must be "jar"

  # ---------------------------------------------------------------------------
  # Dependencies
  # ---------------------------------------------------------------------------

  @setup @dependencies
  Scenario: Core Spring Boot starters
    Given the pom.xml dependency section
    Then it must include the following starters:
      | groupId                        | artifactId                         | purpose                            |
      | org.springframework.boot       | spring-boot-starter-web            | REST API support                   |
      | org.springframework.boot       | spring-boot-starter-data-jpa       | JPA / Hibernate persistence        |
      | org.springframework.boot       | spring-boot-starter-validation     | Bean validation (Jakarta)          |
      | org.springframework.boot       | spring-boot-starter-actuator       | Health checks and metrics          |
      | org.springframework.kafka       | spring-kafka                       | Kafka for domain event emission    |

  @setup @dependencies
  Scenario: Database dependencies
    Given the pom.xml dependency section
    Then it must include:
      | groupId          | artifactId         | scope    | purpose                          |
      | org.postgresql   | postgresql         | runtime  | PostgreSQL JDBC driver           |
      | org.flywaydb     | flyway-core        | compile  | Database migration management    |
      | org.flywaydb     | flyway-database-postgresql | compile | Flyway PostgreSQL support |
      | io.zonky.test    | embedded-postgres  | test     | Embedded PostgreSQL for tests    |

  @setup @dependencies
  Scenario: Utility and documentation dependencies
    Given the pom.xml dependency section
    Then it must include:
      | groupId                 | artifactId               | scope    | purpose                     |
      | org.projectlombok       | lombok                  | provided | Reduce boilerplate code     |
      | org.springdoc           | springdoc-openapi-starter-webmvc-ui | compile | OpenAPI / Swagger UI |
      | org.springframework.boot | spring-boot-starter-test | test    | Testing support             |

  # ---------------------------------------------------------------------------
  # Package Structure
  # ---------------------------------------------------------------------------

  @setup @structure
  Scenario: Layered package architecture
    Given the base package is "com.cargo.booking"
    Then the project must have the following package structure:
      | package                          | purpose                                      |
      | com.cargo.booking                | Main application class                       |
      | com.cargo.booking.controller     | REST controllers                             |
      | com.cargo.booking.service        | Business logic and orchestration              |
      | com.cargo.booking.repository     | Spring Data JPA repository interfaces         |
      | com.cargo.booking.model.entity   | JPA entity classes                            |
      | com.cargo.booking.model.enums    | Enums (e.g. BookingStatus)                   |
      | com.cargo.booking.dto.request    | Inbound request DTOs                         |
      | com.cargo.booking.dto.response   | Outbound response DTOs                       |
      | com.cargo.booking.event          | Domain event classes and publisher            |
      | com.cargo.booking.config         | Spring configuration beans                   |
      | com.cargo.booking.exception      | Custom exceptions and global error handler   |
      | com.cargo.booking.client         | Feign/REST clients for external services     |
      | com.cargo.booking.mapper         | Entity-to-DTO mapping logic                  |

  # ---------------------------------------------------------------------------
  # Application Configuration
  # ---------------------------------------------------------------------------

  @setup @config
  Scenario: Application properties for local development
    Given the file "src/main/resources/application.yml"
    Then it must contain the following configuration:
      | property                                    | value                                      |
      | server.port                                 | 8081                                       |
      | spring.application.name                     | booking-service                            |
      | spring.datasource.url                       | jdbc:postgresql://localhost:5432/booking_db |
      | spring.datasource.username                  | ${DB_USERNAME:booking_user}                |
      | spring.datasource.password                  | ${DB_PASSWORD:booking_pass}                |
      | spring.jpa.hibernate.ddl-auto               | validate                                   |
      | spring.jpa.properties.hibernate.dialect      | org.hibernate.dialect.PostgreSQLDialect    |
      | spring.jpa.open-in-view                     | false                                      |
      | spring.flyway.enabled                       | true                                       |
      | spring.flyway.locations                     | classpath:db/migration                     |
      | spring.kafka.bootstrap-servers              | ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092} |
      | spring.kafka.producer.key-serializer        | org.apache.kafka.common.serialization.StringSerializer |
      | spring.kafka.producer.value-serializer      | org.springframework.kafka.support.serializer.JsonSerializer |
      | spring.kafka.consumer.group-id              | booking-service-group                    |
      | spring.kafka.consumer.auto-offset-reset     | earliest                                 |
      | spring.kafka.consumer.key-deserializer      | org.apache.kafka.common.serialization.StringDeserializer |
      | spring.kafka.consumer.value-deserializer    | org.springframework.kafka.support.serializer.JsonDeserializer |
      | springdoc.api-docs.path                     | /api-docs                                  |
      | springdoc.swagger-ui.path                   | /swagger-ui                                |
      | management.endpoints.web.exposure.include   | health,info,metrics                        |

  @setup @config
  Scenario: Application properties for test profile
    Given the file "src/test/resources/application-test.yml"
    Then it must override the following for tests:
      | property                       | value                        |
      | spring.datasource.url          | Provided by embedded PostgreSQL test bootstrap |
      | spring.datasource.driver-class-name | org.postgresql.Driver   |
      | spring.jpa.hibernate.ddl-auto  | validate                     |
      | spring.flyway.enabled          | true                         |
      | spring.kafka.bootstrap-servers | ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092} |

  # ---------------------------------------------------------------------------
  # Main Application Class
  # ---------------------------------------------------------------------------

  @setup @entrypoint
  Scenario: Spring Boot main class
    Given the base package is "com.cargo.booking"
    Then a class "BookingServiceApplication" must exist in the base package
    And it must be annotated with @SpringBootApplication
    And it must contain a standard main method that calls SpringApplication.run()

  # ---------------------------------------------------------------------------
  # Conventions & Constraints
  # ---------------------------------------------------------------------------

  @setup @conventions
  Scenario: Coding conventions the AI agent must follow
    Given I am generating code for this project
    Then I must follow these conventions:
      | rule                                                                                 |
      | Use Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)    |
      | Use constructor injection (not field injection) for all Spring beans                  |
      | All REST endpoints must be prefixed with "/api/v1"                                   |
      | All timestamps must be stored and returned in UTC using ISO-8601 format               |
      | Entity IDs must be Long values generated with @GeneratedValue(strategy = GenerationType.IDENTITY) |
      | Use records for DTOs where the DTO is immutable                                       |
      | Never expose JPA entities directly in API responses — always map to DTOs              |
      | Database table names must be lowercase_snake_case                                     |
      | Boolean fields must not be prefixed with "is" at the entity level                     |
      | Use SLF4J logging for meaningful business events, state transitions, external calls, and failures; avoid mechanical logging on every method |

  @setup @conventions
  Scenario: Error response structure convention
    Given any error returned by the API
    Then the response body must follow this JSON structure:
      """
      {
        "timestamp": "ISO-8601 UTC",
        "status": "HTTP status code (int)",
        "error": "HTTP reason phrase",
        "message": "Human-readable description",
        "path": "Request URI"
      }
      """

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @setup @out-of-scope
  Scenario: Items NOT covered in project setup
    Given this is the project setup file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                        | deferred to           |
      | Entity field definitions     | 002_domain_model.md  |
      | Repository interfaces        | 003_data_access.md   |
      | Service layer logic          | 004_business_rules.md|
      | Controller implementations   | 005_api_endpoints.md |
      | Security configuration       | 006_security.md      |
      | Error handling details       | 007_error_handling.md|
      | External service clients     | 008_integrations.md  |
      | Test scenarios               | 009_testing.md       |
      | Docker / deployment config   | 010_deployment.md    |
