package com.rahul.cinemaservice.mapper;

import com.rahul.cinemaservice.dto.InternalShowResponse;
import com.rahul.cinemaservice.dto.ShowResponse;
import com.rahul.cinemaservice.entity.Show;
import org.springframework.stereotype.Component;

@Component
public class ShowMapper {

    // the movie is loaded together with the show (@EntityGraph), so this does not fire an extra query
    public ShowResponse toResponse(Show show) {
        ShowResponse response = new ShowResponse();
        response.setId(show.getId());
        response.setMovieId(show.getMovie().getId());
        response.setMovieTitle(show.getMovie().getTitle());
        response.setStartTime(show.getStartTime());
        response.setPrice(show.getPrice());
        return response;
    }

    // the smaller shape Booking needs
    public InternalShowResponse toInternalResponse(Show show) {
        InternalShowResponse response = new InternalShowResponse();
        response.setId(show.getId());
        response.setPrice(show.getPrice());
        response.setStartTime(show.getStartTime());
        return response;
    }
}