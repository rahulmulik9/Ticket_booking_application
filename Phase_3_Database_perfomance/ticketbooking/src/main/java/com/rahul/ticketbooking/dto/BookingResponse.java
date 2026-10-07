package com.rahul.ticketbooking.dto;

import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.enums.BookingStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BookingResponse {

    private Long id;
    private Long showId;
    private Long userId;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private List<Long> seatIds;

    public static BookingResponse from(Booking booking, List<Long> seatIds) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setShowId(booking.getShow().getId());
        response.setUserId(booking.getUserId());
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setSeatIds(seatIds);
        return response;
    }
}