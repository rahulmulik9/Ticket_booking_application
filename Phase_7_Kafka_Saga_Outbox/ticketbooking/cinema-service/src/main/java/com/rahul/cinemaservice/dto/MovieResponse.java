package com.rahul.cinemaservice.dto;

import com.rahul.cinemaservice.entity.Movie;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MovieResponse {

    private Long id;
    private String title;
    private String language;
    private Integer durationMinutes;

    public static MovieResponse from(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setLanguage(movie.getLanguage());
        response.setDurationMinutes(movie.getDurationMinutes());
        return response;
    }
}