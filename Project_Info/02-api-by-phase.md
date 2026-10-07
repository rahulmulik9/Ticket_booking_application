# Ticket Booking System: API, Phase by Phase

Each phase lists what is **new**, **changed**, and **removed** in the API. The final list of all endpoints is at the end.

## Rules for the whole API

- Use plural nouns for resources: `/movies`, `/shows`, `/bookings`.
- Use HTTP methods for the action: GET reads, POST creates, DELETE removes.
- Money is sent as a number with two decimals, never as a string with a currency sign.
- `/internal/...` paths are used only between services and are never exposed by the Gateway.

## Status codes we use

- 200 OK: read or action worked
- 201 Created: a new resource was made
- 204 No Content: delete worked
- 400 Bad Request: invalid input
- 401 Unauthorized: not logged in (Phase 5)
- 403 Forbidden: logged in but not allowed (Phase 5)
- 404 Not Found: the resource does not exist
- 409 Conflict: seat already taken, or duplicate request
- 429 Too Many Requests: rate limit hit (Phases 9 and 10)
- 503 Service Unavailable: a dependency is down (Phase 10)

---

## Phase 0: Requirements and Design

- No endpoints. The list below is the design.

---

## Phase 1: Basic CRUD

Plain paths, no login, one app.

**New (10 public endpoints, plus health)**

| Method | Path | What it does | Success | Errors |
|---|---|---|---|---|
| POST | /movies | Add a movie | 201 | 400 |
| GET | /movies | List movies | 200 | none |
| GET | /movies/{id} | View one movie | 200 | 404 |
| POST | /movies/{id}/shows | Add a show and generate its seats | 201 | 400, 404 |
| GET | /movies/{id}/shows | List shows of a movie | 200 | 404 |
| GET | /shows/{id} | View one show | 200 | 404 |
| GET | /shows/{id}/seats | List seats with status | 200 | 404 |
| POST | /bookings | Book seats (`showId`, `userId`, `seatIds`) | 201 | 400, 404, 409 |
| GET | /bookings/{id} | View a booking | 200 | 404 |
| POST | /bookings/{id}/cancel | Cancel a booking | 200 | 404, 409 |
| GET | /actuator/health | Health check | 200 | none |

Everyone can call everything. Errors are raw at this stage.

---

## Phase 2: Transactions and Error Handling

**New**
- Swagger UI at `/swagger-ui.html`
- OpenAPI JSON at `/v3/api-docs`

**Changed**
- Every path now starts with `/api/v1`. For example `POST /movies` becomes `POST /api/v1/movies`.
- Requests and responses use DTOs, and input is validated.
- Errors use one clean format:
  - 400 for validation
  - 404 for a missing resource
  - 409 for a seat already taken
- `GET /actuator/health` stays at the same path.

**Removed**
- The unversioned paths from Phase 1.

**Error format**

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Seat A1 is already booked",
  "timestamp": "2026-10-06T18:30:05"
}
```

**Booking request and response**

Request `POST /api/v1/bookings`:
```json
{
  "showId": 12,
  "userId": 5,
  "seatIds": [101, 102]
}
```

Response (201):
```json
{
  "id": 77,
  "showId": 12,
  "userId": 5,
  "seatNumbers": ["A1", "A2"],
  "totalAmount": 500.00,
  "status": "CONFIRMED",
  "createdAt": "2026-10-06T18:30:00"
}
```

---

## Phase 3: Database Performance

**New:** no new paths

**Changed**
- `GET /api/v1/movies` accepts query parameters:
  - `page` (0-based) and `size`
  - `sort`, for example `sort=title,asc`
  - `name`, a partial and case-insensitive search
- The response is now a page (the list in `content`, plus paging fields), not a plain list.
- Listing returns light DTOs with only the needed fields.

**Removed:** the plain-list response of `GET /api/v1/movies`

---

## Phase 4: Concurrency and Locking

**New:** no new paths

**Changed**
- `POST /api/v1/bookings` returns a clear 409 when two people clash on the same seat. The loser can try again with other seats.

**Removed:** nothing

---

## Phase 5: Security (JWT and Roles)

Still one app. All protected calls need the header `Authorization: Bearer <token>`.

**New (5 endpoints, 15 public in total)**

| Method | Path | What it does | Who | Success | Errors |
|---|---|---|---|---|---|
| POST | /api/v1/auth/register | Sign up | Public | 201 | 400, 409 |
| POST | /api/v1/auth/login | Log in, get access and refresh tokens | Public | 200 | 400, 401 |
| POST | /api/v1/auth/refresh | Get a new access token | Public (needs refresh token) | 200 | 401 |
| POST | /api/v1/auth/logout | Revoke the refresh token | Logged in | 200 | 401 |
| GET | /api/v1/bookings | List my own bookings | USER | 200 | 401 |

**Changed**

| Endpoint | Before | Now |
|---|---|---|
| POST /api/v1/movies | Anyone | ORGANIZER or ADMIN |
| POST /api/v1/movies/{id}/shows | Anyone | ORGANIZER or ADMIN |
| POST /api/v1/bookings | `userId` in the body | User comes from the token |
| GET /api/v1/bookings/{id} | Anyone | Owner only |
| POST /api/v1/bookings/{id}/cancel | Anyone | Owner only |

- Browsing movies, shows, and seats stays public.
- Protected endpoints can now return 401 and 403.
- Swagger UI gets an **Authorize** button.

**Removed**
- `userId` from the `POST /api/v1/bookings` request body.

**Samples**

Register:
```json
{
  "name": "Asha",
  "email": "asha@example.com",
  "password": "Secret123"
}
```

Login response:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "d3f...",
  "tokenType": "Bearer"
}
```

