package com.rahul.cinemaservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Body of reserve and release: {"seatIds": [1, 2]}
@Getter
@Setter
@NoArgsConstructor
public class SeatActionRequest {

    @NotEmpty(message = "seatIds must not be empty")
    private List<@NotNull(message = "seatIds must not contain null") @Positive(message = "seat ids must be positive") Long> seatIds;
}