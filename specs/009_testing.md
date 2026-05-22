# Testing Strategy

## Metadata

| Field | Value |
| --- | --- |
| File | 009_testing.md |
| Depends on | 001_project_setup.md, 002_domain_model.md, 003_data_access.md, 004_business_rules.md, 005_api_endpoints.md, 006_security.md, 007_error_handling.md, 008_integrations.md |
| Produces | Unit tests, integration tests, test configuration, test utilities, test data builders, WireMock stubs, embedded PostgreSQL setup, MockMvc tests |
| Context | Defines the complete testing strategy for the Cargo Booking Service. The AI agent must have processed 001–008 so that all layers, exceptions, and integrations are known. |

## Goal

- As an AI code generator
- I need to create a comprehensive test suite for the Booking Service
- So that all layers are verified and regressions are caught early

## Background

- Given the base test package is "com.cargo.booking"
- And all test classes reside under "src/test/java/com/cargo/booking"
- And the test profile "test" uses application-test.yml properties when activated by test annotations such as @ActiveProfiles("test")
- And the test dependencies from 001 include spring-boot-starter-test and embedded PostgreSQL

## Additional Test Dependencies

### Test dependencies in pom.xml

Tags: `testing`, `setup`

- Given the pom.xml dependency section
- Then it must include the following additional test-scoped dependencies not already listed in 001_project_setup.md:

| groupId                          | artifactId                           | scope | purpose                                |
| --- | --- | --- | --- |
| org.springframework.security     | spring-security-test                 | test  | MockMvc security testing               |
| org.wiremock                     | wiremock-standalone                   | test  | WireMock for external API mocking      |

## Test Configuration

### Test application properties

Tags: `testing`, `config`

- Given the file "src/test/resources/application-test.yml"
- Then it must include (extending what 001 defines):

| property                                      | value                       | purpose                              |
| --- | --- | --- |
| spring.datasource.url                         | Provided by embedded PostgreSQL test bootstrap | PostgreSQL-compatible test database |
| spring.datasource.driver-class-name           | org.postgresql.Driver       | PostgreSQL driver                    |
| spring.jpa.hibernate.ddl-auto                 | validate                    | Validate schema created by Flyway    |
| spring.flyway.enabled                         | true                        | Run migrations in tests              |
| app.security.enabled                          | true                        | Security is enabled by default in tests |
| app.security.jwt.secret                       | test-secret-key-that-is-at-least-256-bits-long-for-hs256 | Test JWT key |
| app.security.jwt.issuer                       | test-issuer                 | Test JWT issuer                      |
| app.security.jwt.expiration-ms                | 3600000                     | 1 hour for tests                     |

### Base integration test class with embedded PostgreSQL

Tags: `testing`, `config`

- Given an abstract class "BaseIntegrationTest" in package "com.cargo.booking"
- Then it must be annotated with:

| annotation                                                      |
| --- |
| @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) |
| @ActiveProfiles("test")                                         |

- And it must start an embedded PostgreSQL database shared by integration tests
- And it must use @DynamicPropertySource to inject embedded PostgreSQL connection details into Spring context
- And the embedded database must be static (shared across all test classes extending this base)
- And tests that need local stub clients must activate both "test" and "local" profiles with @ActiveProfiles({"test", "local"})

## Test Utilities

### TestDataBuilder utility class

Tags: `testing`, `utility`

- Given a class "TestDataBuilder" in package "com.cargo.booking.testutil"
- Then it must provide static factory methods to create test entities and DTOs:

| method                           | returns                 | description                                   |
| --- | --- | --- |
| aBooking()                       | Booking.BookingBuilder  | Pre-filled Booking builder with valid defaults|
| aBookingWithStatus(BookingStatus) | Booking.BookingBuilder  | Builder with a specific status                |
| anEquipmentLine()                | BookingEquipmentLine.BookingEquipmentLineBuilder | Default equipment line builder |
| aCreateBookingRequest()          | CreateBookingRequest    | Valid request DTO with all required fields    |
| aCustomerRequest()               | CustomerRequest         | Valid customer request                        |
| aCargoRequest()                  | CargoRequest            | Valid cargo request                           |
| anEquipmentRequest()             | EquipmentRequest        | Valid equipment request                       |

