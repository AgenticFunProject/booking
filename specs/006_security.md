# Security

## Metadata

| Field | Value |
| --- | --- |
| File | 006_security.md |
| Depends on | 001_project_setup.md, 002_domain_model.md, 005_api_endpoints.md |
| Produces | Security configuration, JWT filter, authentication entry point, role-based access, CORS configuration, security-related DTOs |
| Context | Defines optional authentication and authorization for the Cargo Booking Service. When security is enabled, it validates JWTs issued by an external identity provider. When security is disabled, the service must still work using request data such as customerId. The AI agent must have processed 001–005 so that endpoints and conventions are known. |

## Goal

- As an AI code generator
- I need to configure authentication and authorization for the Booking Service
- So that only authenticated and authorized clients can access protected endpoints

## Background

- Given the base package is "com.cargo.booking"
- And security classes reside in "com.cargo.booking.security"
- And all endpoints from 005_api_endpoints.md are known
- And the API prefix is "/api/v1" as defined in 001_project_setup.md

## Security Dependency

### Add Spring Security dependency

Tags: `security`, `setup`

- Given the pom.xml dependency section
- Then it must include the following additional starter (not listed in 001):

| groupId                  | artifactId                        | purpose                    |
| --- | --- | --- |
| org.springframework.boot | spring-boot-starter-security      | Authentication & authorization |
| io.jsonwebtoken          | jjwt-api                          | JWT parsing and validation |
| io.jsonwebtoken          | jjwt-impl                         | JWT runtime implementation |
| io.jsonwebtoken          | jjwt-jackson                      | JWT JSON serialization     |

- And the jjwt version must be managed via a property (e.g. 0.12.6 or latest stable)

## JWT Configuration Properties

### JWT configuration properties

Tags: `security`, `config`

- Given the file "src/main/resources/application.yml"
- Then it must include the following security-related properties:

| property                          | value                                        | description                      |
| --- | --- | --- |
| app.security.enabled              | ${SECURITY_ENABLED:true}                     | Enables JWT auth and authorization |
| app.security.jwt.secret           | ${JWT_SECRET:default-dev-secret-key-min-256-bits-long-for-hs256} | HMAC signing key     |
| app.security.jwt.issuer           | ${JWT_ISSUER:cargo-platform}                 | Expected token issuer            |
| app.security.jwt.expiration-ms    | ${JWT_EXPIRATION:3600000}                    | Token validity (1 hour default)  |

### Security can be disabled for local or unsecured deployments

Tags: `security`, `config`

- Given app.security.enabled is false
- Then the SecurityFilterChain must permit all API requests
- And JwtAuthenticationFilter must either not be registered or must short-circuit without validating tokens
- And customerId must come from request data or query parameters
- And no ownership checks based on JWT subject must run
- And this mode is intended for local development, demos, or deployments where authentication is handled outside this service

### JwtProperties configuration class

Tags: `security`, `config`

- Given a class "JwtProperties" in package "com.cargo.booking.security"
- Then it must be annotated with @ConfigurationProperties(prefix = "app.security.jwt")
- And it must be a Java record or @Data class with fields:

| field        | type   | description                   |
| --- | --- | --- |
| secret       | String | The HMAC signing key          |
| issuer       | String | Expected issuer claim         |
| expirationMs | long   | Token expiration in milliseconds |

- And SecurityConfig must enable JwtProperties with @EnableConfigurationProperties so MVC slice tests importing SecurityConfig also receive this bean

### SecurityProperties configuration class

Tags: `security`, `config`

- Given a class "SecurityProperties" in package "com.cargo.booking.security"
- Then it must be annotated with @ConfigurationProperties(prefix = "app.security")
- And it must expose:

| field   | type    | description                              |
| --- | --- | --- |
| enabled | boolean | Enables JWT authentication/authorization |

- And SecurityConfig must depend on SecurityProperties to decide whether to permit all API requests
- And SecurityConfig must enable SecurityProperties with @EnableConfigurationProperties so MVC slice tests importing SecurityConfig also receive this bean

## Roles

### Application roles

Tags: `security`, `roles`

- Given the Booking Service authorization model
- Then the following roles must be recognized:

| role          | description                                                        |
| --- | --- |
| ROLE_CUSTOMER | Direct customer caller; can create, view, and cancel own bookings when token contains a customerId claim |
| ROLE_SERVICE  | Trusted service-to-service caller; can create, read, and cancel bookings on behalf of customers |
| ROLE_OPERATOR | Can view all bookings, confirm, start, and complete bookings        |
| ROLE_ADMIN    | Full access to all operations including all operator permissions    |

