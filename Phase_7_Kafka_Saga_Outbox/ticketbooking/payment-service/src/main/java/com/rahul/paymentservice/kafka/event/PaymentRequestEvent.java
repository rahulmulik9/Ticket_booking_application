package com.rahul.paymentservice.kafka.event;

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
public class PaymentRequestEvent extends BaseEvent {
    private Long bookingId;
    private BigDecimal amount;
}