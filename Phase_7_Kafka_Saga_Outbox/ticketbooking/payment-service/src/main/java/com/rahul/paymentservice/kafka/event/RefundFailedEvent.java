package com.rahul.paymentservice.kafka.event;

import com.rahul.bookingservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundFailedEvent extends BaseEvent {
    private Long bookingId;
    private Long paymentId;
    private Long refundId;
    private String reason;
}