- And roles are extracted from the JWT claims (field: "roles" — an array of strings)
- And the role prefix "ROLE_" must be prepended if not already present in the token
- And the JWT subject identifies the authenticated requester (user or service), not necessarily the booking customer
- And the optional JWT customerId claim may be named "customerId" or "customer_id" when the token represents a direct customer

## JWT Token Service

### JwtTokenProvider service

Tags: `security`, `jwt`

- Given a class "JwtTokenProvider" in package "com.cargo.booking.security"
- Then it must be annotated with @Component
- And it must depend on JwtProperties
- And it must provide the following methods:

| method                                          | returns                  | description                                      |
| --- | --- | --- |
| validateToken(String token)                     | boolean                  | Validates signature, expiration, and issuer       |
| getSubjectFromToken(String token)               | String                   | Extracts the subject claim for the authenticated requester |
| getCustomerIdFromToken(String token)            | Optional<Long>           | Extracts optional customerId/customer_id claim when present |
| getUsernameFromToken(String token)              | String                   | Extracts the "username" or "name" claim           |
| getRolesFromToken(String token)                 | List<String>             | Extracts the "roles" claim as a list of strings   |
| getAuthentication(String token)                 | Authentication           | Builds a UsernamePasswordAuthenticationToken      |

- And validateToken must catch and handle:

| exception                    | meaning                       | action         |
| --- | --- | --- |
| ExpiredJwtException          | Token has expired             | return false   |
| MalformedJwtException        | Token is malformed            | return false   |
| UnsupportedJwtException      | Token format not supported    | return false   |
| SecurityException            | Invalid signature             | return false   |
| IllegalArgumentException     | Token is blank or null        | return false   |

- And all validation failures must be logged at WARN level (without logging the token itself)

## JWT Authentication Filter

### JwtAuthenticationFilter

Tags: `security`, `filter`

- Given a class "JwtAuthenticationFilter" in package "com.cargo.booking.security"
- Then it must extend OncePerRequestFilter
- And it must depend on JwtTokenProvider
- And the doFilterInternal method must:

| step | action                                                                           |
| --- | --- |
| 1    | Extract the Authorization header from the request                                |
| 2    | If the header is missing or does not start with "Bearer ", continue the chain    |
| 3    | Extract the token (strip "Bearer " prefix)                                       |
| 4    | Call jwtTokenProvider.validateToken(token)                                       |
| 5    | If valid, call jwtTokenProvider.getAuthentication(token)                         |
| 6    | Set the Authentication in SecurityContextHolder                                  |
| 7    | Continue the filter chain                                                        |

- And the filter must NOT throw exceptions for invalid tokens — it simply does not authenticate

## Authentication Entry Point

### Custom authentication entry point

Tags: `security`, `entrypoint`

- Given a class "JwtAuthenticationEntryPoint" in package "com.cargo.booking.security"
- Then it must implement AuthenticationEntryPoint
- And it must be annotated with @Component
- And when an unauthenticated request reaches a secured endpoint it must:

| step | action                                                                    |
| --- | --- |
| 1    | Set response status to 401 Unauthorized                                   |
| 2    | Set content type to application/json                                      |
| 3    | Write an error response body following the error structure from 001:       |

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required to access this resource",
  "path": "/api/v1/bookings"
}
```

## Access Denied Handler

### Custom access denied handler

Tags: `security`, `access-denied`

- Given a class "JwtAccessDeniedHandler" in package "com.cargo.booking.security"
- Then it must implement AccessDeniedHandler
- And it must be annotated with @Component
- And when an authenticated user lacks the required role it must:

| step | action                                                                    |
| --- | --- |
| 1    | Set response status to 403 Forbidden                                      |
| 2    | Set content type to application/json                                      |
| 3    | Write an error response body following the error structure from 001:       |

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to perform this action",
  "path": "/api/v1/bookings/123/confirm"
}
```

## Security Filter Chain Configuration

### SecurityConfig class

Tags: `security`, `config`

- Given a class "SecurityConfig" in package "com.cargo.booking.config"
- Then it must be annotated with @Configuration and @EnableWebSecurity and @EnableMethodSecurity
- And it must depend on:

| dependency                   | purpose                               |
| --- | --- |
| SecurityProperties           | Controls whether security is enabled  |
| JwtAuthenticationFilter      | JWT validation filter                 |
| JwtAuthenticationEntryPoint  | 401 error handling                    |
| JwtAccessDeniedHandler       | 403 error handling                    |

- And it must define a SecurityFilterChain bean with the following rules:

| rule                                                                                  |
| --- |
| Disable CSRF (stateless API)                                                          |
| Set session management to STATELESS                                                   |
| Register JwtAuthenticationEntryPoint as the exception handling entry point             |
| Register JwtAccessDeniedHandler as the access denied handler                           |
| Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter                |
| If securityProperties.enabled is false, configure permitAll and skip JWT validation     |

### Endpoint-level access rules

Tags: `security`, `config`

- Given the SecurityFilterChain bean
- Then the following endpoint access rules must be configured:

| pattern                              | method | access                                    |
| --- | --- | --- |
| /api/v1/bookings                     | POST   | hasAnyRole('CUSTOMER', 'SERVICE', 'ADMIN') |
| /api/v1/bookings                     | GET    | hasAnyRole('CUSTOMER', 'SERVICE', 'OPERATOR', 'ADMIN') |
| /api/v1/bookings/{id}                | GET    | hasAnyRole('CUSTOMER', 'SERVICE', 'OPERATOR', 'ADMIN') |
| /api/v1/bookings/{id}/cancel         | PATCH  | hasAnyRole('CUSTOMER', 'SERVICE', 'ADMIN') |
| /api/v1/bookings/{id}/confirm        | PATCH  | hasAnyRole('OPERATOR', 'ADMIN')            |
| /api/v1/bookings/{id}/start          | PATCH  | hasAnyRole('OPERATOR', 'ADMIN')            |
| /api/v1/bookings/{id}/complete       | PATCH  | hasAnyRole('OPERATOR', 'ADMIN')            |
| /swagger-ui/**                       | GET    | permitAll                                  |
| /api-docs/**                         | GET    | permitAll                                  |
| /actuator/health                     | GET    | permitAll                                  |
| /actuator/info                       | GET    | permitAll                                  |
| /actuator/metrics                    | GET    | hasRole('ADMIN')                            |
| any other request                    | *      | authenticated                              |

## Data-Level Authorization (Ownership Rules)

### Customers can only access their own bookings

Tags: `security`, `ownership`

- Given security is enabled
- And a request from a user with role ROLE_CUSTOMER
- And the JWT contains a customerId or customer_id claim
- When the user calls GET /api/v1/bookings
- Then the customerId query parameter must match the customerId claim from the JWT
- And if it does not match, the API/security layer must return HTTP 403 Forbidden before calling BookingService
- And when the user calls POST /api/v1/bookings, request.customerId must match the customerId claim from the JWT
- And if it does not match, the API/security layer must return HTTP 403 Forbidden before calling BookingService
- And the Booking entity must store customerId from the request after this authorization check passes
- And if the user calls GET /api/v1/bookings without customerId, the API/security layer must return HTTP 400 Bad Request before calling BookingService

### Customer role without a customer identity claim

Tags: `security`, `ownership`

- Given security is enabled
- And a request from a user with role ROLE_CUSTOMER
- And the JWT does not contain a customerId or customer_id claim
- When the endpoint requires customer ownership validation
- Then the API/security layer must return HTTP 403 Forbidden before calling BookingService
- And it must not infer customerId from the JWT subject

### Customers can only view and cancel their own bookings

Tags: `security`, `ownership`

- Given security is enabled
- And a request from a user with role ROLE_CUSTOMER
- And the JWT contains a customerId or customer_id claim
- When the user calls GET /api/v1/bookings/{id} or PATCH /api/v1/bookings/{id}/cancel
- Then the API/security layer must verify that the booking's customerId matches the customerId claim from the JWT
- And if it does not match, the API/security layer must return HTTP 403 Forbidden

### BookingAccessAuthorizer component

Tags: `security`, `ownership`

- Given a component "BookingAccessAuthorizer" in package "com.cargo.booking.security"
- Then it must be annotated with @Component
- And it must depend on BookingRepository and use SecurityContextHelper
- And it must provide methods for controller ownership checks:

| method                                           | purpose                                      |
| --- | --- |
| void authorizeCreateCustomer(Long customerId)    | Verify create request customer ownership     |
| void authorizeListCustomer(Long customerId)      | Verify list query customer ownership         |
| void authorizeBookingAccess(Long bookingId)      | Verify access to a booking identified by ID  |
| void authorizeBookingAccess(String reference)    | Verify access to a booking identified by reference |

- And when security is disabled it must allow access without ownership checks
- And when the caller has ROLE_SERVICE, ROLE_OPERATOR, or ROLE_ADMIN it must allow access without customer ownership checks
- And when the caller has ROLE_CUSTOMER and the checked customerId comes from a request body or query parameter it must compare that customerId with the JWT customerId/customer_id claim
- And when the caller has ROLE_CUSTOMER and the checked customerId comes from an existing booking it must load the booking owner and compare it with the JWT customerId/customer_id claim
- And if the customer identity claim is missing it must throw AccessDeniedException before checking request/query customerId values
- And authorizeListCustomer must throw BookingValidationException when ROLE_CUSTOMER has a customer identity claim but omits the customerId query parameter while security is enabled
- And if the repository lookup returns empty it must return without throwing so the subsequent BookingService call owns the BookingNotFoundException and final 404 response
- And if the customer identity claim does not match the checked customerId it must throw AccessDeniedException before BookingService is called

### Service callers, operators, and admins can act for requested customers

Tags: `security`, `ownership`

- Given security is enabled
- And a request from a caller with role ROLE_SERVICE, ROLE_OPERATOR, or ROLE_ADMIN
- When the user accesses a booking endpoint allowed by the endpoint-level access rules
- Then no customer ownership check is required
- And they may act on behalf of the customer identified by request.customerId or query parameter customerId
- And for GET /api/v1/bookings they may omit customerId to list all bookings

### SecurityContextHelper utility

Tags: `security`, `ownership`

- Given a utility class "SecurityContextHelper" in package "com.cargo.booking.security"
- Then it must provide the following static methods:

| method                          | returns      | description                                     |
| --- | --- | --- |
| getCurrentSubject()             | String       | Extracts the requester subject from SecurityContext |
| getCurrentCustomerId()          | Optional<Long> | Extracts optional customerId/customer_id claim |
| getCurrentUsername()            | String       | Extracts the username from SecurityContext       |
| getCurrentRoles()              | List<String> | Extracts the roles from SecurityContext          |
| hasRole(String role)           | boolean      | Checks if the current user has a specific role   |
| isOwnerOrPrivileged(Long ownerId) | boolean   | Returns true if token customerId matches ownerId or caller has SERVICE/OPERATOR/ADMIN role |

## CORS Configuration

### CORS configuration

Tags: `security`, `cors`

- Given the SecurityConfig class
- Then it must define a CorsConfigurationSource bean with the following settings:

| setting          | value                                                  |
| --- | --- |
| allowed origins  | ${CORS_ALLOWED_ORIGINS:http://localhost:3000}           |
| allowed methods  | GET, POST, PATCH, OPTIONS                              |
| allowed headers  | Authorization, Content-Type, Accept                    |
| exposed headers  | Authorization                                          |
| allow credentials| true                                                   |
| max age          | 3600 seconds                                           |

- And the allowed origins must be externalized via application.yml property

## Security for Test Profile

### Test security configuration

Tags: `security`, `test`

- Given the test profile "test"
- Then a test configuration class "TestSecurityConfig" must exist in the test source set
- And it must provide an option to disable JWT validation for integration tests
- And it must provide a utility method or builder to create mock Authentication objects with:

| field      | description                                      |
| --- | --- |
| subject    | The authenticated requester subject              |
| customerId | Optional customer ID claim for direct customer tokens |
| username   | The username/name claim                          |
| roles      | List of roles to assign                         |

- And tests must be able to use @WithMockUser or a custom annotation @WithMockJwt for convenience

## Security Headers

### Security response headers

Tags: `security`, `headers`

- Given the SecurityFilterChain
- Then the following security headers must be configured:

| header                    | value / behavior                                 |
| --- | --- |
| X-Content-Type-Options    | nosniff                                          |
| X-Frame-Options           | DENY                                             |
| Cache-Control             | no-cache, no-store, max-age=0 on secured endpoints |
| Strict-Transport-Security | max-age=31536000; includeSubDomains (when HTTPS)  |

- And these are mostly Spring Security defaults — ensure they are not disabled

## Rate Limiting (Basic)

### Basic rate limiting awareness

Tags: `security`, `rate-limiting`

- Given the Booking Service API
- Then rate limiting is NOT implemented at the application level in v1
- And the assumption is that the API gateway handles rate limiting
- And a comment must be placed in SecurityConfig noting this:

```
// Rate limiting is delegated to the API gateway.
// If standalone rate limiting is needed, consider Bucket4j or Resilience4j RateLimiter.
```

## Out of Scope for this file

### Items NOT covered in security

Tags: `security`, `out-of-scope`

- Given this is the security file only
- Then the following are NOT defined here and will be addressed in later files:

| topic                                          | deferred to           |
| --- | --- |
| Error response mapping for security exceptions | 007_error_handling.md |
| User management / registration                 | Out of scope for v1   |
| OAuth2 / OIDC integration with identity provider| Out of scope for v1  |
| Security-related integration tests             | 009_testing.md        |
