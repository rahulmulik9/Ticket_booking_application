package com.rahul.bookingservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundFailed extends BaseEvent {
    private Long bookingId;
    private Long paymentId;
    private Long refundId;
    private String reason;
}