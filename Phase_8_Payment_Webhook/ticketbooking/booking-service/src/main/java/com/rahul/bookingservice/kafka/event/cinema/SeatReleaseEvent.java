package com.rahul.bookingservice.kafka.event.cinema;

import com.rahul.bookingservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatReleaseEvent extends BaseEvent {
    private Long bookingId;
    private Long showId;
    private List<Long> seatIds;
    private String reason;   // PAYMENT_FAILED or CANCELLED
}