# Booking Flow, Phase by Phase (Sequence Diagrams)

The same "book a seat" request, growing from a simple monolith to the final system.
Each phase starts from the previous diagram and changes only what is noted.

---

## Phase 1: Simple monolith

```mermaid
sequenceDiagram
    actor Client
    participant C as BookingController
    participant S as BookingService
    participant SR as SeatRepository
    participant BR as BookingRepository
    participant DB as PostgreSQL

    Client->>C: POST /bookings (showId, userId, seatIds)
    C->>S: createBooking(request)
    S->>SR: find seats by ids
    SR->>DB: SELECT seats
    DB-->>SR: seats
    SR-->>S: seats
    alt any seat BOOKED
        S-->>C: error
        C-->>Client: 500 or raw error
    else all AVAILABLE
        S->>SR: save seats as BOOKED
        SR->>DB: UPDATE seats
        S->>BR: save booking
        BR->>DB: INSERT booking and booking_seats
        S-->>C: booking
        C-->>Client: 201 Booking CONFIRMED
    end
```

Problem left: no transaction, no clean errors, no login.

---

## Phase 2: Transactions and error handling

```mermaid
sequenceDiagram
    actor Client
    participant C as BookingController
    participant S as BookingService (Transactional)
    participant R as Repositories
    participant DB as PostgreSQL
    participant EH as GlobalExceptionHandler

    Client->>C: POST /api/v1/bookings (BookingRequest DTO)
    C->>C: validate DTO
    C->>S: createBooking(request)
    Note over S,DB: Transaction begins
    S->>R: load seats
    R->>DB: SELECT
    alt seat already BOOKED
        S-->>EH: SeatAlreadyBookedException
        EH-->>Client: 409 clean error JSON
    else all AVAILABLE
        S->>R: mark seats BOOKED and save booking
        R->>DB: UPDATE and INSERT
        Note over S,DB: Commit (any failure rolls back everything)
        S-->>C: BookingResponse DTO
        C-->>Client: 201 Booking CONFIRMED
    end
```

Changed: all-or-nothing booking, DTOs, `/api/v1`, uniform errors, logging.

---

## Phase 3: Database performance

```mermaid
sequenceDiagram
    actor Client
    participant C as Controller
    participant S as Service (Transactional)
    participant R as Repositories
    participant DB as PostgreSQL (indexed)

    Client->>C: GET /api/v1/movies?name=&page=&size=
    C->>S: search movies (Pageable)
    S->>R: DTO projection query
    R->>DB: one query using index (no N+1)
    DB-->>R: one page of rows
    R-->>Client: 200 page of movies

    Client->>C: POST /api/v1/bookings
    C->>S: createBooking
    S->>R: fetch show and seats with fetch join
    R->>DB: single query
    S->>R: save booking
    C-->>Client: 201 Booking CONFIRMED
```

Changed: booking steps same, reads are fast (fetch join, projections, pagination, indexes, tuned pool).

---

## Phase 4: Concurrency and locking

```mermaid
sequenceDiagram
    actor A as User A
    actor B as User B
    participant S as BookingService (Transactional)
    participant DB as PostgreSQL

    A->>S: book seat A1
    B->>S: book seat A1 (same moment)
    S->>DB: load seat A1 with lock or version for A
    S->>DB: load seat A1 with lock or version for B
    Note over S,DB: Optimistic: version check at save. Pessimistic: SELECT FOR UPDATE
    S->>DB: A saves seat and booking
    DB-->>S: success
    S-->>A: 201 Booking CONFIRMED
    S->>DB: B tries to save
    DB-->>S: lock exception (version changed or seat already booked)
    S-->>B: 409 Seat already taken
```

Changed: exactly one booking wins, the other gets a clear 409.

---

## Phase 5: Security (JWT and roles)

```mermaid
sequenceDiagram
    actor Client
    participant F as JwtFilter
    participant C as BookingController (PreAuthorize)
    participant S as BookingService
    participant DB as PostgreSQL

    Client->>F: POST /api/v1/bookings (Authorization Bearer token)
    F->>F: verify signature and expiry
    alt token invalid or missing
        F-->>Client: 401 Unauthorized
    else token valid
        F->>C: request with logged-in user
        alt role not allowed
            C-->>Client: 403 Forbidden
        else role USER
            C->>S: createBooking(request, userId from token)
            S->>DB: lock seats, save booking for this user
            S-->>C: booking
            C-->>Client: 201 Booking CONFIRMED
        end
    end

    Client->>F: GET /api/v1/bookings/77
    F->>C: request with user
    C->>S: getBooking(77, userId)
    S->>DB: load booking
    alt booking.userId is not the caller
        S-->>Client: 403 Forbidden
    else owner
        S-->>Client: 200 booking
    end
```

