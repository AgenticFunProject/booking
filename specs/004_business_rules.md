# Business Rules and Service Layer

## Metadata

| Field | Value |
| --- | --- |
| File | 004_business_rules.md |
| Depends on | 001_project_setup.md, 002_domain_model.md, 003_data_access.md |
| Produces | Service classes, booking reference generator, state machine logic, validation services, external service client interfaces |
| Context | Defines the business logic, orchestration, and state transitions for the Cargo Booking Service. Messaging/event streaming is intentionally out of scope for v1 and may be added later. |

## Goal

- As an AI code generator
- I need to implement the business logic and lifecycle management for bookings
- So that the Booking Service enforces all domain rules consistently

## Background

- Given the base package is "com.cargo.booking"
- And service classes reside in "com.cargo.booking.service"
- And client interfaces reside in "com.cargo.booking.client"
- And all conventions from 001_project_setup.md apply
- And all entities and enums from 002_domain_model.md are available
- And all repositories from 003_data_access.md are available

## BookingService - Core Orchestrator

### BookingService class definition

Tags: `business`, `service`

- Given a service class "BookingService" in package "com.cargo.booking.service"
- Then it must be annotated with @Service
- And it must use constructor injection for all dependencies
- And it must have a SLF4J logger
- And its dependencies must include:

| dependency                | purpose                          |
| --- | --- |
| BookingRepository         | Persistence of booking records   |
| BookingReferenceGenerator | Generate unique booking refs     |
| ScheduleClient            | Validate schedule availability   |
| EquipmentClient           | Reserve equipment on confirmation|
| QuoteClient               | Validate quote validity          |
| BookingStateMachine       | Validate lifecycle transitions   |

## Create Booking

### Create a new booking - happy path

Tags: `business`, `create`

- Given a valid booking request with customerId, scheduleId, quoteId, customer details, cargo, and equipment
- When the BookingService.createBooking() method is called with the request
- Then the service must perform these steps in order:

| step | action                                                                  |
| --- | --- |
| 1    | Validate the equipment list is not empty                                |
| 2    | Validate the request customerId is present                              |
| 3    | Validate every equipment type with EquipmentType.fromCode(type)         |
| 4    | Validate the schedule exists and is open by calling ScheduleClient      |
| 5    | Validate the quote is valid and matches the booking by calling QuoteClient |
| 6    | Generate a unique booking reference using BookingReferenceGenerator     |
| 7    | Build a Booking entity with status PENDING and customerId from the request |
| 8    | Build BookingEquipmentLine entities from the equipment list using parsed EquipmentType values |
| 9    | Associate equipment lines with the booking                              |
| 10   | Save the booking (cascade saves equipment lines)                       |
| 11   | Return the saved booking                                                |

- And the entire operation must be wrapped in @Transactional
- And authentication/authorization, when enabled, must be enforced before the service call by the API/security layer

### Create booking - schedule not found or closed

Tags: `business`, `create`

- Given a booking request with an invalid or closed scheduleId
- When the BookingService.createBooking() method is called with the request
- Then it must throw a ScheduleNotAvailableException with a descriptive message
- And the booking must NOT be persisted

### Create booking - quote invalid or expired

Tags: `business`, `create`

- Given a booking request with an invalid or expired quoteId
- When the BookingService.createBooking() method is called with the request
- Then it must throw a QuoteNotValidException with a descriptive message
- And the booking must NOT be persisted

### Create booking - empty equipment list

Tags: `business`, `create`

- Given a booking request with an empty equipment list
- When the BookingService.createBooking() method is called with the request
- Then it must throw a BookingValidationException with message "At least one equipment line is required"
- And the booking must NOT be persisted

### Create booking - unsupported equipment type

Tags: `business`, `create`

- Given a booking request with an equipment type that EquipmentType.fromCode(type) does not recognize
- When the BookingService.createBooking() method is called with the request
- Then it must throw a BookingValidationException with a message that identifies the unsupported equipment type
- And the booking must NOT be persisted

## Get Booking

### Get booking by ID

Tags: `business`, `read`

- Given a valid booking ID (Long)
- When the BookingService.getBookingById() method is called
- Then it must use the repository method findWithEquipmentLinesById() to eagerly load equipment lines
- And if the booking is not found it must throw a BookingNotFoundException
- And the method must be annotated with @Transactional(readOnly = true)

### Get booking by reference

Tags: `business`, `read`

- Given a valid booking reference (e.g. "BKG-2026-00042")
- When the BookingService.getBookingByReference() method is called
- Then it must use the repository method findWithEquipmentLinesByBookingReference()
- And if the booking is not found it must throw a BookingNotFoundException
- And the method must be annotated with @Transactional(readOnly = true)

