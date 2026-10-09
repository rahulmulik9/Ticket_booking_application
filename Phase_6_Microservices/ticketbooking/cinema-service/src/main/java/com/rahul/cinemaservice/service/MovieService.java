package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.dto.CreateMovieRequest;
import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.exception.ResourceNotFoundException;
import com.rahul.cinemaservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;


    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public Movie createMovie(CreateMovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.getTitle().trim());
        movie.setLanguage(request.getLanguage().trim());
        movie.setDurationMinutes(request.getDurationMinutes());

        Movie saved = movieRepository.save(movie);
        log.info("Created movie with id {}", saved.getId());
        return saved;
    }

    public Page<Movie> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable);
    }

    public Page<Movie> searchMovies(String name, Pageable pageable) {
        if (name.isBlank()) {
            return movieRepository.findAll(pageable);
        }
        return movieRepository.findByTitleContainingIgnoreCase(name.trim(), pageable);
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id " + id));
    }
}