Changed: userId comes from the token, ownership is checked, roles are enforced.

---

## Phase 6: Microservices split

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant EU as Eureka
    participant BK as Booking Service
    participant CN as Cinema Service
    participant PY as Payment Service
    participant BDB as Booking DB
    participant CDB as Cinema DB
    participant PDB as Payment DB

    Client->>GW: POST /api/v1/bookings (JWT)
    GW->>GW: validate JWT once
    GW->>EU: find Booking Service
    GW->>BK: forward request
    BK->>CN: Feign GET /internal/shows/id (price)
    CN->>CDB: read show
    CN-->>BK: show and price
    BK->>CN: Feign POST seats/reserve
    CN->>CDB: mark seats reserved
    CN-->>BK: reserved
    BK->>PY: Feign POST /api/v1/payments (blocking)
    PY->>PDB: save payment
    PY-->>BK: SUCCESS
    BK->>BDB: save booking CONFIRMED
    BK-->>Client: 201 Booking CONFIRMED
```

Changed: no single transaction across services, calls go over the network, each service owns its database.
Problem left: if Payment fails after seats are reserved, data goes out of sync.

---

## Phase 7: Kafka, saga, and outbox

```mermaid
sequenceDiagram
    actor Client
    participant BK as Booking Service
    participant OB as Outbox publisher (Scheduled)
    participant K as Kafka
    participant PY as Payment Service
    participant CN as Cinema Service

    Client->>BK: POST /api/v1/bookings
    BK->>BK: one transaction: save booking CREATED and outbox event
    BK-->>Client: 201 Booking CREATED
    OB->>BK: read unpublished outbox rows
    OB->>K: publish BookingCreated
    K->>PY: BookingCreated
    PY->>PY: skip if messageId already processed
    PY->>PY: charge
    alt payment succeeds
        PY->>K: PaymentCompleted
        K->>BK: PaymentCompleted
        BK->>BK: booking CONFIRMED
    else payment fails
        PY->>K: PaymentFailed
        K->>BK: PaymentFailed
        BK->>BK: booking PAYMENT_FAILED
        BK->>CN: release seats (compensation)
    end
    Note over K,PY: Bad message: retry with delay, then Dead Letter Queue
```

Cancel saga: `POST /bookings/id/cancel` marks cancelled, Payment refunds, Cinema releases seats.

Changed: async events, saga with undo steps, outbox (no lost messages), idempotent consumers (no duplicates), retries and DLQ.

---

## Phase 8: Idempotency, webhooks, notifications

```mermaid
sequenceDiagram
    actor Client
    participant BK as Booking Service
    participant PY as Payment Service
    participant GWY as Fake Payment Gateway
    participant K as Kafka
    participant NT as Notification Service

    Client->>BK: POST /bookings (Idempotency-Key: abc)
    BK->>BK: look up key abc
    alt key seen with same request
        BK-->>Client: saved response (no second booking)
    else key seen with different request
        BK-->>Client: 409 or 422 key reuse
    else new key
        BK->>BK: save key and continue Phase 7 flow
        BK-->>Client: 201 Booking CREATED
    end

    PY->>GWY: start charge
    GWY-->>PY: PENDING
    GWY->>PY: webhook POST /api/v1/webhooks/payment (signed)
    PY->>PY: verify signature, ignore duplicates, handle out-of-order
    PY->>K: PaymentCompleted
    K->>BK: PaymentCompleted
    BK->>K: BookingConfirmed (carries email)
    K->>NT: BookingConfirmed
    NT->>NT: EmailSender sends, save NotificationLog
