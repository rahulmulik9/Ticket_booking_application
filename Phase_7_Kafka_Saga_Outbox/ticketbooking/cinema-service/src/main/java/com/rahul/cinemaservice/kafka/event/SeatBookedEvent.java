package com.rahul.cinemaservice.kafka.event;

import com.rahul.bookingservice.kafka.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatBookedEvent extends BaseEvent {
    private Long bookingId;
    private Long showId;
    private List<Long> seatIds;
    private BigDecimal totalAmount;
}