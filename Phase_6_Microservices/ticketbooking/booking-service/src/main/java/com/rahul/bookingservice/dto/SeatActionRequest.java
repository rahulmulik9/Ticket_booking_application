package com.rahul.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Body for Cinema's reserve and release calls.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatActionRequest {

    private List<Long> seatIds;
}