# File: 008_integrations.md
# Depends on: 001_project_setup.md, 004_business_rules.md, 007_error_handling.md
# Produces: Integration configuration properties, RestClient infrastructure,
#           Resilience4j setup, health configuration, logging interceptor
# Context: Defines the integration patterns and infrastructure for external HTTP
#          services. Messaging/event streaming is intentionally out of scope for v1.
#          The external services (Schedules, Equipment, Quotes) are owned by other
#          teams and their API contracts are NOT yet known.

Feature: Integration Infrastructure
  As an AI code generator
  I need to set up HTTP integration infrastructure
  So that the Booking Service is ready to connect to external services when they become available

  Background:
    Given the base package is "com.cargo.booking"
    And configuration classes reside in "com.cargo.booking.config"
    And client classes reside in "com.cargo.booking.client"
    And the client interfaces and stubs are defined in 004_business_rules.md
    And the external services (Schedules, Equipment, Quotes) are owned by other teams
    And their API contracts are NOT finalized - only interfaces and stubs exist today

  # ---------------------------------------------------------------------------
  # IMPORTANT: What this file does NOT do
  # ---------------------------------------------------------------------------

  @integration @scope
  Scenario: Scope clarification
    Given the external services do not have finalized API contracts
    Then this file must NOT:
      | restriction                                                             |
      | Define specific REST endpoint paths for external services               |
      | Define response DTOs for external APIs (we do not know what they return) |
      | Implement real client classes - only stubs exist until contracts are agreed |
      | Assume the external services are Spring Boot or expose /actuator/health  |
      | Configure queues or any other messaging infrastructure in v1             |
    And this file DOES define:
      | responsibility                                                               |
      | Integration configuration property structure (base URLs, timeouts)            |
      | RestClient bean factory pattern (ready to configure when contracts are known) |
      | Resilience4j dependencies and default configuration                           |
      | HTTP client logging interceptor pattern                                       |
      | Actuator health endpoint configuration                                        |

  # ---------------------------------------------------------------------------
  # Integration Dependency - Resilience4j
  # ---------------------------------------------------------------------------

  @integration @setup
  Scenario: Add Resilience4j dependencies
    Given the pom.xml dependency section
    Then it must include the following additional dependencies (not listed in 001):
      | groupId                 | artifactId                 | purpose                    |
      | io.github.resilience4j | resilience4j-spring-boot4  | Circuit breaker and retry  |
      | io.github.resilience4j | resilience4j-circuitbreaker| Circuit breaker module     |
      | io.github.resilience4j | resilience4j-retry         | Retry module               |
      | org.springframework.boot | spring-boot-starter-aop   | AOP support for annotations|
    And the Spring Boot integration artifact must match the configured Spring Boot major version
    And these are included now so the infrastructure is ready when real clients are implemented

  # ---------------------------------------------------------------------------
  # Integration Configuration Properties
  # ---------------------------------------------------------------------------

  @integration @config
  Scenario: External service base URLs and timeouts
    Given the file "src/main/resources/application.yml"
    Then it must include placeholder integration properties:
      | property                              | value                                    | description               |
      | app.integration.schedule-api.base-url | ${SCHEDULE_API_URL:http://localhost:8082}| Schedules API base URL    |
      | app.integration.schedule-api.timeout-ms | ${SCHEDULE_API_TIMEOUT:5000}           | Connection + read timeout |
      | app.integration.equipment-api.base-url | ${EQUIPMENT_API_URL:http://localhost:8083} | Equipment API base URL |
      | app.integration.equipment-api.timeout-ms | ${EQUIPMENT_API_TIMEOUT:5000}         | Connection + read timeout |
      | app.integration.quote-api.base-url    | ${QUOTE_API_URL:http://localhost:8084}   | Quotes API base URL       |
      | app.integration.quote-api.timeout-ms  | ${QUOTE_API_TIMEOUT:5000}               | Connection + read timeout |
    And these are placeholders - actual URLs will be set when external services are deployed

  @integration @config
  Scenario: IntegrationProperties configuration class
    Given a class "IntegrationProperties" in package "com.cargo.booking.config"
    Then it must be annotated with @ConfigurationProperties(prefix = "app.integration")
    And it must define nested configuration for each external service:
      | nested class | fields                       |
      | ScheduleApi  | baseUrl (String), timeoutMs (int) |
      | EquipmentApi | baseUrl (String), timeoutMs (int) |
      | QuoteApi     | baseUrl (String), timeoutMs (int) |
    And this structure is ready to be injected into real client implementations when they are built
    And @EnableConfigurationProperties(IntegrationProperties.class) must be added to RestClientConfig or the main app class

  # ---------------------------------------------------------------------------
  # RestClient Pattern (Template for Future Implementations)
  # ---------------------------------------------------------------------------

  @integration @pattern
  Scenario: RestClient bean factory pattern
    Given a configuration class "RestClientConfig" in package "com.cargo.booking.config"
    Then it must be annotated with @Configuration
    And it must depend on IntegrationProperties
    And it must define three RestClient beans:
      | bean name           | base URL from                              | purpose             |
      | scheduleRestClient  | integrationProperties.scheduleApi.baseUrl  | Calls Schedules API |
      | equipmentRestClient | integrationProperties.equipmentApi.baseUrl | Calls Equipment API |
      | quoteRestClient     | integrationProperties.quoteApi.baseUrl     | Calls Quotes API    |
    And each RestClient must be configured with:
      | setting        | value                                     |
      | connectTimeout | From the corresponding timeoutMs property |
      | readTimeout    | From the corresponding timeoutMs property |
      | default header | Content-Type: application/json            |
      | default header | Accept: application/json                  |
    And timeout settings must be applied through a ClientHttpRequestFactory such as JdkClientHttpRequestFactory or HttpComponentsClientHttpRequestFactory
    And the logging interceptor (defined below) must be registered on each bean
    And these beans exist so that real client implementations can inject them by qualifier

  # ---------------------------------------------------------------------------
  # Resilience4j Default Configuration
  # ---------------------------------------------------------------------------

  @integration @resilience
  Scenario: Default circuit breaker configuration
    Given the file "src/main/resources/application.yml"
    Then it must include default Resilience4j circuit breaker settings:
      | property                                                                     | value | description                   |
      | resilience4j.circuitbreaker.configs.default.slidingWindowSize                | 10    | Number of calls in the window |
      | resilience4j.circuitbreaker.configs.default.failureRateThreshold             | 50    | Open circuit at 50% failure rate |
      | resilience4j.circuitbreaker.configs.default.waitDurationInOpenState          | 30s   | Wait before half-open         |
      | resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState | 3 | Test calls in half-open       |
    And these are DEFAULT configs - per-instance overrides can be added when real clients are built
    And a comment must note: "Override per instance when external API SLAs are known"

  @integration @resilience
  Scenario: Default retry configuration
    Given the file "src/main/resources/application.yml"
    Then it must include default Resilience4j retry settings:
      | property                                       | value | description        |
      | resilience4j.retry.configs.default.maxAttempts | 3     | Max retry attempts |
      | resilience4j.retry.configs.default.waitDuration | 500ms | Delay between retries |
    And a comment must note: "Retry exceptions should be configured per instance based on actual client errors"

  # ---------------------------------------------------------------------------
  # Integration Pattern Guide (for future implementers)
  # ---------------------------------------------------------------------------

  @integration @pattern
  Scenario: How to implement a real client when an external API contract is available
    Given a developer or agent is ready to implement a real client
    Then the implementation pattern must follow these steps:
      | step | action                                                                              |
      | 1    | Get the API contract (OpenAPI spec, documentation, or agreed endpoint list)          |
      | 2    | Create response DTOs in "com.cargo.booking.client.dto" as Java records              |
      | 3    | Use @JsonIgnoreProperties(ignoreUnknown = true) on all external DTOs                |
      | 4    | Create the implementation class annotated with @Service and @Profile("!local")      |
      | 5    | Inject the corresponding RestClient bean by @Qualifier                              |
      | 6    | Annotate methods with @CircuitBreaker and @Retry using a named instance             |
      | 7    | Implement fallback methods that throw the appropriate custom exception              |
      | 8    | Add per-instance Resilience4j config in application.yml if SLAs differ from defaults|
      | 9    | Add WireMock tests matching the real API contract                                   |
    And this pattern must be documented as a comment in the RestClientConfig class

  # ---------------------------------------------------------------------------
  # HTTP Client Logging Interceptor
  # ---------------------------------------------------------------------------

  @integration @logging
  Scenario: HTTP client logging interceptor
    Given a class "RestClientLoggingInterceptor" in package "com.cargo.booking.client"
    Then it must implement ClientHttpRequestInterceptor
    And it must log at DEBUG level:
      | log entry                                          |
      | Outbound request: {METHOD} {URL}                   |
      | Outbound request headers (excluding Authorization) |
      | Outbound response: {STATUS_CODE} in {elapsed_ms}ms |
    And it must NOT log request or response bodies at DEBUG (only at TRACE)
    And it must NOT log the Authorization header value (redact it)
    And this interceptor must be registered on all three RestClient beans

  # ---------------------------------------------------------------------------
  # Actuator Health Configuration
  # ---------------------------------------------------------------------------

  @integration @health
  Scenario: Actuator health endpoint configuration
    Given the file "src/main/resources/application.yml"
    Then it must include:
      | property                                | value           |
      | management.endpoint.health.show-details | when_authorized |
      | management.endpoint.health.show-components | when_authorized |
      | management.endpoint.health.roles        | ADMIN           |
    And unauthenticated users see only UP/DOWN status
    And ADMIN users see the full health breakdown
    And custom health indicators for external services should be added when real clients are implemented

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @integration @out-of-scope
  Scenario: Items NOT covered in integrations
    Given this is the integrations file only
    Then the following are NOT defined here and will be addressed later:
      | topic                                          | deferred to                               |
      | Real client implementations                    | When external API contracts are available |
      | External API response DTOs                     | When external API contracts are available |
      | Custom health indicators for external services | When external API contracts are available |
      | WireMock tests for real client implementations | When external API contracts are available |
      | Messaging / event streaming                    | Out of scope for v1                       |
      | OAuth2/OIDC token exchange with identity provider | Out of scope for v1                    |
      | Docker Compose for local external services     | Out of scope for v1                       |
      | Service mesh / service discovery               | Out of scope for v1                       |
