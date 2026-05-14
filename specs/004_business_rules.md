# File: 004_business_rules.md
# Depends on: 001_project_setup.md, 002_domain_model.md, 003_data_access.md
# Produces: Service classes, domain event classes, event publisher, booking reference generator,
#           state machine logic, validation services, external service client interfaces
# Context: Defines the business logic, orchestration, state transitions, and domain event
#          emission for the Cargo Booking Service. The AI agent must have processed 001–003
#          so that entities, enums, repositories, and conventions are known.

Feature: Business Rules and Service Layer
  As an AI code generator
  I need to implement the business logic, lifecycle management, and event emission for bookings
  So that the Booking Service enforces all domain rules and notifies downstream systems

  Background:
    Given the base package is "com.cargo.booking"
    And service classes reside in "com.cargo.booking.service"
    And event classes reside in "com.cargo.booking.event"
    And client interfaces reside in "com.cargo.booking.client"
    And all conventions from 001_project_setup.md apply
    And all entities and enums from 002_domain_model.md are available
    And all repositories from 003_data_access.md are available

  # ---------------------------------------------------------------------------
  # BookingService — Core Orchestrator
  # ---------------------------------------------------------------------------

  @business @service
  Scenario: BookingService class definition
    Given a service class "BookingService" in package "com.cargo.booking.service"
    Then it must be annotated with @Service
    And it must use constructor injection for all dependencies
    And it must have a SLF4J logger
    And its dependencies must include:
      | dependency                    | purpose                                    |
      | BookingRepository             | Persistence of booking records              |
      | BookingReferenceGenerator     | Generate unique booking references          |
      | BookingEventPublisher         | Publish domain events to Kafka              |
      | ScheduleClient                | Validate schedule availability              |
      | EquipmentClient               | Reserve equipment on confirmation           |
      | QuoteClient                   | Validate quote validity                     |

  # ---------------------------------------------------------------------------
  # Create Booking
  # ---------------------------------------------------------------------------

  @business @create
  Scenario: Create a new booking — happy path
    Given a valid booking request with scheduleId, quoteId, customer details, cargo, and equipment
    When the BookingService.createBooking() method is called
    Then the service must perform these steps in order:
      | step | action                                                                  |
      | 1    | Validate the schedule exists and is open by calling ScheduleClient      |
      | 2    | Validate the quote is valid and matches the booking by calling QuoteClient |
      | 3    | Generate a unique booking reference using BookingReferenceGenerator     |
      | 4    | Build a Booking entity with status PENDING                              |
      | 5    | Build BookingEquipmentLine entities from the equipment list             |
      | 6    | Associate equipment lines with the booking                              |
      | 7    | Save the booking (cascade saves equipment lines)                        |
      | 8    | Publish a "booking.created" event via BookingEventPublisher             |
      | 9    | Return the saved booking                                                |
    And the entire operation must be wrapped in @Transactional
    And the event must be published after the transaction commits (use @TransactionalEventListener with AFTER_COMMIT phase)

  @business @create
  Scenario: Create booking — schedule not found or closed
    Given a booking request with an invalid or closed scheduleId
    When the BookingService.createBooking() method is called
    Then it must throw a ScheduleNotAvailableException with a descriptive message
    And the booking must NOT be persisted
    And no event must be published

  @business @create
  Scenario: Create booking — quote invalid or expired
    Given a booking request with an invalid or expired quoteId
    When the BookingService.createBooking() method is called
    Then it must throw a QuoteNotValidException with a descriptive message
    And the booking must NOT be persisted
    And no event must be published

  @business @create
  Scenario: Create booking — empty equipment list
    Given a booking request with an empty equipment list
    When the BookingService.createBooking() method is called
    Then it must throw a BookingValidationException with message "At least one equipment line is required"
    And the booking must NOT be persisted

  # ---------------------------------------------------------------------------
  # Get Booking
  # ---------------------------------------------------------------------------

  @business @read
  Scenario: Get booking by ID
    Given a valid booking ID (Long)
    When the BookingService.getBookingById() method is called
    Then it must use the repository method findWithEquipmentLinesById() to eagerly load equipment lines
    And if the booking is not found it must throw a BookingNotFoundException
    And the method must be annotated with @Transactional(readOnly = true)

  @business @read
  Scenario: Get booking by reference
    Given a valid booking reference (e.g. "BKG-2026-00042")
    When the BookingService.getBookingByReference() method is called
    Then it must use the repository method findWithEquipmentLinesByBookingReference()
    And if the booking is not found it must throw a BookingNotFoundException
    And the method must be annotated with @Transactional(readOnly = true)

  @business @read
  Scenario: List bookings for a customer
    Given a customerId and optional status filter
    When the BookingService.getBookingsByCustomer() method is called
    Then it must accept a Pageable parameter for pagination and sorting
    And if status is provided it must use findByCustomerIdAndStatus()
    And if status is null it must use findByCustomerId()
    And the method must be annotated with @Transactional(readOnly = true)

  # ---------------------------------------------------------------------------
  # State Transitions
  # ---------------------------------------------------------------------------

  @business @lifecycle
  Scenario: Confirm a booking
    Given a booking with status PENDING
    When the BookingService.confirmBooking() method is called with the booking ID
    Then the service must perform these steps in order:
      | step | action                                                                     |
      | 1    | Load the booking with equipment lines                                      |
      | 2    | Validate the current status is PENDING (throw IllegalStateTransitionException otherwise) |
      | 3    | Call EquipmentClient to reserve the equipment listed in the booking         |
      | 4    | Update the booking status to CONFIRMED                                     |
      | 5    | Update the updatedAt timestamp                                             |
      | 6    | Save the booking                                                           |
      | 7    | Publish a "booking.confirmed" event                                        |
    And the entire operation must be wrapped in @Transactional
    And if equipment reservation fails it must throw an EquipmentReservationException and NOT change the status

  @business @lifecycle
  Scenario: Start a booking (mark as in progress)
    Given a booking with status CONFIRMED
    When the BookingService.startBooking() method is called with the booking ID
    Then the service must:
      | step | action                                                                     |
      | 1    | Load the booking                                                           |
      | 2    | Validate the current status is CONFIRMED                                   |
      | 3    | Update the booking status to IN_PROGRESS                                   |
      | 4    | Save the booking                                                           |
      | 5    | Publish a "booking.in_progress" event                                      |

  @business @lifecycle
  Scenario: Complete a booking
    Given a booking with status IN_PROGRESS
    When the BookingService.completeBooking() method is called with the booking ID
    Then the service must:
      | step | action                                                                     |
      | 1    | Load the booking                                                           |
      | 2    | Validate the current status is IN_PROGRESS                                 |
      | 3    | Update the booking status to COMPLETED                                     |
      | 4    | Save the booking                                                           |
      | 5    | Publish a "booking.completed" event                                        |

  @business @lifecycle
  Scenario: Cancel a booking
    Given a booking with status PENDING or CONFIRMED
    When the BookingService.cancelBooking() method is called with the booking ID
    Then the service must:
      | step | action                                                                     |
      | 1    | Load the booking with equipment lines                                      |
      | 2    | Validate the current status is PENDING or CONFIRMED                        |
      | 3    | If status was CONFIRMED, call EquipmentClient to release the reserved equipment |
      | 4    | Update the booking status to CANCELLED                                     |
      | 5    | Save the booking                                                           |
      | 6    | Publish a "booking.cancelled" event                                        |
    And if equipment release fails the cancellation must still proceed (log a warning, do not throw)

  @business @lifecycle
  Scenario: Reject invalid state transitions
    Given a booking with any status
    When a state transition is attempted that is not in the allowed transitions from 002_domain_model.md
    Then the service must throw an IllegalStateTransitionException
    And the exception message must include the current status and the attempted target status
    And the booking must NOT be modified
    And no event must be published

  # ---------------------------------------------------------------------------
  # State Transition Helper
  # ---------------------------------------------------------------------------

  @business @lifecycle
  Scenario: State transition validation method
    Given the BookingService or a dedicated BookingStateMachine class
    Then a method must exist that validates whether a transition is allowed:
      | method signature                                                    |
      | void validateTransition(BookingStatus current, BookingStatus target) |
    And it must use the transition rules defined in 002_domain_model.md
    And it must throw IllegalStateTransitionException for any disallowed transition
    And this method must be called before any status change in all lifecycle methods

  # ---------------------------------------------------------------------------
  # Booking Reference Generator
  # ---------------------------------------------------------------------------

  @business @reference
  Scenario: BookingReferenceGenerator service
    Given a service class "BookingReferenceGenerator" in package "com.cargo.booking.service"
    Then it must be annotated with @Service
    And it must depend on BookingRepository (to access the native sequence query)
    And it must have a public method:
      | method signature              | returns | description                            |
      | generateReference()           | String  | Returns a unique booking reference      |
    And the method must:
      | step | action                                                                    |
      | 1    | Fetch the next value from booking_reference_seq via the repository        |
      | 2    | Get the current year in UTC                                               |
      | 3    | Format the reference as "BKG-{YEAR}-{SEQ}" where SEQ is zero-padded to 5 digits |
      | 4    | Return the formatted reference                                           |
    And the method must be safe for concurrent calls (the database sequence handles this)

  # ---------------------------------------------------------------------------
  # Domain Events
  # ---------------------------------------------------------------------------

  @business @events
  Scenario: Base domain event class
    Given an abstract class or sealed interface "BookingEvent" in package "com.cargo.booking.event"
    Then it must contain the following common fields:
      | field             | type    | description                           |
      | bookingReference  | String  | The human-readable booking reference  |
      | scheduleId        | Long    | The schedule this booking is linked to|
      | status            | String  | The booking status after the event    |
      | timestamp         | Instant | When the event occurred (UTC)         |

  @business @events
  Scenario: Concrete event classes
    Given the base BookingEvent class
    Then the following concrete event classes must be created in "com.cargo.booking.event":
      | class                  | additional fields                                 | description                     |
      | BookingCreatedEvent    | customerId, customerName, cargoWeightKg, equipment| Emitted when a booking is created |
      | BookingConfirmedEvent  | equipment                                         | Emitted when booking is confirmed |
      | BookingCancelledEvent  | cancelledBy (String: "CUSTOMER" or "SYSTEM")      | Emitted when booking is cancelled |
      | BookingCompletedEvent  | (none beyond base)                                | Emitted when shipment completes   |
      | BookingInProgressEvent | (none beyond base)                                | Emitted when shipment starts      |
    And the "equipment" field must be a list of objects with "type" (String) and "quantity" (int)
    And all event classes must be serializable to JSON
    And all must use @Builder and @Data from Lombok

  @business @events
  Scenario: BookingEventPublisher service
    Given a service class "BookingEventPublisher" in package "com.cargo.booking.event"
    Then it must be annotated with @Service
    And it must depend on KafkaTemplate<String, Object>
    And it must define the following Kafka topics as constants or configuration properties:
      | constant          | topic name         |
      | TOPIC_CREATED     | booking.created    |
      | TOPIC_CONFIRMED   | booking.confirmed  |
      | TOPIC_CANCELLED   | booking.cancelled  |
      | TOPIC_COMPLETED   | booking.completed  |
      | TOPIC_IN_PROGRESS | booking.in_progress|
    And it must provide a publish method for each event type:
      | method                                        | topic              |
      | publishCreated(BookingCreatedEvent event)      | booking.created    |
      | publishConfirmed(BookingConfirmedEvent event)  | booking.confirmed  |
      | publishCancelled(BookingCancelledEvent event)  | booking.cancelled  |
      | publishCompleted(BookingCompletedEvent event)  | booking.completed  |
      | publishInProgress(BookingInProgressEvent event)| booking.in_progress|
    And the Kafka message key must be the bookingReference
    And each publish method must log the event at INFO level before sending
    And failures to publish must be logged at ERROR level but must NOT roll back the transaction

  # ---------------------------------------------------------------------------
  # Kafka Configuration
  # ---------------------------------------------------------------------------

  @business @events @config
  Scenario: Kafka producer configuration
    Given a configuration class "KafkaConfig" in package "com.cargo.booking.config"
    Then it must be annotated with @Configuration
    And it must configure a KafkaTemplate<String, Object> bean
    And the value serializer must use JsonSerializer
    And it must configure topic auto-creation for all five topics
    And each topic must have:
      | property    | value |
      | partitions  | 3     |
      | replicas    | 1     |

  # ---------------------------------------------------------------------------
  # External Service Clients (Interfaces)
  # ---------------------------------------------------------------------------

  @business @client
  Scenario: ScheduleClient interface
    Given an interface "ScheduleClient" in package "com.cargo.booking.client"
    Then it must define the following methods:
      | method                                        | returns  | description                                   |
      | validateSchedule(Long scheduleId)             | boolean  | Returns true if schedule exists and is open    |
      | getScheduleDetails(Long scheduleId)           | ScheduleDTO | Fetches schedule details for validation     |
    And a stub implementation "ScheduleClientStub" must be created in the same package
    And the stub must be annotated with @Service and @Profile("local")
    And the stub must always return true / a dummy ScheduleDTO for local development
    And it must log a warning that it is using a stub implementation

  @business @client
  Scenario: EquipmentClient interface
    Given an interface "EquipmentClient" in package "com.cargo.booking.client"
    Then it must define the following methods:
      | method                                                              | returns | description                              |
      | reserveEquipment(Long bookingId, List<EquipmentLineDTO> equipment)  | void    | Reserve containers for a confirmed booking|
      | releaseEquipment(Long bookingId)                                    | void    | Release containers on cancellation        |
    And a stub implementation "EquipmentClientStub" must be created
    And the stub must be annotated with @Service and @Profile("local")
    And the stub must log the reservation/release action without calling any external service

  @business @client
  Scenario: QuoteClient interface
    Given an interface "QuoteClient" in package "com.cargo.booking.client"
    Then it must define the following methods:
      | method                                                        | returns  | description                                    |
      | validateQuote(Long quoteId, Long scheduleId, BigDecimal weightKg) | boolean  | Returns true if quote is valid and matches booking |
    And a stub implementation "QuoteClientStub" must be created
    And the stub must be annotated with @Service and @Profile("local")
    And the stub must always return true for local development

  # ---------------------------------------------------------------------------
  # Client DTOs
  # ---------------------------------------------------------------------------

  @business @client @dto
  Scenario: DTOs used by external service clients
    Given the client interfaces above
    Then the following DTOs must be created in "com.cargo.booking.client.dto":
      | class             | fields                                                    | notes                     |
      | ScheduleDTO       | id (Long), routeName (String), departureDate (Instant), status (String) | Returned by ScheduleClient |
      | EquipmentLineDTO  | type (String), quantity (int)                             | Used in EquipmentClient    |
    And these must be Java records (immutable)

  # ---------------------------------------------------------------------------
  # Custom Exceptions
  # ---------------------------------------------------------------------------

  @business @exception
  Scenario: Custom exception classes
    Given the package "com.cargo.booking.exception"
    Then the following exception classes must be created:
      | class                             | extends                  | description                                      |
      | BookingNotFoundException          | RuntimeException         | Thrown when a booking ID or reference is not found|
      | IllegalStateTransitionException   | RuntimeException         | Thrown for invalid booking state transitions      |
      | ScheduleNotAvailableException     | RuntimeException         | Thrown when schedule validation fails             |
      | QuoteNotValidException            | RuntimeException         | Thrown when quote validation fails                |
      | EquipmentReservationException     | RuntimeException         | Thrown when equipment reservation fails           |
      | BookingValidationException        | RuntimeException         | Thrown for general booking validation errors      |
    And each exception must have at least a constructor accepting a String message
    And each exception must have a constructor accepting a String message and a Throwable cause

  # ---------------------------------------------------------------------------
  # Logging Standards
  # ---------------------------------------------------------------------------

  @business @logging
  Scenario: Logging in the service layer
    Given all service classes
    Then the following logging rules must apply:
      | level | when                                                          |
      | INFO  | A booking is successfully created (log bookingReference)      |
      | INFO  | A booking state transition succeeds (log reference, from, to) |
      | INFO  | A domain event is published (log event type and reference)    |
      | WARN  | A stub client is used instead of a real integration           |
      | WARN  | Equipment release fails during cancellation (log and continue)|
      | ERROR | An external service call fails unexpectedly                   |
      | DEBUG | Incoming request details (scheduleId, quoteId, equipment)     |
    And log messages must never include sensitive customer data (email, phone)
    And log messages must include the bookingReference when available for traceability

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @business @out-of-scope
  Scenario: Items NOT covered in business rules
    Given this is the business rules file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                                  | deferred to            |
      | REST controller endpoints              | 005_api_endpoints.md   |
      | Request/response DTO definitions       | 005_api_endpoints.md   |
      | Security and authorization rules       | 006_security.md        |
      | Global exception handler mapping       | 007_error_handling.md  |
      | Real external service implementations  | 008_integrations.md    |
      | Unit and integration tests             | 009_testing.md         |
