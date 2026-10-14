package com.rahul.cinemaservice.dto;

import com.rahul.cinemaservice.entity.Show;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ShowResponse {

    private Long id;
    private Long movieId;
    private String movieTitle;
    private LocalDateTime startTime;
    private BigDecimal price;

    // the movie is loaded together with the show (@EntityGraph), so this does not fire an extra query
    public static ShowResponse from(Show show) {
        ShowResponse response = new ShowResponse();
        response.setId(show.getId());
        response.setMovieId(show.getMovie().getId());
        response.setMovieTitle(show.getMovie().getTitle());
        response.setStartTime(show.getStartTime());
        response.setPrice(show.getPrice());
        return response;
    }
}