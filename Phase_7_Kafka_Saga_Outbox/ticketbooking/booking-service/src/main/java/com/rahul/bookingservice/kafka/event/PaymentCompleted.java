package com.rahul.bookingservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompleted extends BaseEvent {
    private Long bookingId;
    private Long paymentId;
    private BigDecimal amount;
}