package com.rahul.cinemaservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateShowRequest {

    @NotNull(message = "movieId is required")
    @Positive(message = "movieId must be positive")
    private Long movieId;

    @NotNull(message = "startTime is required")
    @Future(message = "startTime must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")   // fits NUMERIC(10,2)
    private BigDecimal price;
}