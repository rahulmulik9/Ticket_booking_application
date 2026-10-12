package com.rahul.cinemaservice.kafka.consumer;

import com.rahul.cinemaservice.kafka.event.SeatReleaseEvent;
import com.rahul.cinemaservice.kafka.event.SeatReservationEvent;
import com.rahul.cinemaservice.service.IdempotencyService;
import com.rahul.cinemaservice.service.SeatReservationService;
import com.rahul.cinemaservice.service.SeatService;
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
    private final SeatService seatService;
    private final IdempotencyService idempotencyService;

    @KafkaHandler
    public void onSeatReservation(SeatReservationEvent event) {
        log.info("Received SeatReservationEvent: booking {}, show {}, seats {}",
                event.getBookingId(), event.getShowId(), event.getSeatIds());
        seatReservationService.reserve(event);
    }

    // Releasing is safe to repeat only until someone else books the seats again.
    // A late duplicate would then free their seats, so it must be skipped.
    @KafkaHandler
    public void onSeatRelease(SeatReleaseEvent event) {
        log.info("Received SeatReleaseEvent: booking {}, show {}, seats {}, reason {}",
                event.getBookingId(), event.getShowId(), event.getSeatIds(), event.getReason());
        idempotencyService.runOnce(event.getEventId(),
                () -> seatService.releaseSeats(event.getShowId(), event.getSeatIds()));
    }

    // Events for other services land here, mapped to IgnoredEvent.
    @KafkaHandler(isDefault = true)
    public void onIgnored(Object event) {
        log.info("Ignored event {}", event.getClass().getSimpleName());
    }
}