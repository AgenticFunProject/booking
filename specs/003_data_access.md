# Data Access Layer

## Metadata

| Field | Value |
| --- | --- |
| File | 003_data_access.md |
| Depends on | 001_project_setup.md, 002_domain_model.md |
| Produces | Repository interfaces, custom queries, pagination/sorting config, specifications |
| Context | Defines how the Booking Service reads and writes data. The AI agent must have processed 001 and 002 so that entities, enums, and conventions are known. |

## Goal

- As an AI code generator
- I need to define Spring Data JPA repositories and query methods for the Booking Service
- So that the service layer has a clean, tested persistence interface

## Background

- Given the base package is "com.cargo.booking"
- And all repositories reside in "com.cargo.booking.repository"
- And all entities and enums are defined in 002_domain_model.md
- And Spring Data JPA is on the classpath as defined in 001_project_setup.md

## Booking Repository

### BookingRepository interface

Tags: `data-access`, `repository`

- Given a repository interface "BookingRepository" in package "com.cargo.booking.repository"
- Then it must extend JpaRepository<Booking, Long>
- And it must also extend JpaSpecificationExecutor<Booking> for dynamic filtering
- And it must be annotated with @Repository

### Find booking by reference

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                              | returns          | description                          |
| --- | --- | --- |
| findByBookingReference(String bookingReference)     | Optional<Booking>| Lookup by human-readable reference   |

### Find bookings by customer

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                                                          | returns        | description                           |
| --- | --- | --- |
| findByCustomerId(Long customerId, Pageable pageable)                            | Page<Booking>  | Paginated list of customer bookings   |

### Find bookings by status

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                                                          | returns        | description                           |
| --- | --- | --- |
| findByStatus(BookingStatus status, Pageable pageable)                           | Page<Booking>  | Paginated list filtered by status     |

### Find bookings by customer and status

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                                                                          | returns        | description                                    |
| --- | --- | --- |
| findByCustomerIdAndStatus(Long customerId, BookingStatus status, Pageable pageable)             | Page<Booking>  | Paginated list filtered by customer and status  |

### Find bookings by schedule

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                              | returns         | description                                   |
| --- | --- | --- |
| findByScheduleId(Long scheduleId)                   | List<Booking>   | All bookings linked to a specific schedule     |

### Check if booking reference already exists

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                              | returns  | description                                  |
| --- | --- | --- |
| existsByBookingReference(String bookingReference)   | boolean  | Check uniqueness before persisting            |

### Count bookings by status for a customer

Tags: `data-access`, `query`

- Given the BookingRepository interface
- Then it must define the following query method:

| method                                                          | returns | description                              |
| --- | --- | --- |
| countByCustomerIdAndStatus(Long customerId, BookingStatus status)| long    | Count bookings in a given state          |

## BookingEquipmentLine Repository

### BookingEquipmentLineRepository interface

Tags: `data-access`, `repository`

- Given a repository interface "BookingEquipmentLineRepository" in package "com.cargo.booking.repository"
- Then it must extend JpaRepository<BookingEquipmentLine, Long>
- And it must be annotated with @Repository

### Find equipment lines by booking

Tags: `data-access`, `query`

- Given the BookingEquipmentLineRepository interface
- Then it must define the following query method:

| method                                              | returns                     | description                            |
| --- | --- | --- |
| findByBookingId(Long bookingId)                     | List<BookingEquipmentLine>  | All equipment lines for a booking      |

### Delete equipment lines by booking

Tags: `data-access`, `query`

- Given the BookingEquipmentLineRepository interface
- Then it must define the following method:

| method                                  | returns | description                                        |
| --- | --- | --- |
| deleteByBookingId(Long bookingId)        | void    | Remove all equipment lines when a booking changes  |

- And this method must be annotated with @Modifying and @Transactional

## Booking Reference Counter

### Fetch next booking reference sequence value for year

Tags: `data-access`, `sequence`

- Given a custom repository component "BookingReferenceCounterRepository" in package "com.cargo.booking.repository"
- Then it must define a method to atomically fetch and increment the next yearly counter value:

| method                         | returns | description                                  |
| --- | --- | --- |
| getNextReferenceSeqForYear(int year) | Long    | Next value for the given UTC calendar year   |

- And the method must be implemented with EntityManager or JdbcClient using native PostgreSQL SQL
- And the native SQL must insert a row for a new year with next_value = 2 and return 1
- And for an existing year it must increment next_value by 1 and return the previous value
- And it must rely on PostgreSQL INSERT ... ON CONFLICT ... DO UPDATE semantics for concurrency safety
- And the method must be annotated or executed within @Transactional

## Pagination and Sorting Defaults

### Default pagination configuration

Tags: `data-access`, `pagination`

- Given the Booking Service API accepts paginated requests
- Then the default page size must be 20
- And the maximum page size must be 100
- And the default sort field must be "createdAt"
- And the default sort direction must be DESC (newest first)
- And pagination must be configured via Spring Boot properties:

| property                                          | value |
| --- | --- |
| spring.data.web.pageable.default-page-size        | 20    |
| spring.data.web.pageable.max-page-size            | 100   |
| spring.data.web.pageable.one-indexed-parameters   | false |

## Entity Fetching Strategy

### Eager and lazy loading rules

Tags: `data-access`, `fetching`

- Given the entity relationships defined in 002_domain_model.md
- Then the following fetch strategies must be applied:

| entity               | relationship     | fetch type | reason                                              |
| --- | --- | --- | --- |
| Booking              | equipmentLines   | LAZY       | Not always needed; load explicitly when required     |
| BookingEquipmentLine | booking          | LAZY       | Prevent N+1; parent is already in context            |

### Custom query to fetch booking with equipment lines

Tags: `data-access`, `fetching`

- Given the BookingRepository interface
- Then it must define a JPQL query to eagerly fetch equipment lines:

| method                                                          | annotation                                                                                     | returns          |
| --- | --- | --- |
| findWithEquipmentLinesById(Long id)                             | @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.equipmentLines WHERE b.id = :id")           | Optional<Booking>|
| findWithEquipmentLinesByBookingReference(String reference)      | @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.equipmentLines WHERE b.bookingReference = :reference") | Optional<Booking>|

## Transactional Boundaries

### Transaction management rules

Tags: `data-access`, `transactions`

- Given the data access layer
- Then the following transaction rules must apply:

| rule                                                                                              |
| --- |
| All read-only queries should use @Transactional(readOnly = true) at the service layer             |
| All write operations should use @Transactional at the service layer                               |
| Repository interfaces themselves must NOT declare @Transactional unless the method performs a custom modifying query |
| deleteByBookingId needs @Modifying and @Transactional                                            |
| getNextReferenceSeqForYear must be implemented as a custom repository method returning Long, not as a Spring Data @Modifying query |

## Specifications for Dynamic Filtering (Optional Advanced Queries)

### BookingSpecification class for dynamic filtering

Tags: `data-access`, `specification`

- Given a class "BookingSpecification" in package "com.cargo.booking.repository"
- Then it must provide static methods that return Specification<Booking>:

| method                                          | entity field        | description                                  |
| --- | --- | --- |
| hasCustomerId(Long customerId)                  | customerId          | Exact match on customer                      |
| hasStatus(BookingStatus status)                 | status              | Exact match on status                        |
| hasScheduleId(Long scheduleId)                  | scheduleId          | Exact match on schedule                      |
| createdAfter(Instant from)                      | createdAt           | Bookings created on or after a timestamp     |
| createdBefore(Instant to)                       | createdAt           | Bookings created on or before a timestamp    |

- And JPA Criteria paths must use entity field names, not database column names
- And each method must return null when the input parameter is null (to allow optional filtering)
- And these specifications must be composable using Specification.where().and()

## Out of Scope for this file

### Items NOT covered in data access

Tags: `data-access`, `out-of-scope`

- Given this is the data access file only
- Then the following are NOT defined here and will be addressed in later files:

| topic                                      | deferred to            |
| --- | --- |
| Service layer orchestration and validation  | 004_business_rules.md |
| DTO mapping from query results              | 005_api_endpoints.md  |
| Caching strategies                          | 008_integrations.md   |
| Test data setup and repository tests        | 009_testing.md        |
