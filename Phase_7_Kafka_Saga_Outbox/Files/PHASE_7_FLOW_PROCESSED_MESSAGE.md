# Phase 7: Booking Flow with the Processed-Message Check

This file shows the **same happy path**, now with the idempotency check added to every consumer.
The outbox is still there. The new part is the **CLAIM** step in each service.

## Why it is needed

The outbox delivers every event **at least once**. If a service crashes after sending but before marking
the row as published, the same event is sent again. A repeated event must not do its work twice,
for example charge the customer twice or free seats that someone else has just booked.

## How the check works

- Every event has a unique `eventId`.
- Every consuming service has a table `processed_messages` with the `eventId` as primary key.
- Before doing its work, the consumer **claims** the `eventId` with
  `INSERT ... ON CONFLICT DO NOTHING`.
  - The insert adds a row: first time, so do the work.
  - The insert adds nothing: the event was already handled, so skip it.
- The claim and the work are in **one transaction**. If the work fails, the claim is rolled back too,
  so the retry runs the work again.
- Cinema and Payment also make a cheap early check (`isProcessed`) before slow work, so a repeated event never reaches the gateway.

---

## Happy path

### Simple flow

```mermaid
flowchart TD
    A["Client<br/>POST /bookings"] --> B["Booking<br/>save PENDING + outbox row<br/>return 201"]
    B -->|"SeatReservationEvent (E1)"| C["Cinema<br/>CLAIM E1<br/>seats BOOKED + outbox row"]
    C -->|"SeatBookedEvent (E2)"| D["Booking<br/>CLAIM E2<br/>CREATED + outbox row"]
    D -->|"PaymentRequestEvent (E3)"| E["Payment<br/>CLAIM E3<br/>charge SUCCESS + outbox row"]
    E -->|"PaymentCompletedEvent (E4)"| F["Booking<br/>CLAIM E4<br/>CONFIRMED + outbox row"]
    F -->|"BookingCompletedEvent (E5)"| G["Notification<br/>CLAIM E5<br/>log: booking confirmed"]

    classDef booking fill:#dbeafe,stroke:#2563eb,color:#111
    classDef cinema fill:#dcfce7,stroke:#16a34a,color:#111
    classDef payment fill:#ffedd5,stroke:#ea580c,color:#111
    classDef notify fill:#f3e8ff,stroke:#9333ea,color:#111
    classDef client fill:#f3f4f6,stroke:#6b7280,color:#111

    class A client
    class B,D,F booking
    class C cinema
    class E payment
    class G notify
```

E1 to E5 are the `eventId` values. Each event has its own id.

### Sequence diagram

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant B as Booking
    participant K as Kafka
    participant C as Cinema
    participant P as Payment
    participant N as Notification

    Client->>B: POST /api/v1/bookings
    Note over B: one transaction<br/>save booking PENDING<br/>save outbox row (seatReservation)
    B-->>Client: 201 PENDING

    B->>K: SeatReservationEvent (E1)
    K->>C: SeatReservationEvent (E1)
    Note over C: early check: E1 processed already? no<br/>one transaction:<br/>CLAIM E1 in processed_messages<br/>seats become BOOKED<br/>save outbox row (seatBooked)
    C->>K: SeatBookedEvent (E2)

    K->>B: SeatBookedEvent (E2)
    Note over B: one transaction:<br/>CLAIM E2 in processed_messages<br/>PENDING to CREATED, save total<br/>save outbox row (paymentRequest)
    B->>K: PaymentRequestEvent (E3)

    K->>P: PaymentRequestEvent (E3)
    Note over P: early check: E3 processed already? no<br/>1. save payment PENDING<br/>2. gateway charge, no transaction held<br/>3. one transaction: CLAIM E3,<br/>payment SUCCESS, outbox row (paymentCompleted)
    P->>K: PaymentCompletedEvent (E4)

    K->>B: PaymentCompletedEvent (E4)
    Note over B: one transaction:<br/>CLAIM E4 in processed_messages<br/>CREATED to CONFIRMED, save paymentId<br/>save outbox row (bookingCompleted)
    B->>K: BookingCompletedEvent (E5)

    K->>N: BookingCompletedEvent (E5)
    Note over N: one transaction:<br/>CLAIM E5 in processed_messages<br/>log: NOTIFY booking confirmed
```

### What each service claims

After one happy-path booking, the `processed_messages` tables hold:

- **Cinema:** E1 (`SeatReservationEvent`)
- **Booking:** E2 (`SeatBookedEvent`) and E4 (`PaymentCompletedEvent`)
- **Payment:** E3 (`PaymentRequestEvent`)
- **Notification:** E5 (`BookingCompletedEvent`)

Booking has no row for E1, E3 or E5, because it only claims the events it **receives**, not the ones it sends.

### What to notice

- A claim and its work always share **one transaction**, so there is no state where the work is done but the claim is missing, or the reverse.
- Booking and Notification claim at the start of the work. Cinema and Payment also check early, before a seat update or a gateway call.
- The primary key on `message_id` is the last defence. Even if two threads race, only one insert can win.
- Final data is the same as before: booking `CONFIRMED`, seats `BOOKED`, payment `SUCCESS`.

### When the same event arrives again

For example, E3 (`PaymentRequestEvent`) is delivered a second time:

- Payment's early check finds E3 in `processed_messages`, logs `already processed, skipped`, and stops.
- No second payment row is created, and the gateway is never called.
- The same applies to every other consumer: a repeated event is skipped, and its work is not done again.
