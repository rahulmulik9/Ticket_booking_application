package com.rahul.cinemaservice.kafka.consumer;

import com.rahul.cinemaservice.kafka.event.SeatReleaseEvent;
import com.rahul.cinemaservice.kafka.event.SeatReservationEvent;
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

    @KafkaHandler
    public void onSeatReservation(SeatReservationEvent event) {
        log.info("Received SeatReservationEvent: booking {}, show {}, seats {}",
                event.getBookingId(), event.getShowId(), event.getSeatIds());
        seatReservationService.reserve(event);
    }

    // The undo step. releaseSeats skips seats that are already AVAILABLE, so a repeated message is harmless.
    @KafkaHandler
    public void onSeatRelease(SeatReleaseEvent event) {
        log.info("Received SeatReleaseEvent: booking {}, show {}, seats {}, reason {}",
                event.getBookingId(), event.getShowId(), event.getSeatIds(), event.getReason());
        seatService.releaseSeats(event.getShowId(), event.getSeatIds());
    }

    // Events for other services land here, mapped to IgnoredEvent.
    @KafkaHandler(isDefault = true)
    public void onIgnored(Object event) {
        log.info("Ignored event {}", event.getClass().getSimpleName());
    }
}