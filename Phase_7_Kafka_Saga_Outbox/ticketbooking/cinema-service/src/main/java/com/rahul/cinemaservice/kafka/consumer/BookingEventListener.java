package com.rahul.cinemaservice.kafka.consumer;

import com.rahul.cinemaservice.kafka.event.BookingRequested;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookingEventListener {

    @KafkaListener(topics = "booking-events")
    public void onBookingRequested(BookingRequested event) {
        log.info("Received BookingRequested: booking {}, show {}, seats {}",
                event.getBookingId(), event.getShowId(), event.getSeatIds());
    }
}