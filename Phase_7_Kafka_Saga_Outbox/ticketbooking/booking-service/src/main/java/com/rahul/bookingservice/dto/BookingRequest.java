package com.rahul.bookingservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// There is no userId here. It comes from the token, so nobody can book as someone else.
@Getter
@Setter
@NoArgsConstructor
public class BookingRequest {

    @NotNull(message = "showId is required")
    @Positive(message = "showId must be positive")
    private Long showId;

    @NotEmpty(message = "seatIds must not be empty")
    private List<@NotNull(message = "seatIds must not contain null") @Positive(message = "seat ids must be positive") Long> seatIds;
}