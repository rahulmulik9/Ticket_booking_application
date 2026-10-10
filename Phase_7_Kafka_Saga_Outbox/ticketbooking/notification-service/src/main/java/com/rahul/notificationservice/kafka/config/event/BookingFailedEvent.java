package com.rahul.notificationservice.kafka.config.event;

import com.rahul.bookingservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingFailedEvent extends BaseEvent {
    private Long bookingId;
    private Long userId;
    private String reason;   // SEATS_UNAVAILABLE or PAYMENT_FAILED
}