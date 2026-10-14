package com.rahul.notificationservice.kafka.consumer;

import com.rahul.notificationservice.kafka.event.BookingCancelledEvent;
import com.rahul.notificationservice.kafka.event.BookingCompletedEvent;
import com.rahul.notificationservice.kafka.event.BookingFailedEvent;
import com.rahul.notificationservice.service.IdempotencyService;
import com.rahul.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// For now each handler only logs. Phase 8 replaces the log with an EmailSender.
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "booking-events")
public class BookingEventListener {

    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;

    @KafkaHandler
    public void onBookingCompleted(BookingCompletedEvent event) {
        idempotencyService.runOnce(event.getEventId(), () -> notificationService.bookingConfirmed(event));
    }

    @KafkaHandler
    public void onBookingFailed(BookingFailedEvent event) {
        idempotencyService.runOnce(event.getEventId(), () -> notificationService.bookingFailed(event));
    }

    @KafkaHandler
    public void onBookingCancelled(BookingCancelledEvent event) {
        idempotencyService.runOnce(event.getEventId(), () -> notificationService.bookingCancelled(event));
    }

    // Events for other services land here, mapped to IgnoredEvent.
    @KafkaHandler(isDefault = true)
    public void onIgnored(Object event) {
        log.info("Ignored event {}", event.getClass().getSimpleName());
    }
}