### List bookings

Tags: `business`, `read`

- Given optional customerId and optional status filters
- When the BookingService.getBookings() method is called
- Then it must accept a Pageable parameter for pagination and sorting
- And if customerId and status are provided it must filter by both
- And if only customerId is provided it must filter by customer
- And if only status is provided it must filter by status
- And if neither filter is provided it must return all bookings matching no additional filters
- And caller visibility must be enforced by the API/security layer before calling the service
- And the method must be annotated with @Transactional(readOnly = true)

## State Transitions

### Confirm a booking

Tags: `business`, `lifecycle`

- Given a booking with status PENDING
- When the BookingService.confirmBooking() method is called with the booking ID
- Then the service must perform these steps in order:

| step | action                                                                     |
| --- | --- |
| 1    | Load the booking with equipment lines                                      |
| 2    | Validate PENDING → CONFIRMED with BookingStateMachine                   |
| 3    | Call EquipmentClient to reserve the equipment listed in the booking        |
| 4    | Update the booking status to CONFIRMED                                     |
| 5    | Save the booking                                                           |
| 6    | Return the saved booking                                                   |

- And the entire operation must be wrapped in @Transactional
- And if equipment reservation fails it must throw an EquipmentReservationException and NOT change the status

### Start a booking (mark as in progress)

Tags: `business`, `lifecycle`

- Given a booking with status CONFIRMED
- When the BookingService.startBooking() method is called with the booking ID
- Then the service must:

| step | action                                   |
| --- | --- |
| 1    | Load the booking                         |
| 2    | Validate CONFIRMED → IN_PROGRESS with BookingStateMachine |
| 3    | Update the booking status to IN_PROGRESS |
| 4    | Save the booking                         |
| 5    | Return the saved booking                 |

- And the entire operation must be wrapped in @Transactional

### Complete a booking

Tags: `business`, `lifecycle`

- Given a booking with status IN_PROGRESS
- When the BookingService.completeBooking() method is called with the booking ID
- Then the service must:

| step | action                                      |
| --- | --- |
| 1    | Load the booking                            |
| 2    | Validate IN_PROGRESS → COMPLETED with BookingStateMachine |
| 3    | Update the booking status to COMPLETED      |
| 4    | Save the booking                            |
| 5    | Return the saved booking                    |

- And the entire operation must be wrapped in @Transactional

### Cancel a booking

Tags: `business`, `lifecycle`

- Given a booking with status PENDING or CONFIRMED
- When the BookingService.cancelBooking() method is called with the booking ID
- Then the service must:

| step | action                                                                     |
| --- | --- |
| 1    | Load the booking with equipment lines                                      |
| 2    | Validate current status → CANCELLED with BookingStateMachine               |
| 3    | If status was CONFIRMED, call EquipmentClient to release the reserved equipment |
| 4    | Update the booking status to CANCELLED                                     |
| 5    | Save the booking                                                           |
| 6    | Return the saved booking                                                   |

- And the entire operation must be wrapped in @Transactional
- And if equipment release fails the cancellation must still proceed (log a warning, do not throw)

### Reject invalid state transitions

Tags: `business`, `lifecycle`

- Given a booking with any status
- When a state transition is attempted that is not in the allowed transitions from 002_domain_model.md
- Then the service must throw an IllegalStateTransitionException
- And the exception message must include the current status and the attempted target status
- And the booking must NOT be modified

## State Transition Helper

### State transition validation method

Tags: `business`, `lifecycle`

- Given a dedicated class "BookingStateMachine" in package "com.cargo.booking.service"
- Then it must be annotated with @Component
- And it must define a method that validates whether a transition is allowed:

| method signature                                                    |
| --- |
| void validateTransition(BookingStatus current, BookingStatus target) |

- And it must use the transition rules defined in 002_domain_model.md
- And it must throw IllegalStateTransitionException for any disallowed transition
- And BookingService must depend on BookingStateMachine and call it before any status change in all lifecycle methods

## Booking Reference Generator

### BookingReferenceGenerator service

Tags: `business`, `reference`

- Given a service class "BookingReferenceGenerator" in package "com.cargo.booking.service"
- Then it must be annotated with @Service
- And it must depend on BookingReferenceCounterRepository (to access the custom yearly counter query)
- And it must have a public method:

| method signature    | returns | description                       |
| --- | --- | --- |
| generateReference() | String  | Returns a unique booking reference|

- And the method must:

| step | action                                                                    |
| --- | --- |
| 1    | Get the current year in UTC                                               |
| 2    | Fetch the next counter value for that year via the repository             |
| 3    | Format the reference as "BKG-{YEAR}-{SEQ}" where SEQ is zero-padded to 5 digits |
| 4    | Return the formatted reference                                           |

