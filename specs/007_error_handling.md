# Error Handling

## Metadata

| Field | Value |
| --- | --- |
| File | 007_error_handling.md |
| Depends on | 001_project_setup.md, 004_business_rules.md, 005_api_endpoints.md, 006_security.md |
| Produces | Global exception handler, error response DTOs, exception-to-HTTP-status mapping, validation error formatting, logging configuration for errors |
| Context | Defines how the Cargo Booking Service handles and formats all error responses. The AI agent must have processed 001–006 so that all exceptions, endpoints, and security error handling are known. |

## Goal

- As an AI code generator
- I need to implement a consistent global error handling strategy for the Booking Service
- So that all API errors return predictable, well-structured responses

## Background

- Given the base package is "com.cargo.booking"
- And exception handling classes reside in "com.cargo.booking.exception"
- And the error response structure was defined in 001_project_setup.md
- And all custom exceptions are defined in 004_business_rules.md
- And security error handling (401, 403) is defined in 006_security.md

## Error Response DTO

### ErrorResponse DTO

Tags: `error`, `dto`

- Given a record "ErrorResponse" in package "com.cargo.booking.exception"
- Then it must be a Java record with the following fields:

| field      | type          | description                                    |
| --- | --- | --- |
| timestamp  | Instant       | When the error occurred (UTC, ISO-8601)        |
| status     | int           | HTTP status code                               |
| error      | String        | HTTP reason phrase (e.g. "Not Found")          |
| message    | String        | Human-readable error description               |
| path       | String        | The request URI that caused the error          |
| requestId  | String        | Optional X-Request-ID correlation value        |

- And it must match the error structure convention from 001_project_setup.md

### ValidationErrorResponse DTO

Tags: `error`, `dto`

- Given a record "ValidationErrorResponse" in package "com.cargo.booking.exception"
- Then it must include all fields from ErrorResponse
- And it must not attempt to extend ErrorResponse because Java records cannot extend another record
- And it must add an additional field:

| field       | type                    | description                                    |
| --- | --- | --- |
| violations  | List<FieldViolation>    | List of individual field validation errors      |

### FieldViolation DTO

Tags: `error`, `dto`

- Given a record "FieldViolation" in package "com.cargo.booking.exception"
- Then it must be a Java record with the following fields:

| field         | type   | description                                      |
| --- | --- | --- |
| field         | String | The field name that failed validation            |
| message       | String | The validation error message                     |
| rejectedValue | Object | The value that was rejected (nullable)           |

## Global Exception Handler

### GlobalExceptionHandler class definition

Tags: `error`, `handler`

- Given a class "GlobalExceptionHandler" in package "com.cargo.booking.exception"
- Then it must be annotated with @RestControllerAdvice
- And it must have a SLF4J logger
- And it must depend on no injected services (it only uses the request and exception)

## Business Exception Mappings

### BookingNotFoundException → 404

Tags: `error`, `handler`, `business`

- Given the GlobalExceptionHandler
- Then it must handle BookingNotFoundException with:

| annotation                                    | status | error     |
| --- | --- | --- |
| @ExceptionHandler(BookingNotFoundException.class) | 404    | Not Found |

- And the response body must be:

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Booking not found with reference BKG-2026-00042",
  "path": "/api/v1/bookings/BKG-2026-00042"
}
```
- And the exception must be logged at WARN level (not ERROR — it's a client issue)

### IllegalStateTransitionException → 409

Tags: `error`, `handler`, `business`

- Given the GlobalExceptionHandler
- Then it must handle IllegalStateTransitionException with:

| annotation                                             | status | error    |
| --- | --- | --- |
| @ExceptionHandler(IllegalStateTransitionException.class) | 409    | Conflict |

- And the response body must be:

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot transition booking from COMPLETED to CANCELLED",
  "path": "/api/v1/bookings/f47ac10b/cancel"
}
```
- And the exception must be logged at WARN level

### ScheduleNotAvailableException → 422

