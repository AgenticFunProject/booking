# File: 003_data_access.md
# Depends on: 001_project_setup.md, 002_domain_model.md
# Produces: Repository interfaces, custom queries, pagination/sorting config, specifications
# Context: Defines how the Booking Service reads and writes data. The AI agent must have
#          processed 001 and 002 so that entities, enums, and conventions are known.

Feature: Data Access Layer
  As an AI code generator
  I need to define Spring Data JPA repositories and query methods for the Booking Service
  So that the service layer has a clean, tested persistence interface

  Background:
    Given the base package is "com.cargo.booking"
    And all repositories reside in "com.cargo.booking.repository"
    And all entities and enums are defined in 002_domain_model.md
    And Spring Data JPA is on the classpath as defined in 001_project_setup.md

  # ---------------------------------------------------------------------------
  # Booking Repository
  # ---------------------------------------------------------------------------

  @data-access @repository
  Scenario: BookingRepository interface
    Given a repository interface "BookingRepository" in package "com.cargo.booking.repository"
    Then it must extend JpaRepository<Booking, Long>
    And it must also extend JpaSpecificationExecutor<Booking> for dynamic filtering
    And it must be annotated with @Repository

  @data-access @query
  Scenario: Find booking by reference
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                              | returns          | description                          |
      | findByBookingReference(String bookingReference)     | Optional<Booking>| Lookup by human-readable reference   |

  @data-access @query
  Scenario: Find bookings by customer
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                                                          | returns        | description                           |
      | findByCustomerId(Long customerId, Pageable pageable)                            | Page<Booking>  | Paginated list of customer bookings   |

  @data-access @query
  Scenario: Find bookings by status
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                                                          | returns        | description                           |
      | findByStatus(BookingStatus status, Pageable pageable)                           | Page<Booking>  | Paginated list filtered by status     |

  @data-access @query
  Scenario: Find bookings by customer and status
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                                                                          | returns        | description                                    |
      | findByCustomerIdAndStatus(Long customerId, BookingStatus status, Pageable pageable)             | Page<Booking>  | Paginated list filtered by customer and status  |

  @data-access @query
  Scenario: Find bookings by schedule
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                              | returns         | description                                   |
      | findByScheduleId(Long scheduleId)                   | List<Booking>   | All bookings linked to a specific schedule     |

  @data-access @query
  Scenario: Check if booking reference already exists
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                              | returns  | description                                  |
      | existsByBookingReference(String bookingReference)   | boolean  | Check uniqueness before persisting            |

  @data-access @query
  Scenario: Count bookings by status for a customer
    Given the BookingRepository interface
    Then it must define the following query method:
      | method                                                          | returns | description                              |
      | countByCustomerIdAndStatus(Long customerId, BookingStatus status)| long    | Count bookings in a given state          |

  # ---------------------------------------------------------------------------
  # BookingEquipmentLine Repository
  # ---------------------------------------------------------------------------

  @data-access @repository
  Scenario: BookingEquipmentLineRepository interface
    Given a repository interface "BookingEquipmentLineRepository" in package "com.cargo.booking.repository"
    Then it must extend JpaRepository<BookingEquipmentLine, Long>
    And it must be annotated with @Repository

  @data-access @query
  Scenario: Find equipment lines by booking
    Given the BookingEquipmentLineRepository interface
    Then it must define the following query method:
      | method                                              | returns                     | description                            |
      | findByBookingId(Long bookingId)                     | List<BookingEquipmentLine>  | All equipment lines for a booking      |

  @data-access @query
  Scenario: Delete equipment lines by booking
    Given the BookingEquipmentLineRepository interface
    Then it must define the following method:
      | method                                  | returns | description                                        |
      | deleteByBookingId(Long bookingId)        | void    | Remove all equipment lines when a booking changes  |
    And this method must be annotated with @Modifying and @Transactional

  # ---------------------------------------------------------------------------
  # Booking Reference Counter
  # ---------------------------------------------------------------------------

  @data-access @sequence
  Scenario: Fetch next booking reference sequence value for year
    Given the BookingRepository interface
    Then it must define a native query to atomically fetch and increment the next yearly counter value:
      | method                         | returns | description                                  |
      | getNextReferenceSeqForYear(int year) | Long    | Next value for the given UTC calendar year   |
    And the native query must insert a row for a new year with next_value = 2 and return 1
    And for an existing year it must increment next_value by 1 and return the previous value
    And it must rely on PostgreSQL INSERT ... ON CONFLICT ... DO UPDATE semantics for concurrency safety

  # ---------------------------------------------------------------------------
  # Pagination and Sorting Defaults
  # ---------------------------------------------------------------------------

  @data-access @pagination
  Scenario: Default pagination configuration
    Given the Booking Service API accepts paginated requests
    Then the default page size must be 20
    And the maximum page size must be 100
    And the default sort field must be "createdAt"
    And the default sort direction must be DESC (newest first)
    And pagination must be configured via Spring Boot properties:
      | property                                          | value |
      | spring.data.web.pageable.default-page-size        | 20    |
      | spring.data.web.pageable.max-page-size            | 100   |
      | spring.data.web.pageable.one-indexed-parameters   | false |

  # ---------------------------------------------------------------------------
  # Entity Fetching Strategy
  # ---------------------------------------------------------------------------

  @data-access @fetching
  Scenario: Eager and lazy loading rules
    Given the entity relationships defined in 002_domain_model.md
    Then the following fetch strategies must be applied:
      | entity               | relationship     | fetch type | reason                                              |
      | Booking              | equipmentLines   | LAZY       | Not always needed; load explicitly when required     |
      | BookingEquipmentLine | booking          | LAZY       | Prevent N+1; parent is already in context            |

  @data-access @fetching
  Scenario: Custom query to fetch booking with equipment lines
    Given the BookingRepository interface
    Then it must define a JPQL query to eagerly fetch equipment lines:
      | method                                                          | annotation                                                                                     | returns          |
      | findWithEquipmentLinesById(Long id)                             | @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.equipmentLines WHERE b.id = :id")           | Optional<Booking>|
      | findWithEquipmentLinesByBookingReference(String reference)      | @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.equipmentLines WHERE b.bookingReference = :reference") | Optional<Booking>|

  # ---------------------------------------------------------------------------
  # Transactional Boundaries
  # ---------------------------------------------------------------------------

  @data-access @transactions
  Scenario: Transaction management rules
    Given the data access layer
    Then the following transaction rules must apply:
      | rule                                                                                              |
      | All read-only queries should use @Transactional(readOnly = true) at the service layer             |
      | All write operations should use @Transactional at the service layer                               |
      | Repository interfaces themselves must NOT declare @Transactional unless the method performs a custom modifying query |
      | deleteByBookingId needs @Modifying and @Transactional                                            |
      | getNextReferenceSeqForYear needs @Modifying and @Transactional because it atomically updates the yearly counter |

  # ---------------------------------------------------------------------------
  # Specifications for Dynamic Filtering (Optional Advanced Queries)
  # ---------------------------------------------------------------------------

  @data-access @specification
  Scenario: BookingSpecification class for dynamic filtering
    Given a class "BookingSpecification" in package "com.cargo.booking.repository"
    Then it must provide static methods that return Specification<Booking>:
      | method                                          | filters by          | description                                  |
      | hasCustomerId(Long customerId)                  | customer_id         | Exact match on customer                      |
      | hasStatus(BookingStatus status)                 | status              | Exact match on status                        |
      | hasScheduleId(Long scheduleId)                  | schedule_id         | Exact match on schedule                      |
      | createdAfter(Instant from)                      | created_at          | Bookings created on or after a timestamp     |
      | createdBefore(Instant to)                       | created_at          | Bookings created on or before a timestamp    |
    And each method must return null when the input parameter is null (to allow optional filtering)
    And these specifications must be composable using Specification.where().and()

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @data-access @out-of-scope
  Scenario: Items NOT covered in data access
    Given this is the data access file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                                      | deferred to            |
      | Service layer orchestration and validation  | 004_business_rules.md |
      | DTO mapping from query results              | 005_api_endpoints.md  |
      | Caching strategies                          | 008_integrations.md   |
      | Test data setup and repository tests        | 009_testing.md        |
