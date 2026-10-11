package com.rahul.paymentservice.kafka.publisher;

import com.rahul.paymentservice.kafka.config.KafkaTopicConfig;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.event.RefundCompletedEvent;
import com.rahul.paymentservice.kafka.event.RefundFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// These methods only write an outbox row. They must be called inside a transaction.
// OutboxPublisher sends the rows to Kafka later.
@Component
@RequiredArgsConstructor
public class PaymentEventRecorder {

    // the same tokens the consumers use in spring.json.type.mapping
    private static final String PAYMENT_COMPLETED = "paymentCompleted";
    private static final String PAYMENT_FAILED = "paymentFailed";
    private static final String REFUND_COMPLETED = "refundCompleted";
    private static final String REFUND_FAILED = "refundFailed";

    private final OutboxWriter outboxWriter;

    public void recordPaymentCompleted(PaymentCompletedEvent event) {
        write(event.getBookingId(), PAYMENT_COMPLETED, event);
    }

    public void recordPaymentFailed(PaymentFailedEvent event) {
        write(event.getBookingId(), PAYMENT_FAILED, event);
    }

    public void recordRefundCompleted(RefundCompletedEvent event) {
        write(event.getBookingId(), REFUND_COMPLETED, event);
    }

    public void recordRefundFailed(RefundFailedEvent event) {
        write(event.getBookingId(), REFUND_FAILED, event);
    }

    private void write(Long bookingId, String eventType, Object event) {
        outboxWriter.write(KafkaTopicConfig.PAYMENT_EVENTS, String.valueOf(bookingId), eventType, event);
    }
}