Booking request (no `userId` now):
```json
{
  "showId": 12,
  "seatIds": [101, 102]
}
```

---

## Phase 6: Microservices Split

All public calls now go through the **API Gateway**. The public paths stay the same as Phase 5, but each one lives in its own service. JWT is checked once, at the Gateway.

**Where each endpoint lives**
- User service: the four `/api/v1/auth/...` endpoints
- Cinema service: all movie, show, and seat endpoints
- Booking service: all booking endpoints
- Payment service: payment endpoints (new)
- Notification service: no public endpoint

**Gateway routes**
- `/api/v1/auth/**` goes to User
- `/api/v1/movies/**` and `/api/v1/shows/**` go to Cinema
- `/api/v1/bookings/**` goes to Booking
- `/api/v1/payments/**` goes to Payment

**New (17 public endpoints in total)**

| Service | Method | Path | What it does | Success | Errors |
|---|---|---|---|---|---|
| Payment | POST | /api/v1/payments | Start a payment (`bookingId`, `amount`) | 201 | 400, 404 |
| Payment | GET | /api/v1/payments/{id} | View a payment | 200 | 404 |

**New internal endpoints (Cinema, not exposed by the Gateway)**

| Method | Path | What it does |
|---|---|---|
| GET | /internal/shows/{id} | Price and details of a show, for Booking |
| POST | /internal/shows/{id}/seats/reserve | Mark seats reserved |
| POST | /internal/shows/{id}/seats/release | Put seats back on sale |

**Changed**
- `POST /api/v1/bookings` now calls Cinema (price, reserve seats) and Payment over Feign, and waits for them.
- Each service has its own Swagger page.

**Removed:** nothing

---

## Phase 7: Kafka, Saga, and Outbox

**New (18 public endpoints in total)**

| Service | Method | Path | What it does | Success | Errors |
|---|---|---|---|---|---|
| Payment | POST | /api/v1/payments/{id}/refund | Refund a payment | 200 | 404, 409 |

**Changed**
- `POST /api/v1/bookings` returns the booking as `CREATED` (waiting for payment) instead of `CONFIRMED`.
- `POST /api/v1/bookings/{id}/cancel` starts the saga: cancel booking, refund payment, release seats.
- `GET /api/v1/bookings/{id}` can return `CREATED`, `CONFIRMED`, `CANCELLED`, or `PAYMENT_FAILED`. The client checks it later to see the final result.
- `GET /api/v1/payments/{id}` can return `REFUNDED`.

**Removed**
- The blocking Feign call from Booking to Payment. Events replace it.

---

## Phase 8: Payments, Webhooks, and Notifications

**New (19 public endpoints in total)**

| Service | Method | Path | What it does | Who | Success | Errors |
|---|---|---|---|---|---|---|
| Payment | POST | /api/v1/webhooks/payment | Called by the fake gateway with the payment result | Fake gateway (signature, not JWT) | 200 | 400, 401 |

**Changed**
- `POST /api/v1/bookings` and `POST /api/v1/payments` accept an `Idempotency-Key` header:
  - same key and same request: returns the saved response, no second booking or charge
  - same key and a different request: rejected with 409
  - two requests with the same key at the same time: only one runs, the other gets 409