- And each method must use sensible defaults:

| field            | default value                                |
| --- | --- |
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

### JwtTestHelper utility class

Tags: `testing`, `utility`

- Given a class "JwtTestHelper" in package "com.cargo.booking.testutil"
- Then it must provide methods to generate valid JWT tokens for testing:

| method                                                                 | returns | description                       |
| --- | --- | --- |
| generateToken(String subject, String username, List<String> roles)    | String  | Valid JWT token for requester      |
| generateCustomerToken(Long customerId)                               | String  | Token with ROLE_CUSTOMER and customerId claim |
| generateServiceToken(String serviceName)                             | String  | Token with ROLE_SERVICE and no customerId claim |
| generateOperatorToken(String subject)                                | String  | Token with ROLE_OPERATOR           |
| generateAdminToken(String subject)                                   | String  | Token with ROLE_ADMIN              |
| generateExpiredToken(String subject)                                 | String  | Expired JWT for negative tests     |

- And it must use the test JWT secret from application-test.yml
- And it must set the issuer to "test-issuer"

### BookingAccessAuthorizer tests

Tags: `testing`, `unit`, `security`

- Given a test class "BookingAccessAuthorizerTest" in package "com.cargo.booking.security"
- Then it must use @ExtendWith(MockitoExtension.class)
- And it must mock BookingRepository
- And it must include the following test cases:

| test method                                      | scenario                                      |
| --- | --- |
| shouldAllowPrivilegedCallerWithoutOwnerCheck()   | SERVICE, OPERATOR, or ADMIN can access        |
| shouldAllowCustomerWhenOwnerMatches()            | CUSTOMER token customerId matches booking owner |
| shouldRejectCustomerWhenOwnerDiffers()           | CUSTOMER token customerId does not match      |
| shouldRejectCustomerWithoutCustomerIdClaim()     | CUSTOMER token has no customerId/customer_id claim |
| shouldAllowCustomerCreateWhenRequestCustomerMatches() | request.customerId matches CUSTOMER token claim |
| shouldRejectCustomerCreateWhenRequestCustomerDiffers() | request.customerId does not match CUSTOMER token claim |
| shouldAllowCustomerListWhenQueryCustomerMatches() | query customerId matches CUSTOMER token claim |
| shouldRejectCustomerListWhenQueryCustomerMissing() | CUSTOMER has token customerId but omits customerId query parameter |
| shouldRejectCustomerListWhenTokenCustomerIdMissing() | CUSTOMER token has no customerId/customer_id claim |
| shouldRejectCustomerListWhenQueryCustomerDiffers() | query customerId does not match CUSTOMER token claim |
| shouldDeferNotFoundToBookingService()            | Repository empty returns without throwing so BookingService owns the 404 |
| shouldAllowAccessWhenSecurityDisabled()          | Disabled security skips ownership checks      |

## Unit Tests — Domain Model

### BookingStateMachine state transition tests

Tags: `testing`, `unit`, `domain`

- Given a test class "BookingStateMachineTest" in package "com.cargo.booking.service"
- Then it must verify all valid transitions from 002_domain_model.md:

| test method                             | from        | to          | expected |
| --- | --- | --- | --- |
| shouldAllowPendingToConfirmed()         | PENDING     | CONFIRMED   | allowed  |
| shouldAllowPendingToCancelled()         | PENDING     | CANCELLED   | allowed  |
| shouldAllowConfirmedToInProgress()      | CONFIRMED   | IN_PROGRESS | allowed  |
| shouldAllowConfirmedToCancelled()       | CONFIRMED   | CANCELLED   | allowed  |
| shouldAllowInProgressToCompleted()      | IN_PROGRESS | COMPLETED   | allowed  |

- And it must verify all invalid transitions throw IllegalStateTransitionException:

| test method                              | from        | to          |
| --- | --- | --- |
| shouldRejectPendingToCompleted()         | PENDING     | COMPLETED   |
| shouldRejectPendingToInProgress()        | PENDING     | IN_PROGRESS |
| shouldRejectConfirmedToPending()         | CONFIRMED   | PENDING     |
| shouldRejectInProgressToCancelled()      | IN_PROGRESS | CANCELLED   |
| shouldRejectCompletedToAnything()        | COMPLETED   | CANCELLED   |
| shouldRejectCancelledToAnything()        | CANCELLED   | PENDING     |

