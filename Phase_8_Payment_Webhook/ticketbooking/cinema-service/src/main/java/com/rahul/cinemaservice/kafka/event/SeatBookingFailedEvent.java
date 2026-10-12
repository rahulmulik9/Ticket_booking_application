package com.rahul.cinemaservice.kafka.event;

import com.rahul.cinemaservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatBookingFailedEvent extends BaseEvent {
    private Long bookingId;
    private Long showId;
    private String reason;   // SEAT_NOT_AVAILABLE, SEAT_NOT_FOUND or SHOW_NOT_FOUND
}