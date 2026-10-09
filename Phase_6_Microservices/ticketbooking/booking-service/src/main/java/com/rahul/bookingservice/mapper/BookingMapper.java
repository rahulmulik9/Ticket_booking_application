package com.rahul.bookingservice.mapper;

import com.rahul.bookingservice.dto.BookingResponse;
import com.rahul.bookingservice.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    // the reservation id is internal, so it is not copied
    public BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setShowId(booking.getShowId());
        response.setUserId(booking.getUserId());
        response.setSeatIds(booking.seatIds());
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}