### BookingReferenceGenerator tests

Tags: `testing`, `unit`, `domain`

- Given a test class "BookingReferenceGeneratorTest" in package "com.cargo.booking.service"
- Then it must use Mockito to mock the BookingReferenceCounterRepository
- And it must verify:

| test method                               | description                                        |
| --- | --- |
| shouldGenerateReferenceWithCurrentYear()  | Reference starts with "BKG-{currentYear}-"          |
| shouldPadSequenceTo5Digits()              | Sequence 1 becomes "00001", 42 becomes "00042"      |
| shouldUseSequenceFromCounterRepository()  | The sequence value comes from the mocked counter repository |

## Unit Tests — Service Layer

### BookingService.createBooking() tests

Tags: `testing`, `unit`, `service`

- Given a test class "BookingServiceCreateTest" in package "com.cargo.booking.service"
- Then it must use @ExtendWith(MockitoExtension.class)
- And it must mock:

| dependency                | purpose                                 |
| --- | --- |
| BookingRepository         | Verify save is called with correct data |
| BookingReferenceGenerator | Return predictable references           |
| ScheduleClient            | Control schedule validation result      |
| EquipmentClient           | Not called during creation              |
| QuoteClient               | Control quote validation result         |

- And it must include the following test cases:

| test method                                            | scenario                                         |
| --- | --- |
| shouldCreateBookingSuccessfully()                     | Happy path — valid request, all validations pass  |
| shouldSetStatusToPending()                            | New booking always starts as PENDING              |
| shouldGenerateUniqueReference()                       | Reference comes from BookingReferenceGenerator    |
| shouldThrowWhenScheduleNotAvailable()                 | ScheduleClient returns false                     |
| shouldThrowWhenQuoteNotValid()                        | QuoteClient returns false                        |
| shouldThrowWhenEquipmentListEmpty()                   | Request with empty equipment list                |
| shouldThrowWhenEquipmentTypeUnsupported()             | Equipment type is not recognized by EquipmentType.fromCode |
| shouldNotPersistBookingWhenValidationFails()          | Verify repository.save() is never called on failure |

### BookingService state transition tests

Tags: `testing`, `unit`, `service`

- Given a test class "BookingServiceLifecycleTest" in package "com.cargo.booking.service"
- Then it must use @ExtendWith(MockitoExtension.class)
- And it must mock BookingStateMachine because BookingStateMachineTest owns transition-rule coverage
- And it must verify BookingService calls BookingStateMachine before every lifecycle status change
- And it must include the following test cases:

| test method                                            | scenario                                         |
| --- | --- |
| shouldConfirmPendingBooking()                         | PENDING → CONFIRMED happy path                    |
| shouldReserveEquipmentOnConfirmation()                | Verify EquipmentClient.reserveEquipment() called  |
| shouldThrowWhenConfirmingNonPendingBooking()          | CONFIRMED → CONFIRMED should fail                 |
| shouldNotChangeStatusWhenEquipmentReservationFails()  | Status remains PENDING if equipment fails         |
| shouldStartConfirmedBooking()                         | CONFIRMED → IN_PROGRESS happy path                |
| shouldCompleteInProgressBooking()                     | IN_PROGRESS → COMPLETED happy path                |
| shouldCancelPendingBooking()                          | PENDING → CANCELLED without equipment release     |
| shouldCancelConfirmedBooking()                        | CONFIRMED → CANCELLED with equipment release      |
| shouldCancelEvenWhenEquipmentReleaseFails()           | Cancellation proceeds despite release failure     |
| shouldThrowWhenCancellingCompletedBooking()           | COMPLETED → CANCELLED should fail                 |

### BookingService read operation tests

Tags: `testing`, `unit`, `service`

- Given a test class "BookingServiceReadTest" in package "com.cargo.booking.service"
- Then it must include the following test cases:

