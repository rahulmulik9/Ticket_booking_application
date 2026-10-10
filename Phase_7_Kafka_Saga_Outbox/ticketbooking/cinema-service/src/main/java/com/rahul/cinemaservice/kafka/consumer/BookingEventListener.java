package com.rahul.cinemaservice.kafka.consumer;

import com.rahul.cinemaservice.kafka.event.SeatReservationEvent;
import com.rahul.cinemaservice.service.SeatReservationService;
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

    private final SeatReservationService seatReservationService;

    @KafkaHandler
    public void onSeatReservation(SeatReservationEvent event) {
        log.info("Received SeatReservationEvent: booking {}, show {}, seats {}",
                event.getBookingId(), event.getShowId(), event.getSeatIds());
        seatReservationService.reserve(event);
    }

    // Events for other services land here, mapped to IgnoredEvent. SeatReleaseEvent also lands
    // here until Step 4 adds its handler.
    @KafkaHandler(isDefault = true)
    public void onIgnored(Object event) {
        log.info("Ignored event {}", event.getClass().getSimpleName());
    }
}