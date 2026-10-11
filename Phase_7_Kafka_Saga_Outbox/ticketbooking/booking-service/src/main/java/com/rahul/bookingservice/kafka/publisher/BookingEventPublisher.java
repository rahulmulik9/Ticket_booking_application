package com.rahul.bookingservice.kafka.publisher;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.kafka.config.KafkaTopicConfig;
import com.rahul.bookingservice.kafka.event.cinema.SeatReleaseEvent;
import com.rahul.bookingservice.kafka.event.cinema.SeatReservationEvent;
import com.rahul.bookingservice.kafka.event.notification.BookingCancelledEvent;
import com.rahul.bookingservice.kafka.event.notification.BookingCompletedEvent;
import com.rahul.bookingservice.kafka.event.notification.BookingFailedEvent;
import com.rahul.bookingservice.kafka.event.payment.PaymentRequestEvent;
import com.rahul.bookingservice.kafka.event.payment.RefundRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSeatReservation(Booking booking) {
        send(booking.getId(), new SeatReservationEvent(booking.getId(), booking.getShowId(), booking.seatIds()));
    }

    public void publishBookingFailed(Booking booking, String reason) {
        send(booking.getId(), new BookingFailedEvent(booking.getId(), booking.getUserId(), reason));
    }

    public void publishPaymentRequest(Long bookingId, BigDecimal amount) {
        send(bookingId, new PaymentRequestEvent(bookingId, amount));
    }

    public void publishBookingCompleted(Booking booking) {
        send(booking.getId(), new BookingCompletedEvent(booking.getId(), booking.getUserId(), booking.getPaymentId(), booking.getTotalAmount()));
    }

    public void publishSeatRelease(Booking booking, String reason) {
        send(booking.getId(), new SeatReleaseEvent(booking.getId(), booking.getShowId(), booking.seatIds(), reason));
    }

    public void publishRefundRequest(Booking booking) {
        send(booking.getId(), new RefundRequestEvent(booking.getId(), booking.getPaymentId(), booking.getTotalAmount()));
    }

    public void publishBookingCancelled(Booking booking) {
        send(booking.getId(), new BookingCancelledEvent(booking.getId(), booking.getUserId()));
    }

    private void send(Long bookingId, Object event) {
        String name = event.getClass().getSimpleName();
        try {
            // key = booking id, so all events of one booking stay in order inside one partition
            kafkaTemplate.send(KafkaTopicConfig.BOOKING_EVENTS, String.valueOf(bookingId), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Could not publish {} for booking {}", name, bookingId, ex);
                        } else {
                            log.info("Published {} for booking {}, partition {}, offset {}", name, bookingId,
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            // Still not safe: a failed publish is lost. Step 6 shows it, Step 7 fixes it.
            log.error("Could not publish {} for booking {}", name, bookingId, ex);
        }
    }
}