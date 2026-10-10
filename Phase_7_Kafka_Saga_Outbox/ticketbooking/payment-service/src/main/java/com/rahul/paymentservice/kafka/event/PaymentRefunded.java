package com.rahul.paymentservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefunded extends BaseEvent {
    private Long bookingId;
    private Long paymentId;
    private Long refundId;
    private BigDecimal amount;
}