# Phase 8: Payments, Webhooks, and Notifications

Tag: `phase-8-payments`

## Problem

After Phase 7 the system was consistent, but three gaps remained:

- **Double charge:** a double-click or client retry on `POST /bookings` or `POST /payments` could create a second booking or a second charge.
- **Instant-only payment result:** the result came back only in the same call. Real gateways confirm later by calling us.
- **Silent customers:** nobody told the customer that a booking was confirmed, failed or cancelled.

## Fix

- **Idempotency keys (Booking and Payment).**
  - The client sends an `Idempotency-Key` header. The key is claimed with `INSERT ... ON CONFLICT DO NOTHING` on `(user_id, key_value)`, in the same transaction as the booking or payment.
  - A repeat returns the saved booking or payment. The same key with a different body (SHA-256 of the request) gets `422`. A missing header gets `400`.
  - An hourly `@Scheduled` job deletes keys older than 24 hours.
- **Payment webhooks.**
  - The fake gateway now answers `PENDING`, then calls `POST /api/v1/webhooks/payment` after `webhook-delay-ms` (default 2000 ms), signed with HMAC-SHA256 in `X-Signature`.
  - Payment verifies the signature (`401` if wrong), claims the `eventId` (a duplicate is ignored with `200`), locks the payment row (`SELECT ... FOR UPDATE`), and moves only a `PENDING` payment to `SUCCESS` or `FAILED`. The outbox event is written in the same transaction.
  - A final state is never changed by a webhook, so late and out-of-order calls are harmless.
- **Email notifications.**
  - The user's email is copied from the JWT onto the booking, then carried on `BookingCompleted`, `BookingFailed` and `BookingCancelled`.
  - Notification sends through an `EmailSender` interface (Strategy pattern). The fake sender logs the email, and `notification_logs` records `SENT` or `FAILED`.

## Alternatives rejected

- **Polling vs push for the booking result:** the client polls `GET /bookings/{id}` for now. SSE is left for Phase 9, because the push is one-way.
- **Storing `responseBody` JSON in the idempotency key:** we store `resourceId` and load the current record instead. A repeat then shows the latest status, which fits async flows, and avoids Jackson 3 changes.
- **Signing the raw webhook body:** we sign `eventId|paymentId|status` instead. It is simpler, and no raw request is needed. Real providers often sign the raw body.
- **Rethrowing an email failure (retry, then DLT):** rejected. A broken email must not block booking events, so the failure is recorded as `FAILED` and we move on.
- **A new table for webhook duplicates:** rejected. The existing `processed_messages` table is reused.

## Numbers

- Public endpoints: 18 to 19 (`POST /api/v1/webhooks/payment`).
- Entities: 11 to 13 (`IdempotencyKey` in Booking and Payment, `NotificationLog` in Notification).
- New Flyway migrations: Booking `V5` (idempotency keys) and `V6` (`user_email`), Payment `V5` (idempotency keys), Notification `V2` (notification logs).
- Postman collection: 44 requests in 10 folders, covering repeats, duplicate and late webhooks, wrong signatures, a declined payment, and cancel with refund.

## Found and fixed on the way

- **Payment `SecurityConfig` rule order.** `/api/v1/payments/**` was matched before the refund rule, so `hasRole("ADMIN")` never ran. Any logged-in user could refund. The refund rule now comes first.
- **Missing header returned 500.** `MissingRequestHeaderException` fell into the catch-all handler. It now returns `400`.

## Known gaps (for later)

- **Double-charge window in the Kafka payment path.** If Payment crashes after the gateway call but before it claims the event id, Kafka redelivers and a second payment is charged. Options: claim first plus a recovery job, or send our `paymentId` to the gateway as its own idempotency reference. Planned for Phase 10.
- **Stuck `PENDING` payments.** If the webhook never arrives, the payment stays `PENDING`. A real system adds a reconciliation job that asks the gateway for the status.
- **REST payments now publish payment events too**, because the webhook writes them for every payment.
- **`FAILED` notifications are not retried.** A retry job for `FAILED` rows is future work.
- **Email address is stored on bookings and logged by the fake sender.** Fine for development. Mask it with a real provider.
- **`runOnce` in the Payment `IdempotencyService` is unused.** Payment claims the event id by hand, because the gateway call must stay outside a transaction.

## Interview line

"Idempotency keys make client retries safe, and webhooks with signature checks handle async payment results. Duplicates are ignored by event id, and a late call can never overwrite a final state."
