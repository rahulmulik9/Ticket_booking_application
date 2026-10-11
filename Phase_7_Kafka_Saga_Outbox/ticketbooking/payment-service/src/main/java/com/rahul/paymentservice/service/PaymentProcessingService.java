package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.event.PaymentRequestEvent;
import com.rahul.paymentservice.kafka.publisher.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentService paymentService;
    private final PaymentEventPublisher paymentEventPublisher;

    public void process(PaymentRequestEvent event) {
        Payment payment = paymentService.charge(event.getBookingId(), event.getAmount());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            paymentEventPublisher.publishPaymentCompleted(
                    new PaymentCompletedEvent(event.getBookingId(), payment.getId(), payment.getAmount()));
        } else {
            paymentEventPublisher.publishPaymentFailed(
                    new PaymentFailedEvent(event.getBookingId(), payment.getId(), "Payment declined by the gateway"));
        }
    }
}