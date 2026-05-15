# File: 005_api_endpoints.md
# Depends on: 001_project_setup.md, 002_domain_model.md, 003_data_access.md, 004_business_rules.md
# Produces: REST controllers, request/response DTOs, mapper classes, API documentation annotations
# Context: Defines the REST API contract for the Cargo Booking Service. The AI agent must have
#          processed 001–004 so that entities, repositories, services, and conventions are known.

Feature: API Endpoints
  As an AI code generator
  I need to define REST controllers, DTOs, and mappers for the Booking Service
  So that clients can interact with the booking system through a well-documented HTTP API

  Background:
    Given the base package is "com.cargo.booking"
    And controllers reside in "com.cargo.booking.controller"
    And request DTOs reside in "com.cargo.booking.dto.request"
    And response DTOs reside in "com.cargo.booking.dto.response"
    And mapper classes reside in "com.cargo.booking.mapper"
    And the API prefix is "/api/v1" as defined in 001_project_setup.md
    And all conventions from 001_project_setup.md apply

  # ---------------------------------------------------------------------------
  # Request DTOs
  # ---------------------------------------------------------------------------

  @api @dto @request
  Scenario: CreateBookingRequest DTO
    Given a record "CreateBookingRequest" in package "com.cargo.booking.dto.request"
    Then it must be a Java record with the following fields:
      | field      | type                  | validation              | description                        |
      | customerId | Long                  | @NotNull                | Customer/account that owns the booking |
      | scheduleId | Long                  | @NotNull                | The schedule to book               |
      | quoteId    | Long                  | @NotNull                | The associated quote               |
      | customer   | CustomerRequest       | @NotNull @Valid         | Nested customer details            |
      | cargo      | CargoRequest          | @NotNull @Valid         | Nested cargo details               |
      | equipment  | List<EquipmentRequest>| @NotEmpty @Valid        | At least one equipment line needed |

  @api @dto @request
  Scenario: CustomerRequest DTO
    Given a record "CustomerRequest" in package "com.cargo.booking.dto.request"
    Then it must be a Java record with the following fields:
      | field | type   | validation                      | description          |
      | name  | String | @NotBlank @Size(max = 255)      | Customer full name   |
      | email | String | @NotBlank @Email @Size(max = 255)| Customer email      |
      | phone | String | @Size(max = 50)                 | Optional phone number|

  @api @dto @request
  Scenario: CargoRequest DTO
    Given a record "CargoRequest" in package "com.cargo.booking.dto.request"
    Then it must be a Java record with the following fields:
      | field       | type       | validation                              | description          |
      | description | String     | @NotBlank @Size(max = 1000)             | Cargo description    |
      | weightKg    | BigDecimal | @NotNull @DecimalMin("0.01")            | Weight in kilograms  |

  @api @dto @request
  Scenario: EquipmentRequest DTO
    Given a record "EquipmentRequest" in package "com.cargo.booking.dto.request"
    Then it must be a Java record with the following fields:
      | field    | type   | validation          | description              |
      | type     | String | @NotBlank           | Equipment type (e.g. "20FT", "40FT") |
      | quantity | int    | @Min(1)             | Number of containers     |
    And the "type" field must be validated against the EquipmentType enum values at the service layer

  # ---------------------------------------------------------------------------
  # Response DTOs
  # ---------------------------------------------------------------------------

  @api @dto @response
  Scenario: BookingResponse DTO
    Given a record "BookingResponse" in package "com.cargo.booking.dto.response"
    Then it must be a Java record with the following fields:
      | field             | type                       | description                           |
      | id                | Long                       | Internal booking ID                   |
      | bookingReference  | String                     | Human-readable reference              |
      | customerId        | Long                       | Customer/account that owns the booking |
      | status            | String                     | Current booking status                |
      | scheduleId        | Long                       | The linked schedule                   |
      | quoteId           | Long                       | The linked quote                      |
      | customer          | CustomerResponse           | Nested customer details               |
      | cargo             | CargoResponse              | Nested cargo details                  |
      | equipment         | List<EquipmentResponse>    | List of equipment lines               |
      | createdAt         | Instant                    | When the booking was created (UTC)    |
      | updatedAt         | Instant                    | When the booking was last updated (UTC)|

  @api @dto @response
  Scenario: BookingCreatedResponse DTO
    Given a record "BookingCreatedResponse" in package "com.cargo.booking.dto.response"
    Then it must be a Java record with the following fields:
      | field             | type    | description                           |
      | id                | Long    | Internal booking ID                   |
      | bookingReference  | String  | Human-readable reference              |
      | customerId        | Long    | Customer/account that owns the booking |
      | status            | String  | Always "PENDING" for a new booking    |
      | createdAt         | Instant | When the booking was created (UTC)    |
    And this is the slim response returned after creating a booking (matches the API spec)

  @api @dto @response
  Scenario: CustomerResponse DTO
    Given a record "CustomerResponse" in package "com.cargo.booking.dto.response"
    Then it must be a Java record with the following fields:
      | field | type   | description          |
      | name  | String | Customer full name   |
      | email | String | Customer email       |
      | phone | String | Customer phone       |

  @api @dto @response
  Scenario: CargoResponse DTO
    Given a record "CargoResponse" in package "com.cargo.booking.dto.response"
    Then it must be a Java record with the following fields:
      | field       | type       | description          |
      | description | String     | Cargo description    |
      | weightKg    | BigDecimal | Weight in kilograms  |

  @api @dto @response
  Scenario: EquipmentResponse DTO
    Given a record "EquipmentResponse" in package "com.cargo.booking.dto.response"
    Then it must be a Java record with the following fields:
      | field    | type   | description              |
      | type     | String | Equipment type           |
      | quantity | int    | Number of containers     |

  @api @dto @response
  Scenario: PagedResponse wrapper
    Given a generic class "PagedResponse<T>" in package "com.cargo.booking.dto.response"
    Then it must be a Java record with the following fields:
      | field         | type     | description                               |
      | content       | List<T>  | The page content                          |
      | page          | int      | Current page number (zero-based)          |
      | size          | int      | Page size                                 |
      | totalElements | long     | Total number of elements across all pages |
      | totalPages    | int      | Total number of pages                     |
      | last          | boolean  | Whether this is the last page             |
    And a static factory method must exist to create a PagedResponse from a Spring Page<T>

  # ---------------------------------------------------------------------------
  # Mapper
  # ---------------------------------------------------------------------------

  @api @mapper
  Scenario: BookingMapper class
    Given a class "BookingMapper" in package "com.cargo.booking.mapper"
    Then it must be annotated with @Component
    And it must provide the following mapping methods:
      | method                                                       | from                    | to                      |
      | toEntity(CreateBookingRequest request, String reference)      | CreateBookingRequest   | Booking                 |
      | toResponse(Booking entity)                                   | Booking                 | BookingResponse         |
      | toCreatedResponse(Booking entity)                            | Booking                 | BookingCreatedResponse  |
      | toEquipmentLineEntity(EquipmentRequest request)              | EquipmentRequest        | BookingEquipmentLine    |
      | toEquipmentResponse(BookingEquipmentLine entity)             | BookingEquipmentLine    | EquipmentResponse       |
    And the toEntity method must:
      | rule                                                                    |
      | Set status to PENDING                                                   |
      | Map customer fields from the nested CustomerRequest                     |
      | Map cargo fields from the nested CargoRequest                           |
      | Convert each EquipmentRequest to a BookingEquipmentLine and associate   |
      | Set the bookingReference from the provided reference parameter          |
      | Set customerId from request.customerId                                  |
    And the mapper must NOT use any reflection-based mapping libraries (keep it explicit)

  # ---------------------------------------------------------------------------
  # Controller
  # ---------------------------------------------------------------------------

  @api @controller
  Scenario: BookingController class definition
    Given a class "BookingController" in package "com.cargo.booking.controller"
    Then it must be annotated with:
      | annotation                          | value/purpose                             |
      | @RestController                     | Marks as REST controller                  |
      | @RequestMapping("/api/v1/bookings") | Base path for all booking endpoints       |
      | @Tag(name = "Bookings")             | OpenAPI grouping                          |
      | @RequiredArgsConstructor            | Lombok constructor injection              |
    And its dependencies must include:
      | dependency        | purpose                            |
      | BookingService    | Business logic orchestration       |
      | BookingMapper     | Entity-to-DTO conversion           |

  # ---------------------------------------------------------------------------
  # POST /api/v1/bookings
  # ---------------------------------------------------------------------------

  @api @endpoint @create
  Scenario: Create booking endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path | httpMethod |
      | createBooking | "" (empty, inherits base path) | POST |
    And the method signature must be:
      """
      @PostMapping
      @ResponseStatus(HttpStatus.CREATED)
      @Operation(summary = "Create a new booking")
      public BookingCreatedResponse createBooking(@Valid @RequestBody CreateBookingRequest request)
      """
    And it must:
      | step | action                                                       |
      | 1    | Apply endpoint authentication and authorization rules defined in 006_security.md |
      | 2    | Call bookingService.createBooking() with the request         |
      | 3    | Map the result to BookingCreatedResponse using the mapper    |
      | 4    | Return the response with HTTP 201 Created                    |

  @api @endpoint @create
  Scenario: Create booking — request body example
    Given the POST /api/v1/bookings endpoint
    Then a valid request body looks like:
      """json
      {
        "customerId": 3001,
        "scheduleId": 1001,
        "quoteId": 2001,
        "customer": {
          "name": "Acme Shipping Co.",
          "email": "logistics@acme.com",
          "phone": "+36-1-234-5678"
        },
        "cargo": {
          "description": "Industrial machinery parts",
          "weightKg": 12000.00
        },
        "equipment": [
          { "type": "20FT", "quantity": 2 },
          { "type": "40HC", "quantity": 1 }
        ]
      }
      """
    And a valid response (HTTP 201) looks like:
      """json
      {
        "id": 42,
        "bookingReference": "BKG-2026-00042",
        "customerId": 3001,
        "status": "PENDING",
        "createdAt": "2026-03-31T10:00:00Z"
      }
      """

  # ---------------------------------------------------------------------------
  # GET /api/v1/bookings/{id}
  # ---------------------------------------------------------------------------

  @api @endpoint @read
  Scenario: Get booking by ID endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path   | httpMethod |
      | getBookingById | "/{id}" | GET |
    And the method signature must be:
      """
      @GetMapping("/{id}")
      @Operation(summary = "Get booking by ID or reference")
      public BookingResponse getBookingById(@PathVariable("id") String id)
      """
    And the "id" parameter must accept both a numeric Long value and a booking reference (BKG-YYYY-NNNNN)
    And the controller must detect the format:
      | input format    | action                                  |
      | Numeric format  | Call bookingService.getBookingById()     |
      | BKG-YYYY-NNNNN | Call bookingService.getBookingByReference() |
    And the result must be mapped to BookingResponse and returned with HTTP 200

  @api @endpoint @read
  Scenario: Get booking — response body example
    Given the GET /api/v1/bookings/{id} endpoint
    Then a valid response (HTTP 200) looks like:
      """json
      {
        "id": 42,
        "bookingReference": "BKG-2026-00042",
        "customerId": 3001,
        "status": "CONFIRMED",
        "scheduleId": 1001,
        "quoteId": 2001,
        "customer": {
          "name": "Acme Shipping Co.",
          "email": "logistics@acme.com",
          "phone": "+36-1-234-5678"
        },
        "cargo": {
          "description": "Industrial machinery parts",
          "weightKg": 12000.00
        },
        "equipment": [
          { "type": "20FT", "quantity": 2 },
          { "type": "40HC", "quantity": 1 }
        ],
        "createdAt": "2026-03-31T10:00:00Z",
        "updatedAt": "2026-04-01T14:30:00Z"
      }
      """

  # ---------------------------------------------------------------------------
  # GET /api/v1/bookings
  # ---------------------------------------------------------------------------

  @api @endpoint @read
  Scenario: List bookings endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path | httpMethod |
      | getBookings | "" (empty, inherits base path) | GET |
    And the method signature must be:
      """
      @GetMapping
      @Operation(summary = "List bookings")
      public PagedResponse<BookingResponse> getBookings(
          @RequestParam(required = false) Long customerId,
          @RequestParam(required = false) BookingStatus status,
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
      )
      """
    And it must:
      | step | action                                                                |
      | 1    | Apply endpoint authentication and authorization rules defined in 006_security.md |
      | 2    | Call bookingService.getBookings(customerId, status, pageable)            |
      | 3    | Map each booking entity to BookingResponse                              |
      | 4    | Wrap in PagedResponse and return with HTTP 200                          |

  @api @endpoint @read
  Scenario: List bookings — query parameter validation
    Given the GET /api/v1/bookings endpoint
    Then the following query parameters must be supported:
      | parameter   | type          | required | default             | description                    |
      | customerId  | Long          | conditional | null (all customers for SERVICE/OPERATOR/ADMIN) | Filter by customer |
      | status      | BookingStatus | no       | null (all statuses) | Filter by booking status       |
      | page        | int           | no       | 0                   | Page number (zero-based)       |
      | size        | int           | no       | 20                  | Page size (max 100)            |
      | sort        | String        | no       | createdAt,desc      | Sort field and direction       |
    And conditional customerId requirements for secured callers are defined in 006_security.md

  # ---------------------------------------------------------------------------
  # PATCH /api/v1/bookings/{id}/cancel
  # ---------------------------------------------------------------------------

  @api @endpoint @cancel
  Scenario: Cancel booking endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path            | httpMethod |
      | cancelBooking | "/{id}/cancel" | PATCH |
    And the method signature must be:
      """
      @PatchMapping("/{id}/cancel")
      @Operation(summary = "Cancel a booking")
      public BookingResponse cancelBooking(@PathVariable("id") Long id)
      """
    And it must:
      | step | action                                                  |
      | 1    | Call bookingService.cancelBooking(id)                   |
      | 2    | Map the result to BookingResponse using the mapper      |
      | 3    | Return the response with HTTP 200                        |

  # ---------------------------------------------------------------------------
  # Additional Lifecycle Endpoints (not in original spec but needed for completeness)
  # ---------------------------------------------------------------------------

  @api @endpoint @lifecycle
  Scenario: Confirm booking endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path              | httpMethod |
      | confirmBooking | "/{id}/confirm" | PATCH |
    And the method signature must be:
      """
      @PatchMapping("/{id}/confirm")
      @Operation(summary = "Confirm a booking")
      public BookingResponse confirmBooking(@PathVariable("id") Long id)
      """
    And it must call bookingService.confirmBooking(id) and return BookingResponse with HTTP 200

  @api @endpoint @lifecycle
  Scenario: Start booking endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path            | httpMethod |
      | startBooking | "/{id}/start" | PATCH |
    And the method signature must be:
      """
      @PatchMapping("/{id}/start")
      @Operation(summary = "Mark booking as in progress")
      public BookingResponse startBooking(@PathVariable("id") Long id)
      """
    And it must call bookingService.startBooking(id) and return BookingResponse with HTTP 200

  @api @endpoint @lifecycle
  Scenario: Complete booking endpoint
    Given the BookingController
    Then it must define the following endpoint:
      | method | path               | httpMethod |
      | completeBooking | "/{id}/complete" | PATCH |
    And the method signature must be:
      """
      @PatchMapping("/{id}/complete")
      @Operation(summary = "Mark booking as completed")
      public BookingResponse completeBooking(@PathVariable("id") Long id)
      """
    And it must call bookingService.completeBooking(id) and return BookingResponse with HTTP 200

  # ---------------------------------------------------------------------------
  # OpenAPI Documentation
  # ---------------------------------------------------------------------------

  @api @documentation
  Scenario: OpenAPI annotations on all endpoints
    Given all controller methods
    Then each method must be annotated with @Operation including:
      | attribute   | purpose                                        |
      | summary     | Short description of the endpoint              |
      | description | Longer description including status codes       |
    And each method must use @ApiResponse annotations for:
      | status code | description                                    |
      | 200         | Successful retrieval or update                 |
      | 201         | Successful creation (POST only)                |
      | 400         | Validation error — invalid request body or params |
      | 404         | Booking not found                              |
      | 409         | Invalid state transition                       |
      | 500         | Internal server error                          |

  # ---------------------------------------------------------------------------
  # Content Negotiation and Headers
  # ---------------------------------------------------------------------------

  @api @headers
  Scenario: Content type and header rules
    Given all endpoints in the BookingController
    Then the following rules must apply:
      | rule                                                                          |
      | All endpoints must produce "application/json"                                 |
      | POST endpoint must consume "application/json"                                 |
      | All Instant fields must be serialized as ISO-8601 strings in UTC              |
      | BigDecimal fields must be serialized as numbers, not strings                  |
      | Null fields must be omitted from JSON responses, including absent requestId values |

  @api @headers
  Scenario: Jackson global configuration
    Given a configuration class "JacksonConfig" in package "com.cargo.booking.config"
    Then it must configure the ObjectMapper with:
      | setting                                                | purpose                              |
      | SerializationFeature.WRITE_DATES_AS_TIMESTAMPS = false | ISO-8601 date format                 |
      | DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false | Ignore unknown fields in requests |
      | JavaTimeModule registered                               | Support for Java 8+ date/time types |
      | inclusion = JsonInclude.Include.NON_NULL                | Omit null fields from responses      |

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @api @out-of-scope
  Scenario: Items NOT covered in API endpoints
    Given this is the API endpoints file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                                      | deferred to           |
      | Authentication and authorization on endpoints | 006_security.md    |
      | Global exception handler and error mapping | 007_error_handling.md  |
      | Real external service client implementations | 008_integrations.md |
      | Controller integration tests               | 009_testing.md         |
