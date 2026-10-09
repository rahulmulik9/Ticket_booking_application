package com.rahul.cinemaservice.controller;

import com.rahul.cinemaservice.dto.CreateMovieRequest;
import com.rahul.cinemaservice.dto.MovieResponse;
import com.rahul.cinemaservice.dto.PageResponse;
import com.rahul.cinemaservice.service.MovieService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Movies")
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody CreateMovieRequest request) {
        return MovieResponse.from(movieService.createMovie(request));
    }

    @GetMapping
    public PageResponse<MovieResponse> getAllMovies(@PageableDefault(sort = "id") Pageable pageable) {
        return PageResponse.from(movieService.getAllMovies(pageable).map(MovieResponse::from));
    }


    @GetMapping(params = "name")
    public PageResponse<MovieResponse> searchMovies(@RequestParam String name,
                                                    @PageableDefault(sort = "id") Pageable pageable) {
        return PageResponse.from(movieService.searchMovies(name, pageable).map(MovieResponse::from));
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return MovieResponse.from(movieService.getMovieById(id));
    }
}