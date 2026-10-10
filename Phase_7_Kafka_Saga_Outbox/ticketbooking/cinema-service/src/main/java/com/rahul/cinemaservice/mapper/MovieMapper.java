package com.rahul.cinemaservice.mapper;

import com.rahul.cinemaservice.dto.MovieResponse;
import com.rahul.cinemaservice.entity.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public MovieResponse toResponse(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setLanguage(movie.getLanguage());
        response.setDurationMinutes(movie.getDurationMinutes());
        return response;
    }
}