- `GET /api/v1/payments/{id}` now shows the result moving from `PENDING` to its final status, updated by the webhook.
- The webhook checks a signature header, ignores duplicate calls (still returns 200), and handles out-of-order calls.
- The Gateway lets the webhook path through without a user JWT.
- Notification service: still no public endpoint. It listens to Kafka, and the event carries the email address.

**Removed:** nothing

---

## Phase 9: Redis (Caching and Seat Hold)

**New (21 public endpoints in total)**

| Service | Method | Path | What it does | Success | Errors |
|---|---|---|---|---|---|
| Booking | POST | /api/v1/holds | Hold seats for 5 minutes (`showId`, `seatIds`) | 201 | 400, 401, 409 |
| Booking | DELETE | /api/v1/holds/{id} | Release a hold before it expires | 204 | 401, 403, 404 |

**Changed**
- `POST /api/v1/bookings` now checks that the user holds the seats. It returns 409 if the hold is missing or expired.
- `GET /api/v1/movies` and `GET /api/v1/shows/{id}` are faster because of the cache. The response shape is the same.
- `POST /api/v1/auth/login` returns 429 after too many attempts.

**Removed:** nothing

**Hold sample**

Request `POST /api/v1/holds`:
```json
{
  "showId": 12,
  "seatIds": [101, 102]
}
```

Response (201):
```json
{
  "id": "h-9f3a",
  "showId": 12,
  "seatIds": [101, 102],
  "expiresAt": "2026-10-06T18:35:00"
}
```

---

## Phase 10: Resilience

**New:** no new paths

**Changed**
- When Payment is slow or down, `POST /api/v1/bookings` and `POST /api/v1/payments` return 503 with a friendly message such as "Payment is busy, try again shortly".
- The Gateway returns 429 when a user sends too many requests.
- Browsing movies and shows keeps working while Payment is down.

**Removed:** nothing

---

## Phase 11: Observability

**New (on every service)**
- `GET /actuator/health` with liveness and readiness (`/actuator/health/liveness`, `/actuator/health/readiness`)
- `GET /actuator/prometheus`

**Changed:** nothing in the public API

**Removed:** nothing

These are for operators and Prometheus. Do not expose them through the Gateway.

---

## Phase 12: Testing and Delivery

- No new endpoints.
- Contract tests check that endpoints between services keep their agreed shape (the OpenAPI files).

---

## Final list: all 21 public endpoints

**User service**

| Method | Path | Who |
|---|---|---|
| POST | /api/v1/auth/register | Public |
| POST | /api/v1/auth/login | Public (429 on too many attempts) |
| POST | /api/v1/auth/refresh | Public (needs refresh token) |
| POST | /api/v1/auth/logout | Logged in |

**Cinema service**

| Method | Path | Who |
|---|---|---|
| POST | /api/v1/movies | ORGANIZER, ADMIN |
| GET | /api/v1/movies | Public (cached) |
| GET | /api/v1/movies/{id} | Public |
| POST | /api/v1/movies/{id}/shows | ORGANIZER, ADMIN |
| GET | /api/v1/movies/{id}/shows | Public |
| GET | /api/v1/shows/{id} | Public (cached) |
| GET | /api/v1/shows/{id}/seats | Public |

**Booking service**

| Method | Path | Who |
|---|---|---|
| POST | /api/v1/bookings | USER (`Idempotency-Key`) |
| GET | /api/v1/bookings | USER (own bookings) |
| GET | /api/v1/bookings/{id} | Owner only |
| POST | /api/v1/bookings/{id}/cancel | Owner only |
| POST | /api/v1/holds | USER |
| DELETE | /api/v1/holds/{id} | Owner only |

**Payment service**

| Method | Path | Who |
|---|---|---|
| POST | /api/v1/payments | USER (`Idempotency-Key`) |
| GET | /api/v1/payments/{id} | Owner only |
| POST | /api/v1/payments/{id}/refund | Called by the cancel saga |
| POST | /api/v1/webhooks/payment | Fake gateway (signature) |

**Internal (Cinema, never exposed by the Gateway)**
- `GET /internal/shows/{id}`
- `POST /internal/shows/{id}/seats/reserve`
- `POST /internal/shows/{id}/seats/release`

**On every service:** `GET /actuator/health` and `GET /actuator/prometheus`

## Summary: public endpoint count by phase

- Phase 1 to 4: 10 (plus health)
- Phase 5: 15
- Phase 6: 17 (plus 3 internal)
- Phase 7: 18
- Phase 8: 19
- Phase 9 to 12: 21 (final)