| test method                                    | scenario                                          |
| --- | --- |
| shouldGetBookingById()                        | Returns booking when found                         |
| shouldThrowNotFoundWhenBookingIdMissing()     | Throws BookingNotFoundException for unknown ID     |
| shouldGetBookingByReference()                 | Returns booking when found by reference            |
| shouldThrowNotFoundWhenReferenceMissing()     | Throws BookingNotFoundException for unknown ref    |
| shouldReturnPagedBookingsForCustomer()        | Returns paginated results                          |
| shouldFilterByCustomerAndStatus()             | Pagination with status filter                      |

## Unit Tests — Mapper

### BookingMapper tests

Tags: `testing`, `unit`, `mapper`

- Given a test class "BookingMapperTest" in package "com.cargo.booking.mapper"
- Then it must verify:

| test method                                        | description                                     |
| --- | --- |
| shouldMapEntityToResponse()                       | All fields correctly mapped from entity to DTO    |
| shouldMapEntityToCreatedResponse()                | Slim response has reference, status, createdAt    |
| shouldMapEquipmentLinesToResponse()               | Equipment lines mapped correctly                  |
| shouldHandleNullPhoneNumber()                     | Null phone is preserved without NPE               |

## Integration Tests — Repository Layer

### BookingRepository integration tests

Tags: `testing`, `integration`, `repository`

- Given a test class "BookingRepositoryTest" in package "com.cargo.booking.repository"
- Then it must be annotated with @DataJpaTest and @ActiveProfiles("test")
- And it must use Zonky @AutoConfigureEmbeddedDatabase or an imported shared test database configuration so the slice test runs against embedded PostgreSQL
- And it must include the following test cases:

| test method                                          | description                                        |
| --- | --- |
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

### BookingReferenceCounterRepository integration tests

Tags: `testing`, `integration`, `repository`

- Given a test class "BookingReferenceCounterRepositoryTest" in package "com.cargo.booking.repository"
- Then it must be annotated with @DataJpaTest and @ActiveProfiles("test")
- And it must use Zonky @AutoConfigureEmbeddedDatabase or an imported shared test database configuration so the slice test runs against embedded PostgreSQL
- And it must import or instantiate the custom BookingReferenceCounterRepository implementation
- And it must include the following test cases:

| test method                                          | description                                        |
| --- | --- |
| shouldReturnOneForNewYear()                         | First call for a year returns sequence 1           |
| shouldIncrementExistingYearCounter()                 | Subsequent calls for same year return 2, 3, ...    |
| shouldKeepCountersIndependentAcrossYears()           | Different years have independent sequences         |
| shouldPersistCounterRowsWithNextValue()              | Counter table stores the next value after calls    |

## Integration Tests — Controller Layer (MockMvc)

### BookingController MockMvc test setup

Tags: `testing`, `integration`, `controller`

- Given a test class "BookingControllerTest" in package "com.cargo.booking.controller"
- Then it must be annotated with:

| annotation                                                              |
| --- |
| @WebMvcTest(BookingController.class)                                    |
| @ActiveProfiles("test")                                                 |
| @Import(SecurityConfig.class)                                           |

- And it must use @MockitoBean for:

| dependency       | purpose                                  |
| --- | --- |
| BookingService   | Mock business logic                      |
| BookingMapper    | Mock entity-DTO conversion               |
| JwtTokenProvider | Mock JWT validation for security context |
| BookingAccessAuthorizer | Mock ownership checks before service calls |

- And it must import or component-scan the real JwtAuthenticationFilter, JwtAuthenticationEntryPoint, and JwtAccessDeniedHandler
- And JwtAuthenticationFilter must not be mocked because mocked servlet filters may not call FilterChain.doFilter()
- And it must use JwtTestHelper to generate tokens for authenticated requests

### POST /api/v1/bookings controller tests

Tags: `testing`, `integration`, `controller`

- Given the BookingControllerTest class
- Then it must include the following MockMvc test cases:

