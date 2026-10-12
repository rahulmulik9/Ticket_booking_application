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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// These methods only write an outbox row. They must be called inside a transaction.
// OutboxPublisher sends the rows to Kafka later.
@Component
@RequiredArgsConstructor
public class BookingEventRecorder {

    // the same tokens the consumers use in spring.json.type.mapping
    private static final String SEAT_RESERVATION = "seatReservation";
    private static final String SEAT_RELEASE = "seatRelease";
    private static final String PAYMENT_REQUEST = "paymentRequest";
    private static final String REFUND_REQUEST = "refundRequest";
    private static final String BOOKING_COMPLETED = "bookingCompleted";
    private static final String BOOKING_FAILED = "bookingFailed";
    private static final String BOOKING_CANCELLED = "bookingCancelled";

    private final OutboxWriter outboxWriter;

    public void recordSeatReservation(Booking booking) {
        write(booking.getId(), SEAT_RESERVATION,
                new SeatReservationEvent(booking.getId(), booking.getShowId(), booking.seatIds()));
    }

    public void recordPaymentRequest(Long bookingId, BigDecimal amount) {
        write(bookingId, PAYMENT_REQUEST, new PaymentRequestEvent(bookingId, amount));
    }

    public void recordSeatRelease(Booking booking, String reason) {
        write(booking.getId(), SEAT_RELEASE,
                new SeatReleaseEvent(booking.getId(), booking.getShowId(), booking.seatIds(), reason));
    }

    public void recordRefundRequest(Booking booking) {
        write(booking.getId(), REFUND_REQUEST,
                new RefundRequestEvent(booking.getId(), booking.getPaymentId(), booking.getTotalAmount()));
    }

    public void recordBookingCompleted(Booking booking) {
        write(booking.getId(), BOOKING_COMPLETED, new BookingCompletedEvent(
                booking.getId(), booking.getUserId(), booking.getPaymentId(), booking.getTotalAmount()));
    }

    public void recordBookingFailed(Booking booking, String reason) {
        write(booking.getId(), BOOKING_FAILED, new BookingFailedEvent(booking.getId(), booking.getUserId(), reason));
    }

    public void recordBookingCancelled(Booking booking) {
        write(booking.getId(), BOOKING_CANCELLED, new BookingCancelledEvent(booking.getId(), booking.getUserId()));
    }

    private void write(Long bookingId, String eventType, Object event) {
        // key = booking id, so all events of one booking stay in order inside one partition
        outboxWriter.write(KafkaTopicConfig.BOOKING_EVENTS, String.valueOf(bookingId), eventType, event);
    }
}