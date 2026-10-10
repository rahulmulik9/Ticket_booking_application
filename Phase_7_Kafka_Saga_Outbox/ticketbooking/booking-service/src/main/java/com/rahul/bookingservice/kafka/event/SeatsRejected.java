package com.rahul.bookingservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatsRejected extends BaseEvent {
    private Long bookingId;
    private Long showId;
    private String reason;   // SEAT_NOT_AVAILABLE, SEAT_NOT_FOUND or SHOW_NOT_FOUND
}