# File: 002_domain_model.md
# Depends on: 001_project_setup.md
# Produces: JPA entities, enums, embeddable types, Flyway migration scripts
# Context: Defines the Booking aggregate and all supporting value objects for the
#          Cargo Booking Service. The AI agent must have processed 001_project_setup.md
#          before this file so that package names, conventions, and dependencies are known.

Feature: Domain Model
  As an AI code generator
  I need to define the JPA entities, enums, and embeddable types for the Booking Service
  So that the persistence layer has a well-structured and validated data model

  Background:
    Given the base package is "com.cargo.booking"
    And all entities reside in "com.cargo.booking.model.entity"
    And all enums reside in "com.cargo.booking.model.enums"
    And database table names use lowercase_snake_case
    And Lombok annotations are used as defined in 001_project_setup.md

  # ---------------------------------------------------------------------------
  # Enums
  # ---------------------------------------------------------------------------

  @domain @enum
  Scenario: BookingStatus enum
    Given the enum "BookingStatus" in package "com.cargo.booking.model.enums"
    Then it must define the following values in this exact order:
      | value       | description                                    |
      | PENDING     | Booking received, awaiting confirmation         |
      | CONFIRMED   | Booking accepted, equipment reserved            |
      | IN_PROGRESS | Cargo picked up, shipment is active             |
      | COMPLETED   | Shipment delivered and equipment returned        |
      | CANCELLED   | Booking cancelled by customer or system          |

  @domain @enum
  Scenario: EquipmentType enum
    Given the enum "EquipmentType" in package "com.cargo.booking.model.enums"
    Then it must define the following values:
      | value  | description              |
      | 20FT   | 20-foot container        |
      | 40FT   | 40-foot container        |
      | 40HC   | 40-foot high cube        |
      | REEFER | Refrigerated container   |

  # ---------------------------------------------------------------------------
  # Booking Entity (Aggregate Root)
  # ---------------------------------------------------------------------------

  @domain @entity
  Scenario: Booking entity definition
    Given the entity "Booking" in package "com.cargo.booking.model.entity"
    And the table name is "bookings"
    Then it must be annotated with @Entity, @Table, @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
    And it must have the following fields:
      | field            | javaType       | column              | nullable | unique | notes                                          |
      | id               | Long           | id                  | false    | true   | @Id, @GeneratedValue(strategy = GenerationType.IDENTITY) |
      | bookingReference | String         | booking_reference   | false    | true   | Human-readable, format BKG-YYYY-NNNNN          |
      | status           | BookingStatus  | status              | false    | false  | @Enumerated(EnumType.STRING), default PENDING   |
      | scheduleId       | Long           | schedule_id         | false    | false  | References external Schedules API               |
      | quoteId          | Long           | quote_id            | false    | false  | References external Quotes API                  |
      | customerId       | Long           | customer_id         | false    | false  | Identifies the customer                         |
      | customerName     | String         | customer_name       | false    | false  | Max length 255                                  |
      | customerEmail    | String         | customer_email      | false    | false  | Max length 255, must be valid email format      |
      | customerPhone    | String         | customer_phone      | true     | false  | Max length 50                                   |
      | cargoDescription | String         | cargo_description   | false    | false  | Max length 1000                                 |
      | cargoWeightKg    | BigDecimal     | cargo_weight_kg     | false    | false  | Must be greater than 0, precision 10 scale 2    |
      | createdAt        | Instant        | created_at          | false    | false  | @CreationTimestamp, stored in UTC                |
      | updatedAt        | Instant        | updated_at          | false    | false  | @UpdateTimestamp, stored in UTC                  |

  @domain @entity
  Scenario: Booking entity relationship to equipment lines
    Given the entity "Booking"
    Then it must have a one-to-many relationship to "BookingEquipmentLine"
    And the field name must be "equipmentLines"
    And it must be annotated with @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    And it must be initialized as an empty ArrayList
    And it must use @Builder.Default to ensure the empty list is preserved by Lombok Builder

  @domain @entity
  Scenario: Booking entity auditing and indexing
    Given the entity "Booking"
    Then the table must have the following indexes:
      | name                          | columns            | unique |
      | idx_booking_reference         | booking_reference  | true   |
      | idx_booking_customer_id       | customer_id        | false  |
      | idx_booking_status            | status             | false  |
      | idx_booking_schedule_id       | schedule_id        | false  |

  # ---------------------------------------------------------------------------
  # BookingEquipmentLine Entity
  # ---------------------------------------------------------------------------

  @domain @entity
  Scenario: BookingEquipmentLine entity definition
    Given the entity "BookingEquipmentLine" in package "com.cargo.booking.model.entity"
    And the table name is "booking_equipment_lines"
    Then it must be annotated with @Entity, @Table, @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
    And it must have the following fields:
      | field     | javaType      | column       | nullable | notes                                                  |
      | id        | Long          | id           | false    | @Id, @GeneratedValue(strategy = GenerationType.IDENTITY)   |
      | type      | EquipmentType | type         | false    | @Enumerated(EnumType.STRING)                           |
      | quantity  | Integer       | quantity     | false    | Must be greater than 0                                 |

  @domain @entity
  Scenario: BookingEquipmentLine relationship to Booking
    Given the entity "BookingEquipmentLine"
    Then it must have a many-to-one relationship to "Booking"
    And the field name must be "booking"
    And it must be annotated with @ManyToOne(fetch = FetchType.LAZY)
    And the join column must be "booking_id" referencing "bookings.id"
    And it must be annotated with @JsonIgnore to prevent circular serialization
    And @ToString.Exclude must be added to avoid Lombok circular toString

  # ---------------------------------------------------------------------------
  # Booking Reference Generation
  # ---------------------------------------------------------------------------

  @domain @logic
  Scenario: Booking reference format and generation
    Given a new Booking is being created
    Then the bookingReference must follow the pattern "BKG-{YEAR}-{SEQUENCE}"
    And {YEAR} is the 4-digit current year in UTC
    And {SEQUENCE} is a zero-padded 5-digit auto-incrementing number
    And the sequence must be unique per year
    And examples of valid references are:
      | reference       |
      | BKG-2026-00001  |
      | BKG-2026-00042  |
      | BKG-2027-00001  |
    And the generation must be handled by a dedicated service or utility class
    And it must be safe for concurrent usage (no duplicate references under load)

  @domain @logic
  Scenario: Booking reference database sequence
    Given the booking reference requires a sequential number
    Then a PostgreSQL sequence named "booking_reference_seq" must be created
    And it must start at 1 and increment by 1
    And it must be reset or partitioned per year via application logic
    And the Flyway migration must create this sequence alongside the tables

  # ---------------------------------------------------------------------------
  # Entity Validation Rules
  # ---------------------------------------------------------------------------

  @domain @validation
  Scenario: Booking entity field validations
    Given the entity "Booking"
    Then the following Jakarta Validation annotations must be applied:
      | field            | annotation                  | parameters              |
      | bookingReference | @NotBlank                   |                         |
      | status           | @NotNull                    |                         |
      | scheduleId       | @NotNull                    |                         |
      | quoteId          | @NotNull                    |                         |
      | customerId       | @NotNull                    |                         |
      | customerName     | @NotBlank                   |                         |
      | customerName     | @Size                       | max = 255               |
      | customerEmail    | @NotBlank                   |                         |
      | customerEmail    | @Email                      |                         |
      | customerEmail    | @Size                       | max = 255               |
      | customerPhone    | @Size                       | max = 50                |
      | cargoDescription | @NotBlank                   |                         |
      | cargoDescription | @Size                       | max = 1000              |
      | cargoWeightKg    | @NotNull                    |                         |
      | cargoWeightKg    | @DecimalMin                 | value = "0.01"          |

  @domain @validation
  Scenario: BookingEquipmentLine entity field validations
    Given the entity "BookingEquipmentLine"
    Then the following Jakarta Validation annotations must be applied:
      | field    | annotation | parameters     |
      | type     | @NotNull   |                |
      | quantity | @NotNull   |                |
      | quantity | @Min       | value = 1      |

  # ---------------------------------------------------------------------------
  # Lifecycle State Transition Rules
  # ---------------------------------------------------------------------------

  @domain @lifecycle
  Scenario: Valid booking state transitions
    Given the BookingStatus enum
    Then only the following state transitions are allowed:
      | from        | to          |
      | PENDING     | CONFIRMED   |
      | PENDING     | CANCELLED   |
      | CONFIRMED   | IN_PROGRESS |
      | CONFIRMED   | CANCELLED   |
      | IN_PROGRESS | COMPLETED   |
    And any transition not in this list must throw an IllegalStateTransitionException
    And the transition validation logic must reside in the Booking entity or a dedicated domain service

  @domain @lifecycle
  Scenario Outline: State transition examples
    Given a Booking with status "<currentStatus>"
    When the system attempts to transition to "<targetStatus>"
    Then the transition must be "<result>"

    Examples:
      | currentStatus | targetStatus | result   |
      | PENDING       | CONFIRMED    | allowed  |
      | PENDING       | CANCELLED    | allowed  |
      | PENDING       | COMPLETED    | rejected |
      | PENDING       | IN_PROGRESS  | rejected |
      | CONFIRMED     | IN_PROGRESS  | allowed  |
      | CONFIRMED     | CANCELLED    | allowed  |
      | CONFIRMED     | PENDING      | rejected |
      | IN_PROGRESS   | COMPLETED    | allowed  |
      | IN_PROGRESS   | CANCELLED    | rejected |
      | COMPLETED     | CANCELLED    | rejected |
      | COMPLETED     | PENDING      | rejected |
      | CANCELLED     | PENDING      | rejected |
      | CANCELLED     | CONFIRMED    | rejected |

  # ---------------------------------------------------------------------------
  # Flyway Migration Scripts
  # ---------------------------------------------------------------------------

  @domain @migration
  Scenario: Initial Flyway migration script
    Given the Flyway migrations directory is "src/main/resources/db/migration"
    Then a migration file "V1__create_booking_tables.sql" must be created
    And it must create the "bookings" table with all columns matching the Booking entity
    And it must create the "booking_equipment_lines" table with a foreign key to "bookings"
    And it must create the "booking_reference_seq" sequence
    And it must create all indexes defined in the indexing scenario
    And all column types must match PostgreSQL equivalents:
      | javaType      | postgresType              |
      | Long          | BIGINT                    |
      | String        | VARCHAR(n)                |
      | BookingStatus | VARCHAR(20)               |
      | EquipmentType | VARCHAR(20)               |
      | BigDecimal    | NUMERIC(10,2)             |
      | Integer       | INTEGER                   |
      | Instant       | TIMESTAMP WITH TIME ZONE  |

  # ---------------------------------------------------------------------------
  # Equals, HashCode, and ToString
  # ---------------------------------------------------------------------------

  @domain @conventions
  Scenario: Entity identity and equality
    Given the entities "Booking" and "BookingEquipmentLine"
    Then @EqualsAndHashCode must use only the "id" field for both entities
    And this must be configured via @EqualsAndHashCode(onlyExplicitlyIncluded = true) and @EqualsAndHashCode.Include on the id field
    And @ToString on BookingEquipmentLine must exclude the "booking" field to prevent circular references

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @domain @out-of-scope
  Scenario: Items NOT covered in domain model
    Given this is the domain model file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                                  | deferred to            |
      | Repository interfaces and queries      | 003_data_access.md    |
      | State transition orchestration logic   | 004_business_rules.md |
      | Request/Response DTOs                  | 005_api_endpoints.md  |
      | Mapping between entities and DTOs      | 005_api_endpoints.md  |
      | Domain event payloads                  | 004_business_rules.md |