| test method                                          | status | description                                    |
| --- | --- | --- |
| shouldCreateBookingAndReturn201()                   | 201    | Valid request.customerId matching CUSTOMER token customerId claim |
| shouldCreateBookingAsAdminForAnyCustomer()          | 201    | ADMIN can create with any valid request.customerId |
| shouldCreateBookingAsServiceForAnyCustomer()        | 201    | SERVICE token can create on behalf of request.customerId |
| shouldReturn400WhenRequestBodyInvalid()             | 400    | Missing required fields                          |
| shouldReturn400WhenCustomerIdMissingFromCreateRequest() | 400 | Missing customerId in request body                |
| shouldReturn400WhenEmailInvalid()                   | 400    | Malformed email in customer request              |
| shouldReturn400WhenEquipmentListEmpty()             | 400    | Empty equipment array                            |
| shouldReturn400WhenEquipmentTypeUnsupported()       | 400    | Service throws BookingValidationException for unsupported equipment type |
| shouldReturn401WhenNoAuthToken()                    | 401    | Request without Authorization header             |
| shouldReturn403WhenOperatorTriesToCreate()          | 403    | OPERATOR role cannot create bookings             |
| shouldReturn403WhenCustomerCreatesForAnotherCustomer() | 403 | request.customerId does not match JWT customerId claim |
| shouldReturn403WhenCustomerTokenHasNoCustomerIdClaim() | 403 | CUSTOMER token cannot infer customerId from subject |
| shouldReturn422WhenScheduleNotAvailable()           | 422    | Service throws ScheduleNotAvailableException     |
| shouldReturn422WhenQuoteNotValid()                  | 422    | Service throws QuoteNotValidException            |

### GET /api/v1/bookings/{id} controller tests

Tags: `testing`, `integration`, `controller`

- Given the BookingControllerTest class
- Then it must include the following MockMvc test cases:

| test method                                          | status | description                                    |
| --- | --- | --- |
| shouldGetBookingByIdAndReturn200()                  | 200    | Valid numeric ID, booking found                  |
| shouldGetBookingByReferenceAndReturn200()           | 200    | Valid BKG reference, booking found               |
| shouldReturn400WhenBookingIdentifierFormatInvalid() | 400    | Invalid ID/reference format throws BookingValidationException |
| shouldReturn404WhenBookingNotFound()                | 404    | Unknown ID returns 404                           |
| shouldReturn401WhenNotAuthenticated()               | 401    | No token                                         |
| shouldReturn403WhenCustomerAccessesOtherCustomerBooking() | 403 | BookingAccessAuthorizer rejects ownership mismatch |

### GET /api/v1/bookings controller tests

Tags: `testing`, `integration`, `controller`

- Given the BookingControllerTest class
- Then it must include the following MockMvc test cases:

| test method                                          | status | description                                    |
| --- | --- | --- |
| shouldListBookingsByCustomerAndReturn200()          | 200    | Valid customerId, paginated results              |
| shouldListAllBookingsForServiceWhenCustomerIdMissing() | 200 | SERVICE may omit customerId                       |
| shouldListAllBookingsForOperatorWhenCustomerIdMissing() | 200 | OPERATOR may omit customerId                     |
| shouldReturn400WhenCustomerIdMissingForCustomer()   | 400    | CUSTOMER must provide customerId query parameter when security is enabled |
| shouldReturn403WhenCustomerIdDoesNotMatchCustomer() | 403    | CUSTOMER cannot list another customer's bookings  |
| shouldFilterByStatus()                              | 200    | customerId + status filter                       |

### PATCH /api/v1/bookings/{id}/cancel controller tests

Tags: `testing`, `integration`, `controller`

- Given the BookingControllerTest class
- Then it must include the following MockMvc test cases:

| test method                                          | status | description                                    |
| --- | --- | --- |
| shouldCancelBookingAndReturn200()                   | 200    | Valid cancel request by CUSTOMER                 |
| shouldCancelBookingAsService()                      | 200    | SERVICE can cancel on behalf of request customer |
| shouldReturn409WhenInvalidStateTransition()         | 409    | Service throws IllegalStateTransitionException   |
| shouldReturn403WhenOperatorTriesToCancel()          | 403    | OPERATOR cannot cancel                           |
| shouldReturn403WhenCustomerCancelsOtherCustomerBooking() | 403 | BookingAccessAuthorizer rejects ownership mismatch |

### PATCH lifecycle endpoint controller tests

Tags: `testing`, `integration`, `controller`

- Given the BookingControllerTest class
- Then it must include the following MockMvc test cases:

