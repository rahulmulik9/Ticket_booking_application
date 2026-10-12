# Phase 7: Events and Classes per Service

This is the fully event-driven design. One topic carries several event types, so a listener handles the types it needs and ignores the rest.

> ## Key points
>
> - **Three services need an outbox, because they publish business events: Booking, Cinema and Payment.**
> - **Four services need `ProcessedMessage`, because they all consume: Booking, Cinema, Payment and Notification.**

---

## Booking service

**Publishes** to `booking-events`:

- `BookingRequested`
- `BookingCreated`
- `BookingRejected`
- `BookingConfirmed`
- `BookingPaymentFailed`
- `BookingCancelled`

**Listens to:**

- `SeatsReserved` and `SeatsRejected` (from `cinema-events`)
- `PaymentCompleted` and `PaymentFailed` (from `payment-events`)
- `PaymentRefunded` and `RefundFailed` (from `payment-events`). For now it only records the result, because the refund is Payment's job.

**New classes:**

- `BookingEventPublisher`: sends events (replaced by the outbox in Step 7)
- `CinemaEventListener`: handles the seat results
- `PaymentEventListener`: handles the payment results
- `KafkaTopicConfig`: creates `booking-events`
- `OutboxEvent` entity, repository and `OutboxPublisher` (`@Scheduled`), Step 7
- `ProcessedMessage` entity and repository, Step 8
- Changes to existing code: `BookingStatus` gets `PENDING` and `REJECTED`, `BookingTransactionService` gets methods for each status change, `BookingService.createBooking` shrinks to "save `PENDING` and publish", and a Flyway migration is added

---

## Cinema service

**Publishes** to `cinema-events`:

- `SeatsReserved`
- `SeatsRejected`

**Listens to** (from `booking-events`):

- `BookingRequested`: try to reserve the seats
- `BookingPaymentFailed`: release the seats
- `BookingCancelled`: release the seats

**New classes:**

- `BookingEventListener`
- `CinemaEventPublisher`
- `KafkaTopicConfig`: creates `cinema-events`
- `OutboxEvent`, repository and publisher (Step 7)
- `ProcessedMessage` entity and repository (Step 8)
- A Flyway migration, plus the Kafka dependency
- It reuses your existing `SeatService` reserve and release logic. The `/internal` endpoints are no longer needed for booking.

---

## Payment service

**Publishes** to `payment-events`:

- `PaymentCompleted`
- `PaymentFailed`
- `PaymentRefunded`
- `RefundFailed`

**Listens to** (from `booking-events`):

- `BookingCreated`: charge the money
- `BookingCancelled`: refund the payment

**New classes:**

- `BookingEventListener`
- `PaymentEventPublisher`
- `KafkaTopicConfig`: creates `payment-events`
- `Refund` entity and repository (Step 5)
- `OutboxEvent`, repository and publisher (Step 7)
- `ProcessedMessage` entity and repository (Step 8)
- `PaymentStatus` gets `REFUNDED`
- The new endpoint `POST /api/v1/payments/{id}/refund`, and a Flyway migration

---

## Notification service

**Publishes:** nothing.

**Listens to** (from `booking-events`):

- `BookingRejected`
- `BookingConfirmed`
- `BookingPaymentFailed`
- `BookingCancelled`

**New classes:**

- `BookingEventListener` (it exists from Step 2, and we extend it)
- `ProcessedMessage` entity and repository (Step 8). This also means Notification needs a database. It has none today, and it needs Postgres plus Flyway added.

---

## Summary

- Publish counts: Booking 6, Cinema 2, Payment 4, Notification 0. That adds up to the 12 events.

> - **Three services need an outbox, because they publish business events: Booking, Cinema and Payment.**
> - **Four services need `ProcessedMessage`, because they all consume: Booking, Cinema, Payment and Notification.**
