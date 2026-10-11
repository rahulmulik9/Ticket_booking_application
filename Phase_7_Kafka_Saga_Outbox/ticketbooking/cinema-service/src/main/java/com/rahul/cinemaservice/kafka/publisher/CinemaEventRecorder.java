package com.rahul.cinemaservice.kafka.publisher;

import com.rahul.cinemaservice.kafka.config.KafkaTopicConfig;
import com.rahul.cinemaservice.kafka.event.SeatBookedEvent;
import com.rahul.cinemaservice.kafka.event.SeatBookingFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// These methods only write an outbox row. They must be called inside a transaction.
// OutboxPublisher sends the rows to Kafka later.
@Component
@RequiredArgsConstructor
public class CinemaEventRecorder {

    // the same tokens the consumers use in spring.json.type.mapping
    private static final String SEAT_BOOKED = "seatBooked";
    private static final String SEAT_BOOKING_FAILED = "seatBookingFailed";

    private final OutboxWriter outboxWriter;

    public void recordSeatBooked(SeatBookedEvent event) {
        write(event.getBookingId(), SEAT_BOOKED, event);
    }

    public void recordSeatBookingFailed(SeatBookingFailedEvent event) {
        write(event.getBookingId(), SEAT_BOOKING_FAILED, event);
    }

    private void write(Long bookingId, String eventType, Object event) {
        outboxWriter.write(KafkaTopicConfig.CINEMA_EVENTS, String.valueOf(bookingId), eventType, event);
    }
}