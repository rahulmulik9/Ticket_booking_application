package com.rahul.bookingservice.kafka.consumer;

import com.rahul.bookingservice.kafka.event.payment.PaymentCompletedEvent;
import com.rahul.bookingservice.kafka.event.payment.PaymentFailedEvent;
import com.rahul.bookingservice.kafka.publisher.BookingEventPublisher;
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
    private final BookingEventPublisher bookingEventPublisher;

    @KafkaHandler
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        bookingTransactionService.markConfirmed(event.getBookingId(), event.getPaymentId()).ifPresentOrElse(
                booking -> {
                    log.info("Booking {} confirmed with payment {}", booking.getId(), event.getPaymentId());
                    bookingEventPublisher.publishBookingCompleted(booking);
                },
                () -> log.info("Booking {} is no longer CREATED, confirmation skipped", event.getBookingId()));
    }

    @KafkaHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        bookingTransactionService.markPaymentFailed(event.getBookingId()).ifPresentOrElse(
                booking -> {
                    log.info("Payment failed for booking {}: {}", booking.getId(), event.getReason());
                    bookingEventPublisher.publishSeatRelease(booking, "PAYMENT_FAILED");   // undo: free the seats
                    bookingEventPublisher.publishBookingFailed(booking, event.getReason());   // tell the user
                },
                () -> log.info("Booking {} is no longer CREATED, payment failure skipped", event.getBookingId()));
    }


    @KafkaHandler(isDefault = true)
    public void onNotHandledYet(Object event) {
        log.warn("No handler yet for {}", event.getClass().getSimpleName());
    }
}