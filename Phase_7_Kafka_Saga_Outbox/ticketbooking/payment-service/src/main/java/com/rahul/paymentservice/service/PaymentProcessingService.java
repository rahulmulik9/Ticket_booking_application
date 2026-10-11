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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final PaymentEventRecorder paymentEventRecorder;
    private final TransactionTemplate transactionTemplate;

    public void process(PaymentRequestEvent event) {
        Payment pending = paymentService.createPending(event.getBookingId(), event.getAmount());   // commits
        GatewayResult result = paymentGateway.charge(pending.getBookingId(), pending.getAmount()); // no transaction here

        // one transaction: the final status and the event row are saved together
        transactionTemplate.executeWithoutResult(status -> {
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