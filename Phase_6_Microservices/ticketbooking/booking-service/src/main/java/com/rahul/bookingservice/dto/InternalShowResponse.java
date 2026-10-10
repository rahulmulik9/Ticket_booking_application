package com.rahul.bookingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Not shared with Cinema on purpose: Booking declares only the fields it reads,
// so Cinema can change its own response without breaking us.
@Getter
@Setter
@NoArgsConstructor
public class InternalShowResponse {

    private Long id;
    private BigDecimal price;
    private LocalDateTime startTime;
}