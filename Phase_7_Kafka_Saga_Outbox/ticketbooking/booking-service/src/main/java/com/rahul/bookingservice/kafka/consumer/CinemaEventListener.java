package com.rahul.bookingservice.kafka.consumer;

import com.rahul.bookingservice.kafka.event.cinema.SeatBookedEvent;
import com.rahul.bookingservice.kafka.event.cinema.SeatBookingFailedEvent;
import com.rahul.bookingservice.kafka.publisher.BookingEventPublisher;
import com.rahul.bookingservice.service.BookingTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// @KafkaListener on the class and @KafkaHandler on the methods: one topic, several event types.
// Spring picks the method by the type of the event.
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "cinema-events")
public class CinemaEventListener {

    private final BookingTransactionService bookingTransactionService;
    private final BookingEventPublisher bookingEventPublisher;

    @KafkaHandler
    public void onSeatBooked(SeatBookedEvent event) {
        boolean updated = bookingTransactionService.markSeatsBooked(event.getBookingId(), event.getTotalAmount());
        log.info("Seats booked for booking {}, total {}, updated: {}", event.getBookingId(), event.getTotalAmount(), updated);
    }

    @KafkaHandler
    public void onSeatBookingFailed(SeatBookingFailedEvent event) {
        bookingTransactionService.markRejected(event.getBookingId()).ifPresentOrElse(
                booking -> {
                    log.info("Booking {} rejected: {}", booking.getId(), event.getReason());
                    bookingEventPublisher.publishBookingFailed(booking, event.getReason());
                },
                () -> log.info("Booking {} is no longer PENDING, rejection skipped", event.getBookingId()));
    }
}