package com.rahul.cinemaservice.kafka.publisher;

import com.rahul.cinemaservice.kafka.config.KafkaTopicConfig;
import com.rahul.cinemaservice.kafka.event.SeatBookedEvent;
import com.rahul.cinemaservice.kafka.event.SeatBookingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CinemaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSeatBooked(SeatBookedEvent event) {
        send(event.getBookingId(), event);
    }

    public void publishSeatBookingFailed(SeatBookingFailedEvent event) {
        send(event.getBookingId(), event);
    }

    private void send(Long bookingId, Object event) {
        String name = event.getClass().getSimpleName();
        try {
            kafkaTemplate.send(KafkaTopicConfig.CINEMA_EVENTS, String.valueOf(bookingId), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Could not publish {} for booking {}", name, bookingId, ex);
                        } else {
                            log.info("Published {} for booking {}, partition {}, offset {}", name, bookingId,
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("Could not publish {} for booking {}", name, bookingId, ex);
        }
    }
}