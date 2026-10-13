# Payment Flow: Kafka and REST

How a payment enters the Payment service, how duplicates are stopped, and how the result comes back. Covers Phase 8 (Steps 1 and 2).

---

## 1. The three entry points

- **REST:** a client calls `POST /api/v1/payments`. Handled by `PaymentController` and `PaymentService`.
- **Kafka:** Booking sends a `PaymentRequestEvent`. Handled by `BookingEventListener` and `PaymentProcessingService`. This is the main booking flow.
- **Webhook:** the gateway calls `POST /api/v1/webhooks/payment` with the final result. Handled by `WebhookController` and `PaymentWebhookService`.

In async mode, REST and Kafka both end with a `PENDING` payment, and the webhook finishes it.

---

## 2. Flow 1: Payment through REST

```
Client
  |  POST /api/v1/payments
  |  header: Idempotency-Key: pay-1
  v
PaymentController
  |  user id from JWT, key from header
  v
PaymentService.createPayment            (no transaction)
  |  1. validate key (blank or > 64 chars -> 400)
  |  2. hash = SHA-256(bookingId + amount)
  v
PaymentTransactionService               (ONE short transaction)
  |  3. INSERT (userId, key, hash) ON CONFLICT DO NOTHING
  |
  +-- inserted (1) = NEW KEY
  |       save PENDING payment
  |       link key -> payment id
  |       commit
  |       returns PaymentClaim(payment, replayed = false)
  |
  +-- not inserted (0) = REPEAT KEY
          same hash?   NO  -> throw -> 422
                       YES -> load the old payment
                              returns PaymentClaim(payment, replayed = true)
  v
PaymentService
  |  replayed = true  -> return the saved payment (NO gateway call, NO second charge)
  |  replayed = false -> call gateway (outside any transaction)
  v
Gateway
  |  async mode: answers PENDING -> we return 201 with status PENDING
  |  sync mode:  answers SUCCESS or FAILED -> finish() saves it
  v
Later (async mode): gateway calls the webhook, which finishes the payment
```

---

## 3. Flow 2: Payment through Kafka

```
Booking service
  |  writes PaymentRequestEvent (eventId = UUID) to its outbox
  v
Kafka topic: booking-events
  v
BookingEventListener.onPaymentRequest
  v
PaymentProcessingService.process        (no transaction around the whole method)
  |  1. isProcessed(eventId)?
  |        YES -> skip, stop           (early check, protects the gateway)
  |        NO  -> continue
  |  2. createPending                  (commits PENDING payment)
  |  3. gateway.charge                 (no transaction, slow call)
  v
Gateway answer
  |
  +-- PENDING (async mode)
  |      short transaction: claim(eventId)
  |      stop. The webhook finishes the payment later.
  |
  +-- SUCCESS or FAILED (sync mode)
         ONE transaction:
           claim(eventId) fails -> skip
           claim ok -> finish() + write PaymentCompleted or PaymentFailed to outbox
```

---

## 4. Flow 3: The webhook (where async payments finish)

```
Gateway --POST /api/v1/webhooks/payment + X-Signature--> WebhookController
  |  1. signature valid?   NO -> 401
  v
PaymentWebhookService                   (ONE transaction)
  |  2. claim(eventId) fails -> duplicate, ignore, reply 200
  |  3. lock payment row (SELECT ... FOR UPDATE)
  |  4. payment not PENDING any more -> ignore, reply 200
  |  5. set SUCCESS or FAILED + write outbox event
  v
Outbox publisher -> Kafka -> Booking confirms or compensates
```

---

## 5. Checking strategy

### REST

- **Id created by:** the client (`Idempotency-Key` header).
- **Checked against:** the `idempotency_keys` table, scoped by user.
- **Extra check:** the request hash. The same key with a different body gets `422`.
- **When:** the claim comes first, in the same transaction as saving the payment.
- **Last defense:** the unique constraint `(user_id, key_value)`.

### Kafka

- **Id created by:** Booking (`eventId`).
- **Checked against:** the `processed_messages` table.
- **When:** an early check (`isProcessed`) before the gateway, and the real claim at the end.
- **Weak spot:** a crash after the gateway call but before the claim can charge twice (known gap, see section 7).

### Webhook

- **Id created by:** the gateway (`eventId`).
- **Checked against:** the signature first, then the `processed_messages` table.
- **Extra check:** the payment must still be `PENDING`, under a row lock.
- **When:** the claim comes first, in the same transaction as the update.

---

## 6. Class responsibilities

- **`PaymentController`:** REST entry point. Reads the user and the `Idempotency-Key` header.
- **`PaymentService`:** REST flow, plus `createPending` and `finish`, which the Kafka flow reuses.
- **`PaymentTransactionService`:** REST only. One short transaction: claim the key and save the `PENDING` payment.
- **`PaymentClaim`:** small result box: the payment, and whether it was a repeat.
- **`PaymentProcessingService`:** Kafka entry point. Event id check, gateway call, outbox events.
- **`BookingEventListener`:** receives Kafka messages and delegates.
- **`WebhookController`:** public endpoint. Verifies the signature.
- **`WebhookSignatureService`:** creates and verifies the HMAC signature.
- **`PaymentWebhookService`:** duplicate check, row lock, final status, outbox event.
- **`IdempotencyService`:** `isProcessed` and `claim` on `processed_messages` (Kafka and webhook).
- **`IdempotencyKey` and `IdempotencyKeyRepository`:** the REST key table.
- **`PaymentEventRecorder`:** writes outbox rows for payment events.
- **`PaymentGateway` and `FakePaymentGateway`:** the bank interface and the pretend bank.

---

## 7. Two different "idempotency" ideas

- **`IdempotencyKey` (REST):** protects against the client repeating a request. The client creates the key.
- **`IdempotencyService` (Kafka and webhook):** protects against the system delivering a message twice. The sender creates the event id.

Same goal, different source of duplicates, so two separate mechanisms.

### Known gap in the Kafka flow

Other services claim the event id and do the work in one transaction (`runOnce`). Payment cannot, because the gateway call is slow and must stay outside a transaction. So the claim happens at the end, and this window exists:

1. Payment charges the gateway.
2. The service crashes before the final claim.
3. Kafka redelivers. The event id was never claimed, so a second payment is created and charged.

Options to close it later (Phase 10): claim first and add a recovery job for stuck `PENDING` payments, or send our `paymentId` to the gateway as its own idempotency reference.

---

## 8. One-line summary

Every entry point asks "have I seen this before?" using an id from the sender. REST uses a client key plus a request hash, Kafka and the webhook use an event id, and the database's unique constraint is always the final guard.