- And the method must be safe for concurrent calls (the database counter upsert handles this)

## External Service Clients (Interfaces)

### ScheduleClient interface

Tags: `business`, `client`

- Given an interface "ScheduleClient" in package "com.cargo.booking.client"
- Then it must define the following methods:

| method                              | returns     | description                               |
| --- | --- | --- |
| validateSchedule(Long scheduleId)   | boolean     | Returns true if schedule exists and is open|
| getScheduleDetails(Long scheduleId) | ScheduleDTO | Fetches schedule details for validation    |

- And a stub implementation "ScheduleClientStub" must be created in the same package
- And the stub must be annotated with @Service and @Profile("local")
- And the stub must always return true / a dummy ScheduleDTO for local development
- And it must log a warning that it is using a stub implementation

### EquipmentClient interface

Tags: `business`, `client`

- Given an interface "EquipmentClient" in package "com.cargo.booking.client"
- Then it must define the following methods:

| method                                                             | returns | description                               |
| --- | --- | --- |
| reserveEquipment(Long bookingId, List<EquipmentLineDTO> equipment) | void    | Reserve containers for a confirmed booking |
| releaseEquipment(Long bookingId)                                   | void    | Release containers on cancellation         |

- And a stub implementation "EquipmentClientStub" must be created
- And the stub must be annotated with @Service and @Profile("local")
- And the stub must log the reservation/release action without calling any external service

### QuoteClient interface

Tags: `business`, `client`

- Given an interface "QuoteClient" in package "com.cargo.booking.client"
- Then it must define the following methods:

| method                                                         | returns | description                                    |
| --- | --- | --- |
| validateQuote(Long quoteId, Long scheduleId, BigDecimal weightKg) | boolean | Returns true if quote is valid and matches booking |

- And a stub implementation "QuoteClientStub" must be created
- And the stub must be annotated with @Service and @Profile("local")
- And the stub must always return true for local development

## Client DTOs

### DTOs used by external service clients

Tags: `business`, `client`, `dto`

- Given the client interfaces above
- Then the following DTOs must be created in "com.cargo.booking.client.dto":

| class            | fields                                                    | notes                   |
| --- | --- | --- |
| ScheduleDTO      | id (Long), routeName (String), departureDate (Instant), status (String) | Returned by ScheduleClient |
| EquipmentLineDTO | type (String), quantity (int)                             | Used in EquipmentClient |

- And these must be Java records (immutable)

## Custom Exceptions

### Custom exception classes

Tags: `business`, `exception`

- Given the package "com.cargo.booking.exception"
- Then the following exception classes must be created:

| class                           | extends          | description                                      |
| --- | --- | --- |
| BookingNotFoundException        | RuntimeException | Thrown when a booking ID or reference is not found|
| IllegalStateTransitionException | RuntimeException | Thrown for invalid booking state transitions      |
| ScheduleNotAvailableException   | RuntimeException | Thrown when schedule validation fails             |
| QuoteNotValidException          | RuntimeException | Thrown when quote validation fails                |
| EquipmentReservationException   | RuntimeException | Thrown when equipment reservation fails           |
| BookingValidationException      | RuntimeException | Thrown for general booking validation errors      |

- And each exception must have at least a constructor accepting a String message
- And each exception must have a constructor accepting a String message and a Throwable cause

## Logging Standards

### Logging in the service layer

Tags: `business`, `logging`

- Given all service classes
- Then the following logging rules must apply:

| level | when                                                          |
| --- | --- |
| INFO  | A booking is successfully created (log bookingReference)      |
| INFO  | A booking state transition succeeds (log reference, from, to) |
| WARN  | A stub client is used instead of a real integration           |
| WARN  | Equipment release fails during cancellation (log and continue)|
| ERROR | An external service call fails unexpectedly                   |
| DEBUG | Incoming request details (scheduleId, quoteId, equipment)     |

- And log messages must never include sensitive customer data (email, phone)
- And log messages must include the bookingReference when available for traceability

## Out of Scope for this file

### Items NOT covered in business rules

Tags: `business`, `out-of-scope`

- Given this is the business rules file only
- Then the following are NOT defined here and will be addressed in later files:

| topic                                 | deferred to           |
| --- | --- |
| REST controller endpoints             | 005_api_endpoints.md  |
| Request/response DTO definitions      | 005_api_endpoints.md  |
| Security and authorization rules      | 006_security.md       |
| Global exception handler mapping      | 007_error_handling.md |
| Real external service implementations | 008_integrations.md   |
| Messaging / event streaming           | Out of scope for v1   |
| Unit and integration tests            | 009_testing.md        |
