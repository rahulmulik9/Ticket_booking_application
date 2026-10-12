package com.rahul.paymentservice.kafka.consumer;

import com.rahul.paymentservice.kafka.event.PaymentRequestEvent;
import com.rahul.paymentservice.kafka.event.RefundRequestEvent;
import com.rahul.paymentservice.service.PaymentProcessingService;
import com.rahul.paymentservice.service.RefundProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "booking-events")
public class BookingEventListener {

    private final PaymentProcessingService paymentProcessingService;
    private final RefundProcessingService refundProcessingService;

    @KafkaHandler
    public void onPaymentRequest(PaymentRequestEvent event) {
        log.info("Received PaymentRequestEvent: booking {}, amount {}", event.getBookingId(), event.getAmount());
        paymentProcessingService.process(event);
    }

    @KafkaHandler
    public void onRefundRequest(RefundRequestEvent event) {
        log.info("Received RefundRequestEvent: booking {}, payment {}, amount {}",
                event.getBookingId(), event.getPaymentId(), event.getAmount());
        refundProcessingService.process(event);
    }
    // Events for other services land here, mapped to IgnoredEvent. RefundRequestEvent also lands
    // here until Step 5 adds its handler.
    @KafkaHandler(isDefault = true)
    public void onIgnored(Object event) {
        log.info("Ignored event {}", event.getClass().getSimpleName());
    }
}