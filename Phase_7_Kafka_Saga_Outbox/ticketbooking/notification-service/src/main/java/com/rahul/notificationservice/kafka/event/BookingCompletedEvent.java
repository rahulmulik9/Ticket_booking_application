package com.rahul.notificationservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingCompletedEvent extends BaseEvent {
    private Long bookingId;
    private Long userId;
    private Long paymentId;
    private BigDecimal totalAmount;
}