package com.rahul.bookingservice.kafka.publisher;

import com.rahul.bookingservice.kafka.config.KafkaTopicConfig;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.kafka.event.BookingRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    // Object, because one template will send all booking event types
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingRequested(Booking booking) {
        BookingRequested event = new BookingRequested(
                booking.getId(), booking.getUserId(), booking.getShowId(), booking.seatIds());
        send(booking.getId(), event);
    }

    private void send(Long bookingId, Object event) {
        try {
            // key = booking id, so all events of one booking stay in order inside one partition
            kafkaTemplate.send(KafkaTopicConfig.BOOKING_EVENTS, String.valueOf(bookingId), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Could not publish {} for booking {}", event.getClass().getSimpleName(), bookingId, ex);
                        } else {
                            log.info("Published {} for booking {}, partition {}, offset {}",
                                    event.getClass().getSimpleName(), bookingId,
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            // A failed publish must not break the booking for now. Step 6 shows why this is a problem.
            log.error("Could not publish {} for booking {}", event.getClass().getSimpleName(), bookingId, ex);
        }
    }
}