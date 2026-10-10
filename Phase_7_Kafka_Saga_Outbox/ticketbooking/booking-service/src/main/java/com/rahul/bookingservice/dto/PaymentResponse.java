package com.rahul.bookingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Only the fields Booking reads. The status stays a String, so Payment can add statuses without breaking us.
@Getter
@Setter
@NoArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String status;
}