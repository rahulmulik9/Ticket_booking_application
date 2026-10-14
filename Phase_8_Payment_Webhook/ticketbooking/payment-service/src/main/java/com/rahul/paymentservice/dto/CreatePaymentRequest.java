package com.rahul.paymentservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "bookingId is required")
    @Positive(message = "bookingId must be positive")
    private Long bookingId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    @Digits(integer = 8, fraction = 2, message = "amount must have at most 2 decimal places")   // fits NUMERIC(10,2)
    private BigDecimal amount;
}