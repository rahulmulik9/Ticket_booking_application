package com.rahul.paymentservice.service;

import com.rahul.paymentservice.dto.PaymentWebhookRequest;
import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.publisher.PaymentEventRecorder;
import com.rahul.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventRecorder paymentEventRecorder;

    // One transaction: the event claim, the status change and the outbox row commit together.
    // If anything throws, the claim rolls back too, so the gateway's retry is processed normally.
    @Transactional(rollbackFor = Exception.class)
    public void handle(PaymentWebhookRequest request) {
        // 1. Duplicate? Only one call can claim an event id.
        if (!idempotencyService.claim(request.getEventId())) {
            log.info("Webhook {} already processed, ignored", request.getEventId());
            return;
        }

        // 2. Lock the payment row, so two different webhooks cannot both read PENDING.
        Payment payment = paymentRepository.findByIdForUpdate(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id " + request.getPaymentId()));

        // 3. Late or out-of-order? Only a PENDING payment can move. A final state is never changed by a webhook.
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Webhook {} ignored: payment {} is already {}", request.getEventId(), payment.getId(), payment.getStatus());
            return;
        }
        if ("PENDING".equals(request.getStatus())) {
            log.info("Webhook {}: payment {} is still processing at the gateway", request.getEventId(), payment.getId());
            return;
        }

        // 4. Apply the result and tell the other services (the outbox row is sent to Kafka later).
        if ("SUCCESS".equals(request.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentEventRecorder.recordPaymentCompleted(
                    new PaymentCompletedEvent(payment.getBookingId(), payment.getId(), payment.getAmount()));
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentEventRecorder.recordPaymentFailed(
                    new PaymentFailedEvent(payment.getBookingId(), payment.getId(), "Payment declined by the gateway"));
        }
        log.info("Webhook {}: payment {} is now {}", request.getEventId(), payment.getId(), payment.getStatus());
    }
}