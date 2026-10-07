package com.rahul.ticketbooking.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BookingRequest {

    private Long showId;
    private Long userId;
    private List<Long> seatIds;
}