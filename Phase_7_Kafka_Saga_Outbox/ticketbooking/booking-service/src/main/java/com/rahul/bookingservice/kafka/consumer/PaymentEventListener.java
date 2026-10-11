package com.rahul.bookingservice.kafka.consumer;

import com.rahul.bookingservice.kafka.event.payment.PaymentCompletedEvent;
import com.rahul.bookingservice.kafka.event.payment.PaymentFailedEvent;
import com.rahul.bookingservice.kafka.event.payment.RefundCompletedEvent;
import com.rahul.bookingservice.kafka.event.payment.RefundFailedEvent;
import com.rahul.bookingservice.service.BookingTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "payment-events")
public class PaymentEventListener {

    private final BookingTransactionService bookingTransactionService;

    @KafkaHandler
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        boolean updated = bookingTransactionService.markConfirmed(event.getBookingId(), event.getPaymentId());
        log.info("Booking {} confirmed with payment {}, updated: {}", event.getBookingId(), event.getPaymentId(), updated);
    }

    @KafkaHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        boolean updated = bookingTransactionService.markPaymentFailed(event.getBookingId(), event.getReason());
        log.info("Payment failed for booking {}: {}, updated: {}", event.getBookingId(), event.getReason(), updated);
    }

    @KafkaHandler
    public void onRefundCompleted(RefundCompletedEvent event) {
        log.info("Refund {} of payment {} completed for booking {}, amount {}",
                event.getRefundId(), event.getPaymentId(), event.getBookingId(), event.getAmount());
    }

    @KafkaHandler
    public void onRefundFailed(RefundFailedEvent event) {
        log.error("Refund failed for booking {}, payment {}: {}. Retry with POST /api/v1/payments/{}/refund",
                event.getBookingId(), event.getPaymentId(), event.getReason(), event.getPaymentId());
    }

    // Safety net for any event type that has no handler.
    @KafkaHandler(isDefault = true)
    public void onNotHandledYet(Object event) {
        log.warn("No handler for {}", event.getClass().getSimpleName());
    }
}