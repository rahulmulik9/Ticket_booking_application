package com.rahul.ticketbooking.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateShowRequest {

    private Long movieId;
    private LocalDateTime startTime;
    private BigDecimal price;
}