| test method                                          | status | description                                    |
| --- | --- | --- |
| shouldConfirmBookingAsOperator()                    | 200    | OPERATOR confirms PENDING booking                |
| shouldStartBookingAsOperator()                      | 200    | OPERATOR starts CONFIRMED booking                |
| shouldCompleteBookingAsOperator()                   | 200    | OPERATOR completes IN_PROGRESS booking           |
| shouldReturn403WhenCustomerTriesToConfirm()         | 403    | CUSTOMER cannot confirm                          |

### BookingController tests with security disabled

Tags: `testing`, `integration`, `controller`

- Given a separate test class "BookingControllerSecurityDisabledTest" in package "com.cargo.booking.controller"
- Then it must use the same controller slice setup as BookingControllerTest
- And it must override app.security.enabled=false for the test context
- And it must include the following MockMvc test cases:

| test method                                | status | description                                    |
| --- | --- | --- |
| shouldCreateBookingWithoutJwtWhenSecurityDisabled() | 201 | Valid request.customerId without Authorization header |
| shouldListBookingsWithCustomerIdWhenSecurityDisabled() | 200 | customerId query parameter comes from request data |

## Integration Tests — Error Handling

### GlobalExceptionHandler integration tests

Tags: `testing`, `integration`, `error`

- Given a test class "ErrorHandlingTest" in package "com.cargo.booking.exception"
- Then it must be annotated with @WebMvcTest and @AutoConfigureMockMvc(addFilters = false)
- And it must test the error response format without security filters intercepting requests
- And it must verify:

| test method                                        | description                                          |
| --- | --- |
| shouldReturnStructuredErrorFor404()               | Error body matches ErrorResponse format               |
| shouldReturnValidationErrorsWithFieldDetails()    | 400 response includes violations list                 |
| shouldReturnStructuredErrorForBookingValidationException() | BookingValidationException maps to standard 400 ErrorResponse |
| shouldReturnSortedFieldViolations()               | Violations are alphabetically ordered                 |
| shouldNotExposeInternalDetailsIn500()              | Generic message for unexpected exceptions             |
| shouldIncludeRequestPathInError()                 | Path field matches the request URI                    |
| shouldIncludeRequestIdWhenProvided()              | X-Request-ID header is reflected in error response    |

## Integration Tests — Security

### Security integration tests

Tags: `testing`, `integration`, `security`

- Given a test class "SecurityIntegrationTest" in package "com.cargo.booking.security"
- Then it must extend BaseIntegrationTest
- And it must activate both the "test" and "local" profiles so BookingService can use local stub clients
- And it must explicitly override app.security.enabled=true because the local profile disables security by default
- And it must verify:

