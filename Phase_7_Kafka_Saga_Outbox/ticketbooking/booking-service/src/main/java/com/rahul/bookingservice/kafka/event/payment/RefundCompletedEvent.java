package com.rahul.bookingservice.kafka.event.payment;

import com.rahul.bookingservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundCompletedEvent extends BaseEvent {
    private Long bookingId;
    private Long paymentId;
    private Long refundId;
    private BigDecimal amount;
}