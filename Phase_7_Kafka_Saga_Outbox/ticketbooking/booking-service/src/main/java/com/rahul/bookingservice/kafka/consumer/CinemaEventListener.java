package com.rahul.bookingservice.kafka.consumer;

import com.rahul.bookingservice.kafka.event.cinema.SeatBookedEvent;
import com.rahul.bookingservice.kafka.event.cinema.SeatBookingFailedEvent;
import com.rahul.bookingservice.service.BookingTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "cinema-events")
public class CinemaEventListener {

    private final BookingTransactionService bookingTransactionService;

    @KafkaHandler
    public void onSeatBooked(SeatBookedEvent event) {
        boolean updated = bookingTransactionService.markSeatsBooked(event.getBookingId(), event.getTotalAmount());
        log.info("Seats booked for booking {}, total {}, updated: {}", event.getBookingId(), event.getTotalAmount(), updated);
    }

    @KafkaHandler
    public void onSeatBookingFailed(SeatBookingFailedEvent event) {
        boolean updated = bookingTransactionService.markRejected(event.getBookingId(), event.getReason());
        log.info("Booking {} rejected: {}, updated: {}", event.getBookingId(), event.getReason(), updated);
    }
}