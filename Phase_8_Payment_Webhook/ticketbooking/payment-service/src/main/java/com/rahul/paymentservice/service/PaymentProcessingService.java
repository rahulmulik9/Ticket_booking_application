package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.gateway.GatewayResult;
import com.rahul.paymentservice.gateway.PaymentGateway;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.event.PaymentRequestEvent;
import com.rahul.paymentservice.kafka.publisher.PaymentEventRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// Kafka entry point. Called by BookingEventListener for each PaymentRequestEvent.
// Duplicate protection: event id (processed_messages). Output: outbox events, no HTTP response.


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingService {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final PaymentEventRecorder paymentEventRecorder;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;

    public void process(PaymentRequestEvent event) {
        // check first, so a repeated message never reaches the gateway
        if (idempotencyService.isProcessed(event.getEventId())) {
            log.info("PaymentRequestEvent {} already processed, skipped", event.getEventId());
            return;
        }

        Payment pending = paymentService.createPending(event.getBookingId(), event.getAmount());   // commits
        GatewayResult result = paymentGateway.charge(pending.getBookingId(), pending.getAmount()); // no transaction here

        // one transaction: the claim, the final status and the event row commit together
        transactionTemplate.executeWithoutResult(status -> {
            if (!idempotencyService.claim(event.getEventId())) {
                log.warn("PaymentRequestEvent {} was processed by another thread meanwhile", event.getEventId());
                return;
            }
            Payment finished = paymentService.finish(pending, result);
            if (finished.getStatus() == PaymentStatus.SUCCESS) {
                paymentEventRecorder.recordPaymentCompleted(
                        new PaymentCompletedEvent(event.getBookingId(), finished.getId(), finished.getAmount()));
            } else {
                paymentEventRecorder.recordPaymentFailed(
                        new PaymentFailedEvent(event.getBookingId(), finished.getId(), "Payment declined by the gateway"));
            }
        });
    }
}