# File: 009_testing.md
# Depends on: 001_project_setup.md, 002_domain_model.md, 003_data_access.md,
#             004_business_rules.md, 005_api_endpoints.md, 006_security.md,
#             007_error_handling.md, 008_integrations.md
# Produces: Unit tests, integration tests, test configuration, test utilities, test data builders,
#           WireMock stubs, Testcontainers setup, MockMvc tests
# Context: Defines the complete testing strategy for the Cargo Booking Service. The AI agent
#          must have processed 001–008 so that all layers, exceptions, and integrations are known.

Feature: Testing Strategy
  As an AI code generator
  I need to create a comprehensive test suite for the Booking Service
  So that all layers are verified and regressions are caught early

  Background:
    Given the base test package is "com.cargo.booking"
    And all test classes reside under "src/test/java/com/cargo/booking"
    And the test profile "test" is activated via application-test.yml (defined in 001)
    And the test dependencies from 001 include spring-boot-starter-test and embedded PostgreSQL

  # ---------------------------------------------------------------------------
  # Additional Test Dependencies
  # ---------------------------------------------------------------------------

  @testing @setup
  Scenario: Test dependencies in pom.xml
    Given the pom.xml dependency section
    Then it must include the following test-scoped dependencies:
      | groupId                          | artifactId                           | scope | purpose                                |
      | org.springframework.boot         | spring-boot-starter-test             | test  | Core test support (JUnit 5, Mockito, AssertJ) |
      | org.springframework.security     | spring-security-test                 | test  | MockMvc security testing               |
      | org.springframework.kafka        | spring-kafka-test                    | test  | Kafka test utilities                   |
      | org.testcontainers               | testcontainers                       | test  | Testcontainers core                    |
      | org.testcontainers               | kafka                                | test  | Kafka Testcontainer                    |
      | org.testcontainers               | junit-jupiter                        | test  | JUnit 5 Testcontainers integration     |
      | org.wiremock                     | wiremock-standalone                   | test  | WireMock for external API mocking      |
      | io.zonky.test                    | embedded-database-spring-test         | test  | Spring test integration for embedded PostgreSQL |
    And the Testcontainers BOM must be managed in the dependencyManagement section

  # ---------------------------------------------------------------------------
  # Test Configuration
  # ---------------------------------------------------------------------------

  @testing @config
  Scenario: Test application properties
    Given the file "src/test/resources/application-test.yml"
    Then it must include (extending what 001 defines):
      | property                                      | value                       | purpose                              |
      | spring.datasource.url                         | Provided by embedded PostgreSQL test bootstrap | PostgreSQL-compatible test database |
      | spring.datasource.driver-class-name           | org.postgresql.Driver       | PostgreSQL driver                    |
      | spring.jpa.hibernate.ddl-auto                 | validate                    | Validate schema created by Flyway    |
      | spring.flyway.enabled                         | true                        | Run migrations in tests              |
      | spring.kafka.bootstrap-servers                | Provided by KafkaContainer test bootstrap | Kafka broker for tests |
      | app.security.jwt.secret                       | test-secret-key-that-is-at-least-256-bits-long-for-hs256 | Test JWT key |
      | app.security.jwt.issuer                       | test-issuer                 | Test JWT issuer                      |
      | app.security.jwt.expiration-ms                | 3600000                     | 1 hour for tests                     |

  @testing @config
  Scenario: Base integration test class with embedded PostgreSQL and KafkaContainer
    Given an abstract class "BaseIntegrationTest" in package "com.cargo.booking"
    Then it must be annotated with:
      | annotation                                                      |
      | @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) |
      | @ActiveProfiles("test")                                         |
      | @Testcontainers                                                 |
    And it must start an embedded PostgreSQL database shared by integration tests
    And it must define the following shared containers:
      | container                   | image                    | configuration                       |
      | KafkaContainer              | confluentinc/cp-kafka    | Default settings                    |
    And it must use @DynamicPropertySource to inject embedded PostgreSQL and Kafka connection details into Spring context
    And the embedded database and containers must be static (shared across all test classes extending this base)

  # ---------------------------------------------------------------------------
  # Test Utilities
  # ---------------------------------------------------------------------------

  @testing @utility
  Scenario: TestDataBuilder utility class
    Given a class "TestDataBuilder" in package "com.cargo.booking.testutil"
    Then it must provide static factory methods to create test entities and DTOs:
      | method                           | returns                 | description                                   |
      | aBooking()                       | Booking.BookingBuilder  | Pre-filled Booking builder with valid defaults|
      | aBookingWithStatus(BookingStatus) | Booking.BookingBuilder  | Builder with a specific status                |
      | anEquipmentLine()                | BookingEquipmentLine.BookingEquipmentLineBuilder | Default equipment line builder |
      | aCreateBookingRequest()          | CreateBookingRequest    | Valid request DTO with all required fields    |
      | aCustomerRequest()               | CustomerRequest         | Valid customer request                        |
      | aCargoRequest()                  | CargoRequest            | Valid cargo request                           |
      | anEquipmentRequest()             | EquipmentRequest        | Valid equipment request                       |
    And each method must use sensible defaults:
      | field            | default value                                |
      | bookingReference | "BKG-2026-00001"                              |
      | status           | PENDING                                       |
      | scheduleId       | 1001L                                           |
      | quoteId          | 2001L                                           |
      | customerId       | 3001L                                           |
      | customerName     | "Test Customer"                                |
      | customerEmail    | "test@example.com"                            |
      | cargoDescription | "Test cargo"                                   |
      | cargoWeightKg    | BigDecimal.valueOf(1000)                      |
      | equipmentType    | "20FT"                                        |
      | equipmentQty     | 1                                              |

  @testing @utility
  Scenario: JwtTestHelper utility class
    Given a class "JwtTestHelper" in package "com.cargo.booking.testutil"
    Then it must provide methods to generate valid JWT tokens for testing:
      | method                                                                 | returns | description                       |
      | generateToken(Long userId, String username, List<String> roles)       | String  | Valid JWT token for test user      |
      | generateCustomerToken(Long userId)                                    | String  | Token with ROLE_CUSTOMER           |
      | generateOperatorToken(Long userId)                                    | String  | Token with ROLE_OPERATOR           |
      | generateAdminToken(Long userId)                                       | String  | Token with ROLE_ADMIN              |
      | generateExpiredToken(Long userId)                                     | String  | Expired JWT for negative tests     |
    And it must use the test JWT secret from application-test.yml
    And it must set the issuer to "test-issuer"

  # ---------------------------------------------------------------------------
  # Unit Tests — Domain Model
  # ---------------------------------------------------------------------------

  @testing @unit @domain
  Scenario: BookingStatus state transition tests
    Given a test class "BookingStatusTest" in package "com.cargo.booking.model.enums"
    Then it must verify all valid transitions from 002_domain_model.md:
      | test method                             | from        | to          | expected |
      | shouldAllowPendingToConfirmed()         | PENDING     | CONFIRMED   | allowed  |
      | shouldAllowPendingToCancelled()         | PENDING     | CANCELLED   | allowed  |
      | shouldAllowConfirmedToInProgress()      | CONFIRMED   | IN_PROGRESS | allowed  |
      | shouldAllowConfirmedToCancelled()       | CONFIRMED   | CANCELLED   | allowed  |
      | shouldAllowInProgressToCompleted()      | IN_PROGRESS | COMPLETED   | allowed  |
    And it must verify all invalid transitions throw IllegalStateTransitionException:
      | test method                              | from        | to          |
      | shouldRejectPendingToCompleted()         | PENDING     | COMPLETED   |
      | shouldRejectPendingToInProgress()        | PENDING     | IN_PROGRESS |
      | shouldRejectConfirmedToPending()         | CONFIRMED   | PENDING     |
      | shouldRejectInProgressToCancelled()      | IN_PROGRESS | CANCELLED   |
      | shouldRejectCompletedToAnything()        | COMPLETED   | CANCELLED   |
      | shouldRejectCancelledToAnything()        | CANCELLED   | PENDING     |

  @testing @unit @domain
  Scenario: BookingReferenceGenerator tests
    Given a test class "BookingReferenceGeneratorTest" in package "com.cargo.booking.service"
    Then it must use Mockito to mock the BookingRepository
    And it must verify:
      | test method                               | description                                        |
      | shouldGenerateReferenceWithCurrentYear()  | Reference starts with "BKG-{currentYear}-"          |
      | shouldPadSequenceTo5Digits()              | Sequence 1 becomes "00001", 42 becomes "00042"      |
      | shouldUseSequenceFromRepository()         | The sequence value comes from the mocked repository |

  # ---------------------------------------------------------------------------
  # Unit Tests — Service Layer
  # ---------------------------------------------------------------------------

  @testing @unit @service
  Scenario: BookingService.createBooking() tests
    Given a test class "BookingServiceCreateTest" in package "com.cargo.booking.service"
    Then it must use @ExtendWith(MockitoExtension.class)
    And it must mock:
      | dependency                | purpose                                 |
      | BookingRepository         | Verify save is called with correct data |
      | BookingReferenceGenerator | Return predictable references           |
      | BookingEventPublisher     | Verify event is published               |
      | ScheduleClient            | Control schedule validation result      |
      | EquipmentClient           | Not called during creation              |
      | QuoteClient               | Control quote validation result         |
      | BookingMapper             | Return predictable mapped entities      |
    And it must include the following test cases:
      | test method                                            | scenario                                         |
      | shouldCreateBookingSuccessfully()                     | Happy path — valid request, all validations pass  |
      | shouldSetStatusToPending()                            | New booking always starts as PENDING              |
      | shouldGenerateUniqueReference()                       | Reference comes from BookingReferenceGenerator    |
      | shouldPublishBookingCreatedEvent()                    | Verify event publisher is called after save       |
      | shouldThrowWhenScheduleNotAvailable()                 | ScheduleClient returns false                     |
      | shouldThrowWhenQuoteNotValid()                        | QuoteClient returns false                        |
      | shouldThrowWhenEquipmentListEmpty()                   | Request with empty equipment list                |
      | shouldNotPersistBookingWhenValidationFails()          | Verify repository.save() is never called on failure |
      | shouldNotPublishEventWhenValidationFails()            | Verify publisher is never called on failure       |

  @testing @unit @service
  Scenario: BookingService state transition tests
    Given a test class "BookingServiceLifecycleTest" in package "com.cargo.booking.service"
    Then it must use @ExtendWith(MockitoExtension.class)
    And it must include the following test cases:
      | test method                                            | scenario                                         |
      | shouldConfirmPendingBooking()                         | PENDING → CONFIRMED happy path                    |
      | shouldReserveEquipmentOnConfirmation()                | Verify EquipmentClient.reserveEquipment() called  |
      | shouldPublishConfirmedEvent()                         | Verify event published after confirmation         |
      | shouldThrowWhenConfirmingNonPendingBooking()          | CONFIRMED → CONFIRMED should fail                 |
      | shouldNotChangeStatusWhenEquipmentReservationFails()  | Status remains PENDING if equipment fails         |
      | shouldStartConfirmedBooking()                         | CONFIRMED → IN_PROGRESS happy path                |
      | shouldCompleteInProgressBooking()                     | IN_PROGRESS → COMPLETED happy path                |
      | shouldCancelPendingBooking()                          | PENDING → CANCELLED without equipment release     |
      | shouldCancelConfirmedBooking()                        | CONFIRMED → CANCELLED with equipment release      |
      | shouldCancelEvenWhenEquipmentReleaseFails()           | Cancellation proceeds despite release failure     |
      | shouldPublishCancelledEvent()                         | Verify event published after cancellation         |
      | shouldThrowWhenCancellingCompletedBooking()           | COMPLETED → CANCELLED should fail                 |

  @testing @unit @service
  Scenario: BookingService read operation tests
    Given a test class "BookingServiceReadTest" in package "com.cargo.booking.service"
    Then it must include the following test cases:
      | test method                                    | scenario                                          |
      | shouldGetBookingById()                        | Returns booking when found                         |
      | shouldThrowNotFoundWhenBookingIdMissing()     | Throws BookingNotFoundException for unknown ID     |
      | shouldGetBookingByReference()                 | Returns booking when found by reference            |
      | shouldThrowNotFoundWhenReferenceMissing()     | Throws BookingNotFoundException for unknown ref    |
      | shouldReturnPagedBookingsForCustomer()        | Returns paginated results                          |
      | shouldFilterByCustomerAndStatus()             | Pagination with status filter                      |

  # ---------------------------------------------------------------------------
  # Unit Tests — Mapper
  # ---------------------------------------------------------------------------

  @testing @unit @mapper
  Scenario: BookingMapper tests
    Given a test class "BookingMapperTest" in package "com.cargo.booking.mapper"
    Then it must verify:
      | test method                                        | description                                     |
      | shouldMapRequestToEntity()                        | All fields correctly mapped from DTO to entity    |
      | shouldSetStatusToPendingOnMapping()               | Status is always PENDING in mapped entity         |
      | shouldMapEntityToResponse()                       | All fields correctly mapped from entity to DTO    |
      | shouldMapEntityToCreatedResponse()                | Slim response has reference, status, createdAt    |
      | shouldMapEquipmentLinesToResponse()               | Equipment lines mapped correctly                  |
      | shouldHandleNullPhoneNumber()                     | Null phone is preserved without NPE               |

  # ---------------------------------------------------------------------------
  # Integration Tests — Repository Layer
  # ---------------------------------------------------------------------------

  @testing @integration @repository
  Scenario: BookingRepository integration tests
    Given a test class "BookingRepositoryTest" in package "com.cargo.booking.repository"
    Then it must be annotated with @DataJpaTest and @ActiveProfiles("test")
    And it must use @AutoConfigureTestDatabase(replace = Replace.NONE) if Testcontainers is used
    And it must include the following test cases:
      | test method                                          | description                                        |
      | shouldSaveAndFindBookingById()                      | Save a booking, retrieve by ID, verify fields       |
      | shouldFindByBookingReference()                      | Lookup by human-readable reference                  |
      | shouldReturnEmptyForUnknownReference()              | Optional.empty() for non-existent reference         |
      | shouldFindByCustomerIdPaginated()                   | Paginated results filtered by customerId            |
      | shouldFindByCustomerIdAndStatus()                   | Filtered by both customer and status                |
      | shouldFindByStatus()                                | Filtered by status with pagination                  |
      | shouldCheckExistsByBookingReference()               | existsBy returns true for existing, false otherwise |
      | shouldFetchBookingWithEquipmentLines()              | JOIN FETCH loads equipment eagerly                  |
      | shouldCascadeSaveEquipmentLines()                   | Saving booking cascades to equipment lines          |
      | shouldCascadeDeleteEquipmentLines()                 | Orphan removal works when lines are cleared         |

  # ---------------------------------------------------------------------------
  # Integration Tests — Controller Layer (MockMvc)
  # ---------------------------------------------------------------------------

  @testing @integration @controller
  Scenario: BookingController MockMvc test setup
    Given a test class "BookingControllerTest" in package "com.cargo.booking.controller"
    Then it must be annotated with:
      | annotation                                                              |
      | @WebMvcTest(BookingController.class)                                    |
      | @ActiveProfiles("test")                                                 |
      | @Import(SecurityConfig.class)                                           |
    And it must use @MockBean for:
      | dependency        | purpose                                   |
      | BookingService    | Mock business logic                       |
      | BookingMapper     | Mock entity-DTO conversion                |
      | JwtTokenProvider  | Mock JWT validation for security context  |
    And it must use JwtTestHelper to generate tokens for authenticated requests

  @testing @integration @controller
  Scenario: POST /api/v1/bookings controller tests
    Given the BookingControllerTest class
    Then it must include the following MockMvc test cases:
      | test method                                          | status | description                                    |
      | shouldCreateBookingAndReturn201()                   | 201    | Valid request, authenticated as CUSTOMER         |
      | shouldReturn400WhenRequestBodyInvalid()             | 400    | Missing required fields                          |
      | shouldReturn400WhenEmailInvalid()                   | 400    | Malformed email in customer request              |
      | shouldReturn400WhenEquipmentListEmpty()             | 400    | Empty equipment array                            |
      | shouldReturn401WhenNoAuthToken()                    | 401    | Request without Authorization header             |
      | shouldReturn403WhenOperatorTriesToCreate()          | 403    | OPERATOR role cannot create bookings             |
      | shouldReturn422WhenScheduleNotAvailable()           | 422    | Service throws ScheduleNotAvailableException     |
      | shouldReturn422WhenQuoteNotValid()                  | 422    | Service throws QuoteNotValidException            |

  @testing @integration @controller
  Scenario: GET /api/v1/bookings/{id} controller tests
    Given the BookingControllerTest class
    Then it must include the following MockMvc test cases:
      | test method                                          | status | description                                    |
      | shouldGetBookingByIdAndReturn200()                  | 200    | Valid numeric ID, booking found                  |
      | shouldGetBookingByReferenceAndReturn200()           | 200    | Valid BKG reference, booking found               |
      | shouldReturn404WhenBookingNotFound()                | 404    | Unknown ID returns 404                           |
      | shouldReturn401WhenNotAuthenticated()               | 401    | No token                                         |

  @testing @integration @controller
  Scenario: GET /api/v1/bookings controller tests
    Given the BookingControllerTest class
    Then it must include the following MockMvc test cases:
      | test method                                          | status | description                                    |
      | shouldListBookingsByCustomerAndReturn200()          | 200    | Valid customerId, paginated results              |
      | shouldReturn400WhenCustomerIdMissing()              | 400    | Required query param missing                     |
      | shouldFilterByStatus()                              | 200    | customerId + status filter                       |

  @testing @integration @controller
  Scenario: PATCH /api/v1/bookings/{id}/cancel controller tests
    Given the BookingControllerTest class
    Then it must include the following MockMvc test cases:
      | test method                                          | status | description                                    |
      | shouldCancelBookingAndReturn200()                   | 200    | Valid cancel request by CUSTOMER                 |
      | shouldReturn409WhenInvalidStateTransition()         | 409    | Service throws IllegalStateTransitionException   |
      | shouldReturn403WhenOperatorTriesToCancel()          | 403    | OPERATOR cannot cancel                           |

  @testing @integration @controller
  Scenario: PATCH lifecycle endpoint controller tests
    Given the BookingControllerTest class
    Then it must include the following MockMvc test cases:
      | test method                                          | status | description                                    |
      | shouldConfirmBookingAsOperator()                    | 200    | OPERATOR confirms PENDING booking                |
      | shouldStartBookingAsOperator()                      | 200    | OPERATOR starts CONFIRMED booking                |
      | shouldCompleteBookingAsOperator()                   | 200    | OPERATOR completes IN_PROGRESS booking           |
      | shouldReturn403WhenCustomerTriesToConfirm()         | 403    | CUSTOMER cannot confirm                          |

  # ---------------------------------------------------------------------------
  # Integration Tests — Error Handling
  # ---------------------------------------------------------------------------

  @testing @integration @error
  Scenario: GlobalExceptionHandler integration tests
    Given a test class "ErrorHandlingTest" in package "com.cargo.booking.exception"
    Then it must be annotated with @WebMvcTest and test the error response format
    And it must verify:
      | test method                                        | description                                          |
      | shouldReturnStructuredErrorFor404()               | Error body matches ErrorResponse format               |
      | shouldReturnValidationErrorsWithFieldDetails()    | 400 response includes violations list                 |
      | shouldReturnSortedFieldViolations()               | Violations are alphabetically ordered                 |
      | shouldNotExposeInternalDetailsIn500()              | Generic message for unexpected exceptions             |
      | shouldIncludeRequestPathInError()                 | Path field matches the request URI                    |
      | shouldIncludeRequestIdWhenProvided()              | X-Request-ID header is reflected in error response    |

  # ---------------------------------------------------------------------------
  # Integration Tests — Security
  # ---------------------------------------------------------------------------

  @testing @integration @security
  Scenario: Security integration tests
    Given a test class "SecurityIntegrationTest" in package "com.cargo.booking.security"
    Then it must extend BaseIntegrationTest
    And it must verify:
      | test method                                              | description                                      |
      | shouldAllowAccessToSwaggerWithoutAuth()                 | /swagger-ui/** is public                          |
      | shouldAllowAccessToHealthWithoutAuth()                  | /actuator/health is public                        |
      | shouldRejectRequestWithExpiredToken()                   | 401 for expired JWT                               |
      | shouldRejectRequestWithMalformedToken()                 | 401 for garbage token                             |
      | shouldRejectRequestWithWrongIssuer()                    | 401 for wrong issuer claim                        |
      | shouldEnforceCustomerOwnershipOnGetBooking()            | CUSTOMER can only see own bookings                |
      | shouldEnforceCustomerOwnershipOnCancel()                | CUSTOMER can only cancel own bookings             |
      | shouldAllowOperatorToAccessAnyBooking()                 | OPERATOR can view any booking                     |
      | shouldAllowAdminFullAccess()                            | ADMIN can access all endpoints                    |

  # ---------------------------------------------------------------------------
  # Integration Tests — Kafka Events
  # ---------------------------------------------------------------------------

  @testing @integration @kafka
  Scenario: Kafka event publishing integration tests
    Given a test class "BookingEventPublisherTest" in package "com.cargo.booking.event"
    Then it must use KafkaContainer from Testcontainers
    And it must verify:
      | test method                                           | description                                        |
      | shouldPublishBookingCreatedEvent()                   | Event published to "booking.created" topic           |
      | shouldPublishBookingConfirmedEvent()                 | Event published to "booking.confirmed" topic         |
      | shouldPublishBookingCancelledEvent()                 | Event published to "booking.cancelled" topic         |
      | shouldPublishBookingCompletedEvent()                 | Event published to "booking.completed" topic         |
      | shouldUseBookingReferenceAsMessageKey()              | Kafka message key is the bookingReference            |
      | shouldSerializeEventPayloadAsJson()                  | Message value is valid JSON with expected fields     |
    And each test must consume from the topic using a test KafkaConsumer to verify the message

  # ---------------------------------------------------------------------------
  # Integration Tests — External Service Clients (Deferred)
  # ---------------------------------------------------------------------------

  @testing @integration @wiremock
  Scenario: WireMock tests for external service clients
    Given the external services (Schedules, Equipment, Quotes) do not have finalized API contracts
    Then WireMock-based integration tests for real client implementations are DEFERRED
    And they must be written when the external API contracts are available
    And the test infrastructure must be prepared now:
      | preparation                                                                    |
      | WireMock dependency is included in pom.xml (test scope)                        |
      | A base class "BaseWireMockTest" must be created in "com.cargo.booking.client"  |
      | It must configure WireMock servers on dynamic ports for each external service   |
      | Test application properties must override base URLs to point to WireMock       |
    And when contracts are known, tests should cover at minimum:
      | scenario type                                         |
      | Happy path — external service returns success         |
      | Not found — external service returns 404              |
      | Server error — external service returns 500           |
      | Timeout — external service does not respond in time   |
      | Circuit breaker opens after repeated failures         |

  # ---------------------------------------------------------------------------
  # End-to-End Tests
  # ---------------------------------------------------------------------------

  @testing @e2e
  Scenario: Full booking lifecycle end-to-end test
    Given a test class "BookingLifecycleE2ETest" in package "com.cargo.booking"
    Then it must extend BaseIntegrationTest (embedded PostgreSQL and KafkaContainer)
    And it must use the "local" profile so that stub clients are active (no real external services)
    And it must use TestRestTemplate or WebTestClient with a real JWT token
    And it must verify the complete happy path:
      | step | action                                                     | expected                           |
      | 1    | POST /api/v1/bookings with valid request as CUSTOMER       | 201, status=PENDING, reference set |
      | 2    | GET /api/v1/bookings/{id} as CUSTOMER                      | 200, full booking details          |
      | 3    | PATCH /api/v1/bookings/{id}/confirm as OPERATOR            | 200, status=CONFIRMED              |
      | 4    | Verify "booking.confirmed" event on Kafka topic            | Event payload matches booking      |
      | 5    | PATCH /api/v1/bookings/{id}/start as OPERATOR              | 200, status=IN_PROGRESS            |
      | 6    | PATCH /api/v1/bookings/{id}/complete as OPERATOR           | 200, status=COMPLETED              |
      | 7    | Verify "booking.completed" event on Kafka topic            | Event payload matches booking      |
      | 8    | GET /api/v1/bookings?customerId={id} as CUSTOMER           | 200, list includes this booking    |

  @testing @e2e
  Scenario: Cancellation flow end-to-end test
    Given the BookingLifecycleE2ETest class
    Then it must also verify the cancellation flow:
      | step | action                                                     | expected                            |
      | 1    | POST /api/v1/bookings with valid request as CUSTOMER       | 201, status=PENDING                 |
      | 2    | PATCH /api/v1/bookings/{id}/cancel as CUSTOMER             | 200, status=CANCELLED               |
      | 3    | Verify "booking.cancelled" event on Kafka topic            | Event payload matches booking       |
      | 4    | PATCH /api/v1/bookings/{id}/confirm as OPERATOR            | 409, cannot confirm cancelled booking|

  # ---------------------------------------------------------------------------
  # Test Naming and Organization Conventions
  # ---------------------------------------------------------------------------

  @testing @conventions
  Scenario: Test naming and structure conventions
    Given all test classes
    Then the following conventions must apply:
      | rule                                                                                        |
      | Test method names use should...() pattern (e.g. shouldCreateBookingSuccessfully())          |
      | Each test method tests exactly one behavior                                                 |
      | Tests follow Arrange-Act-Assert (AAA) or Given-When-Then structure internally               |
      | Unit tests use @DisplayName for human-readable descriptions                                 |
      | Integration tests are tagged with @Tag("integration") for selective execution               |
      | E2E tests are tagged with @Tag("e2e") for selective execution                               |
      | No test should depend on the execution order of other tests                                 |
      | Each test must clean up its own data or rely on transaction rollback                         |

  @testing @conventions
  Scenario: Test coverage targets
    Given the Booking Service test suite
    Then the following coverage targets should be aimed for:
      | layer          | target   | notes                                          |
      | Service layer  | 90%+     | Core business logic must be thoroughly tested  |
      | Controller     | 85%+     | All endpoints and error paths                  |
      | Repository     | 80%+     | Custom queries and specifications              |
      | Mapper         | 90%+     | All mapping paths including edge cases         |
      | Security       | 85%+     | Auth, roles, and ownership checks              |
      | Integration    | 75%+     | Client implementations with WireMock           |
    And these are targets, not hard gates — the AI should aim for them but not generate filler tests

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @testing @out-of-scope
  Scenario: Items NOT covered in testing
    Given this is the testing file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                                    | deferred to           |
      | Docker Compose for test infrastructure   | 010_deployment.md     |
      | Performance / load testing               | Out of scope for v1   |
      | Contract testing (Pact / Spring Cloud)   | Out of scope for v1   |
      | Mutation testing                         | Out of scope for v1   |