```

Changed: safe retries, async payment result through webhook, email notification.

---

## Phase 9: Redis (cache and seat hold)

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant BK as Booking Service
    participant R as Redis
    participant CN as Cinema Service
    participant CDB as Cinema DB

    Client->>GW: GET /api/v1/shows/12
    GW->>CN: forward
    CN->>R: get cached show
    alt cache hit
        R-->>CN: show
    else cache miss
        CN->>CDB: read show
        CN->>R: store with TTL
    end
    CN-->>Client: 200 show

    Client->>GW: POST /api/v1/holds (showId, seatIds)
    GW->>BK: forward
    BK->>R: SET hold with 5 minute TTL (distributed lock)
    BK-->>Client: 201 hold created

    Client->>GW: POST /api/v1/bookings
    GW->>BK: forward
    BK->>R: check hold belongs to this user
    alt hold missing or expired
        BK-->>Client: 409 hold expired
    else hold valid
        BK->>BK: continue Phase 8 flow
        BK-->>Client: 201 Booking CREATED
    end
    Note over R: Unpaid hold expires by itself, seats go back on sale
```

Changed: hold, pay, then confirm or expire. Cached reads. Login rate limit (429) in User service.

---

## Phase 10: Resilience

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant R as Redis
    participant BK as Booking Service
    participant RS as Resilience4j (timeout, retry, breaker, bulkhead)
    participant PY as Payment Service

    Client->>GW: POST /api/v1/bookings
    GW->>R: check per-user rate limit
    alt limit exceeded
        GW-->>Client: 429 Too Many Requests
    else allowed
        GW->>BK: forward
        BK->>RS: call Payment
        RS->>PY: request with timeout
        alt Payment slow or down
            PY--xRS: timeout or error
            RS->>RS: retry with backoff (idempotent calls only)
            RS->>RS: breaker opens after threshold
            RS-->>BK: fallback
            BK-->>Client: 503 Payment is busy, try again shortly
        else Payment healthy
            PY-->>RS: ok
            BK-->>Client: 201 Booking CREATED
        end
    end
```

Changed: a failing dependency degrades one feature, browsing stays healthy.

---

## Phase 11: Observability

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant BK as Booking Service
    participant K as Kafka
    participant PY as Payment Service
    participant Z as Zipkin
    participant P as Prometheus

    Client->>GW: POST /api/v1/bookings
    GW->>GW: create traceId T1
    GW->>BK: forward (trace header T1)
    BK->>K: publish event (T1 in message header)
    K->>PY: consume event (T1)
    GW-->>Z: span
    BK-->>Z: span
    PY-->>Z: span
    P->>GW: scrape /actuator/prometheus
    P->>BK: scrape /actuator/prometheus
    P->>PY: scrape /actuator/prometheus
    Note over Z,P: Grafana shows latency, errors, Kafka lag. Alerts fire on high error rate or slow responses. Every log line carries T1.
```

Changed: the booking flow is the same, but you can now see where one request slowed down or failed.

---

## Phase 12: Testing and delivery

```mermaid
sequenceDiagram
    actor Dev as Developer
    participant GH as GitHub Actions
    participant T as Tests (Testcontainers)
    participant REG as Image registry
    participant K8 as Kubernetes (minikube or kind)

    Dev->>GH: git push
    GH->>GH: build with Maven
    GH->>T: unit, slice, integration, contract tests
    T->>T: real Postgres, Kafka, Redis containers
    T-->>GH: pass or fail
    alt tests pass
        GH->>REG: build and push Docker images
        REG-->>K8: rolling update
        K8-->>Dev: new version live (rollback if needed)
    else tests fail
        GH-->>Dev: build failed
    end
```

Changed: the booking flow is the same, but every change is built, tested, and packaged automatically.

---

## Summary: what each phase adds to the path

- Phase 1: Controller, Service, Repository, DB
- Phase 2: transaction, DTOs, global error handler
- Phase 3: faster reads (fetch join, projections, pagination, indexes)
- Phase 4: seat locking (optimistic and pessimistic)
- Phase 5: JWT filter, roles, ownership check
- Phase 6: Gateway, Eureka, Feign, separate services and databases
- Phase 7: Kafka, saga, outbox, idempotent consumers, DLQ
- Phase 8: idempotency keys, webhooks, email notifications
- Phase 9: Redis cache, seat hold, distributed lock, rate limit
- Phase 10: timeouts, retry, circuit breaker, bulkhead, fallbacks
- Phase 11: metrics, tracing, correlation IDs, alerts
- Phase 12: tests, Docker, CI, Kubernetes basics