Tags: `error`, `handler`, `business`

- Given the GlobalExceptionHandler
- Then it must handle ScheduleNotAvailableException with:

| annotation                                              | status | error                |
| --- | --- | --- |
| @ExceptionHandler(ScheduleNotAvailableException.class)  | 422    | Unprocessable Entity |

- And the message must describe why the schedule is not available
- And the exception must be logged at WARN level

### QuoteNotValidException → 422

Tags: `error`, `handler`, `business`

- Given the GlobalExceptionHandler
- Then it must handle QuoteNotValidException with:

| annotation                                        | status | error                |
| --- | --- | --- |
| @ExceptionHandler(QuoteNotValidException.class)   | 422    | Unprocessable Entity |

- And the message must describe why the quote is not valid
- And the exception must be logged at WARN level

### EquipmentReservationException → 503

Tags: `error`, `handler`, `business`

- Given the GlobalExceptionHandler
- Then it must handle EquipmentReservationException with:

| annotation                                              | status | error               |
| --- | --- | --- |
| @ExceptionHandler(EquipmentReservationException.class)  | 503    | Service Unavailable |

- And the message must indicate that equipment reservation is temporarily unavailable
- And the exception must be logged at ERROR level (this is a system issue)

### BookingValidationException → 400

Tags: `error`, `handler`, `business`

- Given the GlobalExceptionHandler
- Then it must handle BookingValidationException with:

| annotation                                            | status | error       |
| --- | --- | --- |
| @ExceptionHandler(BookingValidationException.class)   | 400    | Bad Request |

- And the message must describe the validation failure
- And invalid booking identifiers for GET /api/v1/bookings/{id} must be handled here because the controller receives the path variable as String
- And the exception must be logged at WARN level

## Validation Exception Mappings

### MethodArgumentNotValidException → 400 with field details

Tags: `error`, `handler`, `validation`

- Given the GlobalExceptionHandler
- Then it must handle MethodArgumentNotValidException with:

| annotation                                                    | status | error       |
| --- | --- | --- |
| @ExceptionHandler(MethodArgumentNotValidException.class)      | 400    | Bad Request |

- And the response must use ValidationErrorResponse (not plain ErrorResponse)
- And the violations list must be built from the BindingResult:

| step | action                                                                       |
| --- | --- |
| 1    | Iterate over all FieldError entries in the BindingResult                     |
| 2    | Map each to a FieldViolation with field name, message, and rejected value    |
| 3    | Sort violations alphabetically by field name for consistency                 |

