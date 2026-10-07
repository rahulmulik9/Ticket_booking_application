package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.exception.ResourceNotFoundException;
import com.rahul.ticketbooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;

    public Movie createMovie(Movie movie) {
        Movie saved = movieRepository.save(movie);
        log.info("Created movie with id {}", saved.getId());
        return saved;
    }

    //public List<Movie> getAllMovies() {return movieRepository.findAll();}
    public Page<Movie> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable);
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id " + id));
    }
}