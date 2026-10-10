package com.rahul.bookingservice.dto;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.enums.BookingStatus;
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
    private List<Long> seatIds;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private Long paymentId;

    public static BookingResponse from(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setShowId(booking.getShowId());
        response.setUserId(booking.getUserId());
        response.setSeatIds(booking.seatIds());
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setPaymentId(booking.getPaymentId());
        return response;
    }
}