package com.rahul.bookingservice.kafka.event.notification;

import com.rahul.bookingservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent extends BaseEvent {
    private Long bookingId;
    private Long userId;
}