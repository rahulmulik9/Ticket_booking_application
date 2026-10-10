package com.rahul.cinemaservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelled extends BaseEvent {
    private Long bookingId;
    private Long userId;
    private Long showId;
    private List<Long> seatIds;
    private Long paymentId;
}