package com.rahul.ticketbooking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "showId is required")
    @Positive(message = "showId must be positive")
    private Long showId;

    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Long userId;

    @NotEmpty(message = "seatIds must not be empty")
    private List<@NotNull(message = "seatId must not be null") @Positive(message = "seatId must be positive") Long> seatIds;
}