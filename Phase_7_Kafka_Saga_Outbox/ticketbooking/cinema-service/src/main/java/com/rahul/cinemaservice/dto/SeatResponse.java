package com.rahul.cinemaservice.dto;

import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.enums.SeatStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SeatResponse {

    private Long id;
    private String seatNumber;
    private SeatStatus status;

    public static SeatResponse from(Seat seat) {
        SeatResponse response = new SeatResponse();
        response.setId(seat.getId());
        response.setSeatNumber(seat.getSeatNumber());
        response.setStatus(seat.getStatus());
        return response;
    }
}