| test method                                              | description                                      |
| --- | --- |
| shouldAllowAccessToSwaggerWithoutAuth()                 | /swagger-ui/** is public                          |
| shouldAllowAccessToHealthWithoutAuth()                  | /actuator/health is public                        |
| shouldRejectRequestWithExpiredToken()                   | 401 for expired JWT                               |
| shouldRejectRequestWithMalformedToken()                 | 401 for garbage token                             |
| shouldRejectRequestWithWrongIssuer()                    | 401 for wrong issuer claim                        |
| shouldEnforceCustomerOwnershipOnGetBooking()            | CUSTOMER can only see own bookings                |
| shouldEnforceCustomerOwnershipOnCancel()                | CUSTOMER can only cancel own bookings             |
| shouldRejectCustomerOwnershipCheckWithoutCustomerIdClaim() | CUSTOMER token without customerId claim gets 403 |
| shouldAllowServiceTokenToActForRequestedCustomer()       | SERVICE token can use request/query customerId    |
| shouldAllowOperatorToAccessAnyBooking()                 | OPERATOR can view any booking                     |
| shouldAllowAdminFullAccess()                            | ADMIN can access all endpoints                    |

# Integration Tests — External Service Clients (Deferred)
# ---------------------------------------------------------------------------

### WireMock tests for external service clients

Tags: `testing`, `integration`, `wiremock`

- Given the external services (Schedules, Equipment, Quotes) do not have finalized API contracts
- Then WireMock-based integration tests for real client implementations are DEFERRED
- And they must be written when the external API contracts are available
- And the test infrastructure must be prepared now:

| preparation                                                                    |
| --- |
| WireMock dependency is included in pom.xml (test scope)                        |
| A base class "BaseWireMockTest" must be created in "com.cargo.booking.client"  |
| It must configure WireMock servers on dynamic ports for each external service   |
| Test application properties must override base URLs to point to WireMock       |

- And when contracts are known, tests should cover at minimum:

| scenario type                                         |
| --- |
| Happy path — external service returns success         |
| Not found — external service returns 404              |
| Server error — external service returns 500           |
| Timeout — external service does not respond in time   |
| Circuit breaker opens after repeated failures         |

## End-to-End Tests

### Full booking lifecycle end-to-end test

Tags: `testing`, `e2e`

- Given a test class "BookingLifecycleE2ETest" in package "com.cargo.booking"
- Then it must extend BaseIntegrationTest (embedded PostgreSQL)
- And it must use both the "test" and "local" profiles so that test infrastructure and stub clients are active
- And it must run with app.security.enabled=false to verify the service works without JWT security
- And it must use TestRestTemplate or WebTestClient without requiring Authorization headers
- And it must verify the complete happy path:

| step | action                                                     | expected                           |
| --- | --- | --- |
| 1    | POST /api/v1/bookings with valid request.customerId         | 201, status=PENDING, reference set |
| 2    | GET /api/v1/bookings/{id}                                  | 200, full booking details          |
| 3    | PATCH /api/v1/bookings/{id}/confirm                        | 200, status=CONFIRMED              |
| 4    | PATCH /api/v1/bookings/{id}/start                          | 200, status=IN_PROGRESS            |
| 5    | PATCH /api/v1/bookings/{id}/complete                       | 200, status=COMPLETED              |
| 6    | GET /api/v1/bookings?customerId={request.customerId}        | 200, list includes this booking    |

### Cancellation flow end-to-end test

Tags: `testing`, `e2e`

- Given the BookingLifecycleE2ETest class
- Then it must also verify the cancellation flow:

| step | action                                                     | expected                            |
| --- | --- | --- |
| 1    | POST /api/v1/bookings with valid request.customerId         | 201, status=PENDING                 |
| 2    | PATCH /api/v1/bookings/{id}/cancel                         | 200, status=CANCELLED               |
| 3    | PATCH /api/v1/bookings/{id}/confirm                        | 409, cannot confirm cancelled booking|

## Test Naming and Organization Conventions

### Test naming and structure conventions

Tags: `testing`, `conventions`

- Given all test classes
- Then the following conventions must apply:

| rule                                                                                        |
| --- |
| Test method names use should...() pattern (e.g. shouldCreateBookingSuccessfully())          |
| Each test method tests exactly one behavior                                                 |
| Tests follow Arrange-Act-Assert (AAA) or Given-When-Then structure internally               |
| Unit tests use @DisplayName for human-readable descriptions                                 |
| Integration tests are tagged with @Tag("integration") for selective execution               |
| E2E tests are tagged with @Tag("e2e") for selective execution                               |
| No test should depend on the execution order of other tests                                 |
| Each test must clean up its own data or rely on transaction rollback                         |

### Test coverage targets

Tags: `testing`, `conventions`

- Given the Booking Service test suite
- Then the following coverage targets should be aimed for:

| layer          | target   | notes                                          |
| --- | --- | --- |
| Service layer  | 90%+     | Core business logic must be thoroughly tested  |
| Controller     | 85%+     | All endpoints and error paths                  |
| Repository     | 80%+     | Custom queries and specifications              |
| Mapper         | 90%+     | All mapping paths including edge cases         |
| Security       | 85%+     | Auth, roles, and ownership checks              |
| Integration    | 75%+     | Repository, controller, and security integration tests now; client WireMock tests when real clients exist |

- And these are targets, not hard gates — the AI should aim for them but not generate filler tests

## Out of Scope for this file

### Items NOT covered in testing

Tags: `testing`, `out-of-scope`

- Given this is the testing file only
- Then the following are NOT defined here and will be addressed in later files:

| topic                                    | deferred to           |
| --- | --- |
| Docker Compose for test infrastructure   | 010_deployment.md     |
| Performance / load testing               | Out of scope for v1   |
| Contract testing (Pact / Spring Cloud)   | Out of scope for v1   |
| Mutation testing                         | Out of scope for v1   |