- And the response body must look like:

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed with 2 error(s)",
  "path": "/api/v1/bookings",
  "requestId": "req-123",
  "violations": [
    {
      "field": "cargo.weightKg",
      "message": "must be greater than or equal to 0.01",
      "rejectedValue": -5
    },
    {
      "field": "customer.email",
      "message": "must be a well-formed email address",
      "rejectedValue": "not-an-email"
    }
  ]
}
```
- And the exception must be logged at WARN level with the number of violations

### ConstraintViolationException → 400 with field details

Tags: `error`, `handler`, `validation`

- Given the GlobalExceptionHandler
- Then it must handle ConstraintViolationException (Jakarta) with:

| annotation                                                  | status | error       |
| --- | --- | --- |
| @ExceptionHandler(ConstraintViolationException.class)       | 400    | Bad Request |

- And it must map each ConstraintViolation to a FieldViolation
- And the response format must match the MethodArgumentNotValidException format above

### HttpMessageNotReadableException → 400

Tags: `error`, `handler`, `validation`

- Given the GlobalExceptionHandler
- Then it must handle HttpMessageNotReadableException with:

| annotation                                                    | status | error       |
| --- | --- | --- |
| @ExceptionHandler(HttpMessageNotReadableException.class)      | 400    | Bad Request |

- And the message must be "Malformed JSON request body"
- And the raw exception message must NOT be exposed (it may contain internal details)
- And the exception must be logged at WARN level

### MissingServletRequestParameterException → 400

Tags: `error`, `handler`, `validation`

- Given the GlobalExceptionHandler
- Then it must handle MissingServletRequestParameterException with:

| annotation                                                            | status | error       |
| --- | --- | --- |
| @ExceptionHandler(MissingServletRequestParameterException.class)      | 400    | Bad Request |

- And the message must indicate which parameter is missing
- And conditional parameters such as customerId on GET /api/v1/bookings must be validated by controller/security logic, not by Spring's required-parameter mechanism
- And the exception must be logged at WARN level

### MethodArgumentTypeMismatchException → 400

Tags: `error`, `handler`, `validation`

- Given the GlobalExceptionHandler
- Then it must handle MethodArgumentTypeMismatchException with:

| annotation                                                          | status | error       |
| --- | --- | --- |
| @ExceptionHandler(MethodArgumentTypeMismatchException.class)        | 400    | Bad Request |

- And the message must indicate the parameter name and expected type
- And example: "Parameter 'status' must be a valid BookingStatus"
- And the exception must be logged at WARN level

## HTTP Method and Path Errors

### HttpRequestMethodNotSupportedException → 405

Tags: `error`, `handler`, `http`

- Given the GlobalExceptionHandler
- Then it must handle HttpRequestMethodNotSupportedException with:

| annotation                                                             | status | error              |
| --- | --- | --- |
| @ExceptionHandler(HttpRequestMethodNotSupportedException.class)        | 405    | Method Not Allowed |

- And the message must indicate the method used and the supported methods
- And example: "Method 'DELETE' is not supported. Supported methods: GET, POST, PATCH"
- And the exception must be logged at WARN level

### NoHandlerFoundException → 404

Tags: `error`, `handler`, `http`

- Given the GlobalExceptionHandler
- Then it must handle NoHandlerFoundException with:

| annotation                                            | status | error     |
| --- | --- | --- |
| @ExceptionHandler(NoHandlerFoundException.class)      | 404    | Not Found |

- And the message must be "No endpoint found for {METHOD} {PATH}"
- And the exception must be logged at WARN level
- And the following property must be set in application.yml:

| property                                            | value |
| --- | --- |
| spring.mvc.throw-exception-if-no-handler-found      | true  |

- And the implementation must preserve SpringDoc / Swagger UI static resource mappings
- And unknown "/api/**" paths must still return the standard JSON ErrorResponse format

## Security Exceptions (fallback — primary handled in 006)

### AccessDeniedException → 403 (fallback)

Tags: `error`, `handler`, `security`

- Given the GlobalExceptionHandler
- Then it must handle AccessDeniedException with:

| annotation                                              | status | error     |
| --- | --- | --- |
| @ExceptionHandler(AccessDeniedException.class)          | 403    | Forbidden |

- And the message must be "You do not have permission to perform this action"
- And this is a fallback — the primary 403 handling is in JwtAccessDeniedHandler (006_security.md)
- And the exception must be logged at WARN level

## Catch-All Handler

### Generic Exception → 500

Tags: `error`, `handler`, `catchall`

- Given the GlobalExceptionHandler
- Then it must handle Exception.class as a catch-all with:

| annotation                              | status | error                  |
| --- | --- | --- |
| @ExceptionHandler(Exception.class)      | 500    | Internal Server Error  |

- And the message must be "An unexpected error occurred. Please try again later."
- And the actual exception details must NOT be exposed in the response body
- And the exception must be logged at ERROR level with the full stack trace
- And this handler must be the lowest priority (@Order(Ordered.LOWEST_PRECEDENCE))

## Error Response Builder Utility

### ErrorResponseBuilder utility

Tags: `error`, `utility`

- Given a utility class "ErrorResponseBuilder" in package "com.cargo.booking.exception"
- Then it must provide static factory methods to reduce boilerplate in the handler:

| method                                                                              | returns               |
| --- | --- |
| buildError(HttpStatus status, String message, HttpServletRequest request)            | ErrorResponse         |
| buildValidationError(HttpStatus status, String message, List<FieldViolation> violations, HttpServletRequest request) | ValidationErrorResponse |

- And each method must:

| rule                                                          |
| --- |
| Set timestamp to Instant.now() in UTC                         |
| Set status from the HttpStatus code                           |
| Set error from the HttpStatus reason phrase                   |
| Set path from request.getRequestURI()                         |
| Set requestId from the X-Request-ID header when present        |

## Exception Hierarchy Summary

### Complete exception-to-status mapping reference

Tags: `error`, `reference`

- Given all exception handlers defined above
- Then the full mapping must be:

| exception                                  | HTTP status | log level | category        |
| --- | --- | --- | --- |
| BookingNotFoundException                   | 404         | WARN      | Business        |
| IllegalStateTransitionException            | 409         | WARN      | Business        |
| ScheduleNotAvailableException              | 422         | WARN      | Business        |
| QuoteNotValidException                     | 422         | WARN      | Business        |
| EquipmentReservationException              | 503         | ERROR     | Integration     |
| BookingValidationException                 | 400         | WARN      | Business        |
| MethodArgumentNotValidException            | 400         | WARN      | Validation      |
| ConstraintViolationException               | 400         | WARN      | Validation      |
| HttpMessageNotReadableException            | 400         | WARN      | Validation      |
| MissingServletRequestParameterException    | 400         | WARN      | Validation      |
| MethodArgumentTypeMismatchException         | 400         | WARN      | Validation      |
| HttpRequestMethodNotSupportedException     | 405         | WARN      | HTTP            |
| NoHandlerFoundException                    | 404         | WARN      | HTTP            |
| AccessDeniedException                      | 403         | WARN      | Security        |
| Exception (catch-all)                      | 500         | ERROR     | System          |

- And client errors (4xx) must be logged at WARN level
- And server errors (5xx) must be logged at ERROR level with stack traces

## Logging Configuration for Errors

### Error logging standards

Tags: `error`, `logging`

- Given the GlobalExceptionHandler
- Then the following logging rules must apply:

| rule                                                                                         |
| --- |
| WARN-level logs must include: exception class name, message, request method, request path   |
| ERROR-level logs must include: all WARN fields plus the full stack trace                     |
| Logs must never include request body content (may contain sensitive data)                    |
| Logs must never include Authorization header values                                         |
| Logs must include a correlation/request ID if one is present in the headers (X-Request-ID)  |

### Request ID propagation

Tags: `error`, `logging`

- Given incoming requests may carry an "X-Request-ID" header
- Then the error response should include the request ID if present:

| field      | source                                              |
| --- | --- |
| requestId  | Value from X-Request-ID header when present          |

- And the ErrorResponse record already includes an optional "requestId" field (nullable)
- And the GlobalExceptionHandler must extract this header from the HttpServletRequest
- And requestId must be omitted from JSON responses when absent because null fields are globally omitted

## Spring Boot Error Properties

### Disable default Spring Boot error page

Tags: `error`, `config`

- Given the file "src/main/resources/application.yml"
- Then it must include the following properties to disable Spring's default error handling:

| property                                          | value  | purpose                                      |
| --- | --- | --- |
| server.error.include-message                      | never  | Prevent Spring from leaking error messages   |
| server.error.include-binding-errors               | never  | Prevent Spring from leaking binding errors   |
| server.error.include-stacktrace                   | never  | Prevent stack trace in responses             |
| server.error.whitelabel.enabled                   | false  | Disable the default white-label error page   |

## Out of Scope for this file

### Items NOT covered in error handling

Tags: `error`, `out-of-scope`

- Given this is the error handling file only
- Then the following are NOT defined here and will be addressed in later files:

| topic                                               | deferred to           |
| --- | --- |
| Circuit breaker / retry error handling for clients  | 008_integrations.md   |
| Error handling integration tests                    | 009_testing.md        |
| Error monitoring and alerting setup                 | 010_deployment.md     |
