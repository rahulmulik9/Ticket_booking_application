package com.rahul.paymentservice.kafka.event;

import com.rahul.paymentservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent extends BaseEvent {
    private Long bookingId;
    private Long paymentId;
    private String reason;
}