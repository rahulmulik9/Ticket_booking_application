package com.rahul.cinemaservice.mapper;

import com.rahul.cinemaservice.dto.SeatResponse;
import com.rahul.cinemaservice.entity.Seat;
import org.springframework.stereotype.Component;

@Component
public class SeatMapper {

    // the reservation id is not copied: it is internal and users do not need it
    public SeatResponse toResponse(Seat seat) {
        SeatResponse response = new SeatResponse();
        response.setId(seat.getId());
        response.setSeatNumber(seat.getSeatNumber());
        response.setStatus(seat.getStatus());
        return response;
    }
}