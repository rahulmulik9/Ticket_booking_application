package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Refund;
import com.rahul.paymentservice.enums.RefundStatus;
import com.rahul.paymentservice.exception.RefundNotAllowedException;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.gateway.GatewayResult;
import com.rahul.paymentservice.kafka.event.RefundCompletedEvent;
import com.rahul.paymentservice.kafka.event.RefundFailedEvent;
import com.rahul.paymentservice.kafka.event.RefundRequestEvent;
import com.rahul.paymentservice.kafka.publisher.PaymentEventRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundProcessingService {

    private final RefundService refundService;
    private final PaymentEventRecorder paymentEventRecorder;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;

    public void process(RefundRequestEvent event) {
        // check first. Without it, a repeat would find the payment already REFUNDED and send a false RefundFailedEvent.
        if (idempotencyService.isProcessed(event.getEventId())) {
            log.info("RefundRequestEvent {} already processed, skipped", event.getEventId());
            return;
        }

        try {
            Refund pending = refundService.startRefund(event.getPaymentId());   // commits
            GatewayResult result = refundService.callGateway(pending);          // no transaction here

            // one transaction: the claim, the refund, the payment status and the event row commit together
            transactionTemplate.executeWithoutResult(status -> {
                if (!idempotencyService.claim(event.getEventId())) {
                    return;
                }
                Refund finished = refundService.finishRefund(pending, result);
                if (finished.getStatus() == RefundStatus.DONE) {
                    paymentEventRecorder.recordRefundCompleted(new RefundCompletedEvent(
                            event.getBookingId(), event.getPaymentId(), finished.getId(), finished.getAmount()));
                } else {
                    paymentEventRecorder.recordRefundFailed(new RefundFailedEvent(
                            event.getBookingId(), event.getPaymentId(), finished.getId(), "Refund declined by the gateway"));
                }
            });
        } catch (ResourceNotFoundException | RefundNotAllowedException ex) {
            log.warn("Refund for booking {} not possible: {}", event.getBookingId(), ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> {
                if (!idempotencyService.claim(event.getEventId())) {
                    return;
                }
                paymentEventRecorder.recordRefundFailed(
                        new RefundFailedEvent(event.getBookingId(), event.getPaymentId(), null, ex.getMessage()));
            });
        }
    }
}