# File: 006_security.md
# Depends on: 001_project_setup.md, 002_domain_model.md, 005_api_endpoints.md
# Produces: Security configuration, JWT filter, authentication entry point, role-based access,
#           CORS configuration, security-related DTOs
# Context: Defines authentication and authorization for the Cargo Booking Service. Since this
#          is a microservice behind an API gateway, it validates JWTs issued by an external
#          identity provider rather than managing users directly. The AI agent must have
#          processed 001–005 so that endpoints and conventions are known.

Feature: Security
  As an AI code generator
  I need to configure authentication and authorization for the Booking Service
  So that only authenticated and authorized clients can access protected endpoints

  Background:
    Given the base package is "com.cargo.booking"
    And security classes reside in "com.cargo.booking.security"
    And all endpoints from 005_api_endpoints.md are known
    And the API prefix is "/api/v1" as defined in 001_project_setup.md

  # ---------------------------------------------------------------------------
  # Security Dependency
  # ---------------------------------------------------------------------------

  @security @setup
  Scenario: Add Spring Security dependency
    Given the pom.xml dependency section
    Then it must include the following additional starter (not listed in 001):
      | groupId                  | artifactId                        | purpose                    |
      | org.springframework.boot | spring-boot-starter-security      | Authentication & authorization |
      | io.jsonwebtoken          | jjwt-api                          | JWT parsing and validation |
      | io.jsonwebtoken          | jjwt-impl                         | JWT runtime implementation |
      | io.jsonwebtoken          | jjwt-jackson                      | JWT JSON serialization     |
    And the jjwt version must be managed via a property (e.g. 0.12.6 or latest stable)

  # ---------------------------------------------------------------------------
  # JWT Configuration Properties
  # ---------------------------------------------------------------------------

  @security @config
  Scenario: JWT configuration properties
    Given the file "src/main/resources/application.yml"
    Then it must include the following security-related properties:
      | property                          | value                                        | description                      |
      | app.security.jwt.secret           | ${JWT_SECRET:default-dev-secret-key-min-256-bits-long-for-hs256} | HMAC signing key     |
      | app.security.jwt.issuer           | ${JWT_ISSUER:cargo-platform}                 | Expected token issuer            |
      | app.security.jwt.expiration-ms    | ${JWT_EXPIRATION:3600000}                    | Token validity (1 hour default)  |

  @security @config
  Scenario: JwtProperties configuration class
    Given a class "JwtProperties" in package "com.cargo.booking.security"
    Then it must be annotated with @ConfigurationProperties(prefix = "app.security.jwt")
    And it must be a Java record or @Data class with fields:
      | field        | type   | description                   |
      | secret       | String | The HMAC signing key          |
      | issuer       | String | Expected issuer claim         |
      | expirationMs | long   | Token expiration in milliseconds |
    And @EnableConfigurationProperties(JwtProperties.class) must be added to the security config or main app class

  # ---------------------------------------------------------------------------
  # Roles
  # ---------------------------------------------------------------------------

  @security @roles
  Scenario: Application roles
    Given the Booking Service authorization model
    Then the following roles must be recognized:
      | role          | description                                                        |
      | ROLE_CUSTOMER | Can create bookings, view own bookings, cancel own bookings         |
      | ROLE_OPERATOR | Can view all bookings, confirm, start, and complete bookings        |
      | ROLE_ADMIN    | Full access to all operations including all operator permissions    |
    And roles are extracted from the JWT claims (field: "roles" — an array of strings)
    And the role prefix "ROLE_" must be prepended if not already present in the token

  # ---------------------------------------------------------------------------
  # JWT Token Service
  # ---------------------------------------------------------------------------

  @security @jwt
  Scenario: JwtTokenProvider service
    Given a class "JwtTokenProvider" in package "com.cargo.booking.security"
    Then it must be annotated with @Component
    And it must depend on JwtProperties
    And it must provide the following methods:
      | method                                          | returns                  | description                                      |
      | validateToken(String token)                     | boolean                  | Validates signature, expiration, and issuer       |
      | getUserIdFromToken(String token)                | Long                     | Extracts the subject claim as a Long              |
      | getUsernameFromToken(String token)              | String                   | Extracts the "username" or "name" claim           |
      | getRolesFromToken(String token)                 | List<String>             | Extracts the "roles" claim as a list of strings   |
      | getAuthentication(String token)                 | Authentication           | Builds a UsernamePasswordAuthenticationToken      |
    And validateToken must catch and handle:
      | exception                    | meaning                       | action         |
      | ExpiredJwtException          | Token has expired             | return false   |
      | MalformedJwtException        | Token is malformed            | return false   |
      | UnsupportedJwtException      | Token format not supported    | return false   |
      | SecurityException            | Invalid signature             | return false   |
      | IllegalArgumentException     | Token is blank or null        | return false   |
    And all validation failures must be logged at WARN level (without logging the token itself)

  # ---------------------------------------------------------------------------
  # JWT Authentication Filter
  # ---------------------------------------------------------------------------

  @security @filter
  Scenario: JwtAuthenticationFilter
    Given a class "JwtAuthenticationFilter" in package "com.cargo.booking.security"
    Then it must extend OncePerRequestFilter
    And it must depend on JwtTokenProvider
    And the doFilterInternal method must:
      | step | action                                                                           |
      | 1    | Extract the Authorization header from the request                                |
      | 2    | If the header is missing or does not start with "Bearer ", continue the chain    |
      | 3    | Extract the token (strip "Bearer " prefix)                                       |
      | 4    | Call jwtTokenProvider.validateToken(token)                                       |
      | 5    | If valid, call jwtTokenProvider.getAuthentication(token)                         |
      | 6    | Set the Authentication in SecurityContextHolder                                  |
      | 7    | Continue the filter chain                                                        |
    And the filter must NOT throw exceptions for invalid tokens — it simply does not authenticate

  # ---------------------------------------------------------------------------
  # Authentication Entry Point
  # ---------------------------------------------------------------------------

  @security @entrypoint
  Scenario: Custom authentication entry point
    Given a class "JwtAuthenticationEntryPoint" in package "com.cargo.booking.security"
    Then it must implement AuthenticationEntryPoint
    And it must be annotated with @Component
    And when an unauthenticated request reaches a secured endpoint it must:
      | step | action                                                                    |
      | 1    | Set response status to 401 Unauthorized                                   |
      | 2    | Set content type to application/json                                      |
      | 3    | Write an error response body following the error structure from 001:       |
      """json
      {
        "timestamp": "2026-04-01T10:00:00Z",
        "status": 401,
        "error": "Unauthorized",
        "message": "Authentication is required to access this resource",
        "path": "/api/v1/bookings"
      }
      """

  # ---------------------------------------------------------------------------
  # Access Denied Handler
  # ---------------------------------------------------------------------------

  @security @access-denied
  Scenario: Custom access denied handler
    Given a class "JwtAccessDeniedHandler" in package "com.cargo.booking.security"
    Then it must implement AccessDeniedHandler
    And it must be annotated with @Component
    And when an authenticated user lacks the required role it must:
      | step | action                                                                    |
      | 1    | Set response status to 403 Forbidden                                      |
      | 2    | Set content type to application/json                                      |
      | 3    | Write an error response body following the error structure from 001:       |
      """json
      {
        "timestamp": "2026-04-01T10:00:00Z",
        "status": 403,
        "error": "Forbidden",
        "message": "You do not have permission to perform this action",
        "path": "/api/v1/bookings/123/confirm"
      }
      """

  # ---------------------------------------------------------------------------
  # Security Filter Chain Configuration
  # ---------------------------------------------------------------------------

  @security @config
  Scenario: SecurityConfig class
    Given a class "SecurityConfig" in package "com.cargo.booking.config"
    Then it must be annotated with @Configuration and @EnableWebSecurity and @EnableMethodSecurity
    And it must depend on:
      | dependency                   | purpose                               |
      | JwtAuthenticationFilter      | JWT validation filter                 |
      | JwtAuthenticationEntryPoint  | 401 error handling                    |
      | JwtAccessDeniedHandler       | 403 error handling                    |
    And it must define a SecurityFilterChain bean with the following rules:
      | rule                                                                                  |
      | Disable CSRF (stateless API)                                                          |
      | Set session management to STATELESS                                                   |
      | Register JwtAuthenticationEntryPoint as the exception handling entry point             |
      | Register JwtAccessDeniedHandler as the access denied handler                           |
      | Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter                |

  @security @config
  Scenario: Endpoint-level access rules
    Given the SecurityFilterChain bean
    Then the following endpoint access rules must be configured:
      | pattern                              | method | access                                    |
      | /api/v1/bookings                     | POST   | hasAnyRole('CUSTOMER', 'ADMIN')            |
      | /api/v1/bookings                     | GET    | hasAnyRole('CUSTOMER', 'OPERATOR', 'ADMIN')|
      | /api/v1/bookings/{id}                | GET    | hasAnyRole('CUSTOMER', 'OPERATOR', 'ADMIN')|
      | /api/v1/bookings/{id}/cancel         | PATCH  | hasAnyRole('CUSTOMER', 'ADMIN')            |
      | /api/v1/bookings/{id}/confirm        | PATCH  | hasAnyRole('OPERATOR', 'ADMIN')            |
      | /api/v1/bookings/{id}/start          | PATCH  | hasAnyRole('OPERATOR', 'ADMIN')            |
      | /api/v1/bookings/{id}/complete       | PATCH  | hasAnyRole('OPERATOR', 'ADMIN')            |
      | /swagger-ui/**                       | GET    | permitAll                                  |
      | /api-docs/**                         | GET    | permitAll                                  |
      | /actuator/health                     | GET    | permitAll                                  |
      | /actuator/info                       | GET    | permitAll                                  |
      | /actuator/metrics                    | GET    | hasRole('ADMIN')                            |
      | any other request                    | *      | authenticated                              |

  # ---------------------------------------------------------------------------
  # Data-Level Authorization (Ownership Rules)
  # ---------------------------------------------------------------------------

  @security @ownership
  Scenario: Customers can only access their own bookings
    Given a request from a user with role ROLE_CUSTOMER
    When the user calls GET /api/v1/bookings
    Then the customerId query parameter must match the authenticated user's ID from the JWT
    And if it does not match, the service must return HTTP 403 Forbidden
    And customer-created bookings must store customerId from the JWT subject, never from request body input

  @security @ownership
  Scenario: Customers can only view and cancel their own bookings
    Given a request from a user with role ROLE_CUSTOMER
    When the user calls GET /api/v1/bookings/{id} or PATCH /api/v1/bookings/{id}/cancel
    Then the service must verify that the booking's customerId matches the authenticated user's ID
    And if it does not match, the service must return HTTP 403 Forbidden

  @security @ownership
  Scenario: Operators and admins can access all bookings
    Given a request from a user with role ROLE_OPERATOR or ROLE_ADMIN
    When the user accesses any booking endpoint
    Then no ownership check is required — they may access and manage any booking

  @security @ownership
  Scenario: SecurityContextHelper utility
    Given a utility class "SecurityContextHelper" in package "com.cargo.booking.security"
    Then it must provide the following static methods:
      | method                          | returns      | description                                     |
      | getCurrentUserId()              | Long         | Extracts the user ID from SecurityContext        |
      | getCurrentUsername()            | String       | Extracts the username from SecurityContext       |
      | getCurrentRoles()              | List<String> | Extracts the roles from SecurityContext          |
      | hasRole(String role)           | boolean      | Checks if the current user has a specific role   |
      | isOwnerOrPrivileged(Long ownerId) | boolean   | Returns true if user is the owner or has OPERATOR/ADMIN role |

  # ---------------------------------------------------------------------------
  # CORS Configuration
  # ---------------------------------------------------------------------------

  @security @cors
  Scenario: CORS configuration
    Given the SecurityConfig class
    Then it must define a CorsConfigurationSource bean with the following settings:
      | setting          | value                                                  |
      | allowed origins  | ${CORS_ALLOWED_ORIGINS:http://localhost:3000}           |
      | allowed methods  | GET, POST, PATCH, DELETE, OPTIONS                      |
      | allowed headers  | Authorization, Content-Type, Accept                    |
      | exposed headers  | Authorization                                          |
      | allow credentials| true                                                   |
      | max age          | 3600 seconds                                           |
    And the allowed origins must be externalized via application.yml property

  # ---------------------------------------------------------------------------
  # Security for Test Profile
  # ---------------------------------------------------------------------------

  @security @test
  Scenario: Test security configuration
    Given the test profile "test"
    Then a test configuration class "TestSecurityConfig" must exist in the test source set
    And it must provide an option to disable JWT validation for integration tests
    And it must provide a utility method or builder to create mock Authentication objects with:
      | field    | description                                |
      | userId   | The Long to use as the authenticated user  |
      | username | The username claim                         |
      | roles    | List of roles to assign                    |
    And tests must be able to use @WithMockUser or a custom annotation @WithMockJwt for convenience

  # ---------------------------------------------------------------------------
  # Security Headers
  # ---------------------------------------------------------------------------

  @security @headers
  Scenario: Security response headers
    Given the SecurityFilterChain
    Then the following security headers must be configured:
      | header                    | value / behavior                                 |
      | X-Content-Type-Options    | nosniff                                          |
      | X-Frame-Options           | DENY                                             |
      | Cache-Control             | no-cache, no-store, max-age=0 on secured endpoints |
      | Strict-Transport-Security | max-age=31536000; includeSubDomains (when HTTPS)  |
    And these are mostly Spring Security defaults — ensure they are not disabled

  # ---------------------------------------------------------------------------
  # Rate Limiting (Basic)
  # ---------------------------------------------------------------------------

  @security @rate-limiting
  Scenario: Basic rate limiting awareness
    Given the Booking Service API
    Then rate limiting is NOT implemented at the application level in v1
    And the assumption is that the API gateway handles rate limiting
    And a comment must be placed in SecurityConfig noting this:
      """
      // Rate limiting is delegated to the API gateway.
      // If standalone rate limiting is needed, consider Bucket4j or Resilience4j RateLimiter.
      """

  # ---------------------------------------------------------------------------
  # Out of Scope for this file
  # ---------------------------------------------------------------------------

  @security @out-of-scope
  Scenario: Items NOT covered in security
    Given this is the security file only
    Then the following are NOT defined here and will be addressed in later files:
      | topic                                          | deferred to           |
      | Error response mapping for security exceptions | 007_error_handling.md |
      | User management / registration                 | Out of scope for v1   |
      | OAuth2 / OIDC integration with identity provider| Out of scope for v1  |
      | Security-related integration tests             | 009_testing.md        |
