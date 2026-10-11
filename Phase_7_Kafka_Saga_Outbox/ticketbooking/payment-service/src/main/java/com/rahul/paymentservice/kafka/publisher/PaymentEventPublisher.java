package com.rahul.paymentservice.kafka.publisher;

import com.rahul.paymentservice.kafka.config.KafkaTopicConfig;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.event.RefundCompletedEvent;
import com.rahul.paymentservice.kafka.event.RefundFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        send(event.getBookingId(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        send(event.getBookingId(), event);
    }

    public void publishRefundCompleted(RefundCompletedEvent event) {
        send(event.getBookingId(), event);
    }

    public void publishRefundFailed(RefundFailedEvent event) {
        send(event.getBookingId(), event);
    }
    private void send(Long bookingId, Object event) {
        String name = event.getClass().getSimpleName();
        try {
            kafkaTemplate.send(KafkaTopicConfig.PAYMENT_EVENTS, String.valueOf(bookingId), event)
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