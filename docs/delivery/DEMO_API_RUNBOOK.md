# Demo and API Runbook

This runbook is for coworkers who want to run the Cargo Booking Service locally,
open the API docs, and exercise the booking lifecycle with `curl`.

## Local Startup

Prerequisites:

- Java 21
- Docker and Docker Compose for the containerized local stack
- Maven wrapper from this repository (`./mvnw`)

Start PostgreSQL and the application with the local profile:

```bash
make docker-up
```

Follow service logs:

```bash
make docker-logs
```

Stop the local stack:

```bash
make docker-down
```

Reset the Maven build output and remove the Compose database volume:

```bash
make clean
```

The Compose stack starts:

| Service | URL or port | Notes |
| --- | --- | --- |
| Booking API | `http://localhost:8081` | Spring Boot app with `/api/v1` endpoints |
| PostgreSQL | `localhost:5432` | Database `booking_db`, user `booking_user` |
| Swagger UI | `http://localhost:8081/swagger-ui` | Interactive API docs |
| OpenAPI JSON | `http://localhost:8081/api-docs` | Machine-readable API spec |
| Health | `http://localhost:8081/actuator/health` | Public health endpoint |

To open or print the Swagger UI URL:

```bash
make swagger
```

To run the app directly with Maven instead of the service container, start a
compatible PostgreSQL first, then run:

```bash
make run
```

The local profile uses stub schedule, quote, and equipment clients. Schedule and
quote validation accept the sample IDs, equipment reservation/release is logged,
and JWT security is disabled by default in Docker Compose.

## Common Shell Setup

```bash
BASE_URL=http://localhost:8081
CUSTOMER_ID=3001
```

Check that the service is reachable:

```bash
curl -sS "$BASE_URL/actuator/health"
```

Expected result: HTTP 200 with an `UP` health status when the application is
ready.

## Create Booking

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/bookings" \
  -H 'Content-Type: application/json' \
  -d '{
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
  }'
```

Expected result:

- HTTP 201 Created.
- Response includes `id`, `bookingReference`, `customerId`, `status`, and
  `createdAt`.
- `status` is `PENDING`.
- `bookingReference` follows `BKG-YYYY-NNNNN`.

Use the returned numeric ID and reference in later examples:

```bash
BOOKING_ID=<id-from-create-response>
BOOKING_REF=<bookingReference-from-create-response>
```

## List Bookings

```bash
curl -i -sS "$BASE_URL/api/v1/bookings?customerId=$CUSTOMER_ID&page=0&size=20"
```

Expected result:

- HTTP 200 OK.
- Response is a paged object with `content`, `page`, `size`, `totalElements`,
  `totalPages`, and `last`.
- The new booking appears in `content` with status `PENDING`.

Filter by lifecycle status:

```bash
curl -i -sS "$BASE_URL/api/v1/bookings?customerId=$CUSTOMER_ID&status=PENDING"
```

Expected result: HTTP 200 OK with only matching bookings for the requested
customer and status.

## Get Booking

Fetch by numeric ID:

```bash
curl -i -sS "$BASE_URL/api/v1/bookings/$BOOKING_ID"
```

Fetch by booking reference:

```bash
curl -i -sS "$BASE_URL/api/v1/bookings/$BOOKING_REF"
```

Expected result:

- HTTP 200 OK.
- Response includes the full booking: IDs, reference, status, customer, cargo,
  equipment, `createdAt`, and `updatedAt`.
- Invalid identifiers return HTTP 400.
- Missing bookings return HTTP 404.

## Complete Lifecycle

The legal success path is:

```text
PENDING -> CONFIRMED -> IN_PROGRESS -> COMPLETED
```

Confirm the booking:

```bash
curl -i -sS -X PATCH "$BASE_URL/api/v1/bookings/$BOOKING_ID/confirm"
```

Expected result: HTTP 200 OK and `status` becomes `CONFIRMED`. In local mode,
the equipment reservation stub accepts the request and writes a log line.

Start the booking:

```bash
curl -i -sS -X PATCH "$BASE_URL/api/v1/bookings/$BOOKING_ID/start"
```

Expected result: HTTP 200 OK and `status` becomes `IN_PROGRESS`.

Complete the booking:

```bash
curl -i -sS -X PATCH "$BASE_URL/api/v1/bookings/$BOOKING_ID/complete"
```

Expected result: HTTP 200 OK and `status` becomes `COMPLETED`.

List completed bookings:

```bash
curl -i -sS "$BASE_URL/api/v1/bookings?customerId=$CUSTOMER_ID&status=COMPLETED"
```

Expected result: HTTP 200 OK and the completed booking appears in the page.

## Cancellation Flow

Cancellation is terminal and is not valid after completion, so use a separate
new booking for this demo.

Create another booking with the same create command, then set:

```bash
CANCEL_BOOKING_ID=<id-from-second-create-response>
```

Cancel while pending:

```bash
curl -i -sS -X PATCH "$BASE_URL/api/v1/bookings/$CANCEL_BOOKING_ID/cancel"
```

Expected result: HTTP 200 OK and `status` becomes `CANCELLED`.

To demonstrate cancellation after equipment reservation, create a third booking,
confirm it, then cancel it:

```bash
RESERVED_BOOKING_ID=<id-from-third-create-response>

curl -i -sS -X PATCH "$BASE_URL/api/v1/bookings/$RESERVED_BOOKING_ID/confirm"
curl -i -sS -X PATCH "$BASE_URL/api/v1/bookings/$RESERVED_BOOKING_ID/cancel"
```

Expected result:

- Confirm returns HTTP 200 OK with `status` `CONFIRMED`.
- Cancel returns HTTP 200 OK with `status` `CANCELLED`.
- In local mode, the equipment release stub accepts the request and writes a log
  line.

Invalid lifecycle attempts return HTTP 409 Conflict. Examples include cancelling
a `COMPLETED` booking, starting a `PENDING` booking, or completing a
`CONFIRMED` booking before it is started.

## Security Notes

The Docker Compose local stack sets `SECURITY_ENABLED=false`, so the curl
examples do not need JWT bearer tokens. When security is enabled in dev or prod,
protected endpoints require a valid JWT with the role and customer ownership
rules documented in `AGENTS.md` and `specs/006_security.md`.
