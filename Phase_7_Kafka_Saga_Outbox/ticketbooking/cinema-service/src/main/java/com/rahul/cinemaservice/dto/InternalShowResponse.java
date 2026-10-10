package com.rahul.cinemaservice.dto;

import com.rahul.cinemaservice.entity.Show;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// What Booking needs about a show: its price, and proof that it exists.
@Getter
@Setter
@NoArgsConstructor
public class InternalShowResponse {

    private Long id;
    private BigDecimal price;
    private LocalDateTime startTime;

    public static InternalShowResponse fromEntity(Show show) {
        InternalShowResponse response = new InternalShowResponse();
        response.setId(show.getId());
        response.setPrice(show.getPrice());
        response.setStartTime(show.getStartTime());
        return response;
    }
}