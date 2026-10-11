# Phase 7: Booking Flow with the Outbox

This file shows the booking flow when every service uses the **outbox pattern**.
It covers the happy path and one failure: the payment is declined.

## How the outbox works

- A service never sends to Kafka in the middle of its work.
- Instead, it saves an **outbox row** in the same database transaction as its own change.
  Both are saved, or neither is.
- A job called **OutboxPublisher** runs every second. It reads the unpublished rows in order,
  sends each one to Kafka, waits for Kafka's confirmation, and then marks the row as published.
- So an event cannot be lost after a database save, even if the service crashes or Kafka is down.

---

## 1. Happy path

### Simple flow

```mermaid
flowchart TD
    A["Client<br/>POST /bookings"] --> B["Booking<br/>save PENDING + outbox row<br/>return 201"]
    B -->|"SeatReservationEvent"| C["Cinema<br/>seats BOOKED + outbox row"]
    C -->|"SeatBookedEvent"| D["Booking<br/>CREATED + outbox row"]
    D -->|"PaymentRequestEvent"| E["Payment<br/>charge SUCCESS + outbox row"]
    E -->|"PaymentCompletedEvent"| F["Booking<br/>CONFIRMED + outbox row"]
    F -->|"BookingCompletedEvent"| G["Notification<br/>log: booking confirmed"]

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

    Note over B,K: OutboxPublisher runs every second
    B->>K: SeatReservationEvent on booking-events
    Note over B: row marked published after Kafka confirms

    K->>C: SeatReservationEvent
    Note over C: one transaction<br/>seats become BOOKED<br/>save outbox row (seatBooked)
    C->>K: SeatBookedEvent on cinema-events

    K->>B: SeatBookedEvent
    Note over B: one transaction<br/>PENDING to CREATED, save total<br/>save outbox row (paymentRequest)
    B->>K: PaymentRequestEvent on booking-events

    K->>P: PaymentRequestEvent
    Note over P: 1. save payment PENDING<br/>2. gateway charge, no transaction held<br/>3. one transaction: payment SUCCESS<br/>and outbox row (paymentCompleted)
    P->>K: PaymentCompletedEvent on payment-events

    K->>B: PaymentCompletedEvent
    Note over B: one transaction<br/>CREATED to CONFIRMED, save paymentId<br/>save outbox row (bookingCompleted)
    B->>K: BookingCompletedEvent on booking-events

    K->>N: BookingCompletedEvent
    Note over N: log: NOTIFY booking confirmed
```

### What to notice

- The client gets **201 with `PENDING`** at once. It reads the final status later with `GET /api/v1/bookings/{id}`.
- Every arrow into Kafka is the OutboxPublisher of the sending service, not the business code.
- Every service saves its own change and its outbox row in **one transaction**.
- Payment keeps the slow gateway call **outside** any transaction, so no database connection is held while it waits.
- Final data: booking `CONFIRMED`, seats `BOOKED`, payment `SUCCESS`.

---

## 2. Payment failed

### Simple flow

```mermaid
flowchart TD
    A["Booking<br/>CREATED, seats held"] -->|"PaymentRequestEvent"| B["Payment<br/>gateway declines<br/>payment FAILED + outbox row"]
    B -->|"PaymentFailedEvent"| C["Booking<br/>PAYMENT_FAILED<br/>+ 2 outbox rows"]
    C -->|"SeatReleaseEvent (undo)"| D["Cinema<br/>seats back to AVAILABLE"]
    C -->|"BookingFailedEvent"| E["Notification<br/>log: booking failed"]

    classDef booking fill:#dbeafe,stroke:#2563eb,color:#111
    classDef cinema fill:#dcfce7,stroke:#16a34a,color:#111
    classDef payment fill:#ffedd5,stroke:#ea580c,color:#111
    classDef notify fill:#f3e8ff,stroke:#9333ea,color:#111

    class A,C booking
    class B payment
    class D cinema
    class E notify
```

### Sequence diagram

```mermaid
sequenceDiagram
    autonumber
    participant B as Booking
    participant K as Kafka
    participant P as Payment
    participant C as Cinema
    participant N as Notification

    Note over B: booking is CREATED<br/>the seats are BOOKED
    B->>K: PaymentRequestEvent on booking-events

    K->>P: PaymentRequestEvent
    Note over P: 1. save payment PENDING<br/>2. gateway DECLINES, no transaction held<br/>3. one transaction: payment FAILED<br/>and outbox row (paymentFailed)
    P->>K: PaymentFailedEvent on payment-events

    K->>B: PaymentFailedEvent
    Note over B: one transaction<br/>CREATED to PAYMENT_FAILED<br/>outbox row (seatRelease)<br/>outbox row (bookingFailed)
    B->>K: SeatReleaseEvent on booking-events
    B->>K: BookingFailedEvent on booking-events

    K->>C: SeatReleaseEvent
    Note over C: seats back to AVAILABLE<br/>this is the undo step

    K->>N: BookingFailedEvent
    Note over N: log: NOTIFY booking failed
```

### What to notice

- There is no shared transaction across services, so the undo is an explicit event: **`SeatReleaseEvent`**.
- Booking sends it, not Payment, because only Booking knows the show and the seat ids.
- Booking saves the status change and **both** outbox rows in one transaction, so it cannot end up
  `PAYMENT_FAILED` with the seats never released.
- Final data: booking `PAYMENT_FAILED`, seats `AVAILABLE`, payment `FAILED`.
