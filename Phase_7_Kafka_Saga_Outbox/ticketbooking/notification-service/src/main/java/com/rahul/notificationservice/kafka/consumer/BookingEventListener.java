package com.rahul.notificationservice.kafka.consumer;

import com.rahul.notificationservice.kafka.event.BookingCancelledEvent;
import com.rahul.notificationservice.kafka.event.BookingCompletedEvent;
import com.rahul.notificationservice.kafka.event.BookingFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// For now each handler only logs. Phase 8 replaces the log with an EmailSender.
@Component
@Slf4j
@KafkaListener(topics = "booking-events")
public class BookingEventListener {

    @KafkaHandler
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.info("NOTIFY user {}: booking {} is confirmed, paid {} (payment {})",
                event.getUserId(), event.getBookingId(), event.getTotalAmount(), event.getPaymentId());
    }

    @KafkaHandler
    public void onBookingFailed(BookingFailedEvent event) {
        log.info("NOTIFY user {}: booking {} failed, reason: {}",
                event.getUserId(), event.getBookingId(), event.getReason());
    }

    @KafkaHandler
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("NOTIFY user {}: booking {} is cancelled, the refund is on its way",
                event.getUserId(), event.getBookingId());
    }

    // Events for other services land here, mapped to IgnoredEvent.
    @KafkaHandler(isDefault = true)
    public void onIgnored(Object event) {
        log.info("Ignored event {}", event.getClass().getSimpleName());
    }
}