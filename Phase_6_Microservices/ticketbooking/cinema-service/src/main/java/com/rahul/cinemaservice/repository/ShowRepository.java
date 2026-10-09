package com.rahul.cinemaservice.repository;

import com.rahul.cinemaservice.entity.Show;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {

    // Load the movie in the same query. Without this, reading the movie title later
    // fires one extra query per show (the N+1 problem from Phase 3).
    @Override
    @EntityGraph(attributePaths = "movie")
    Optional<Show> findById(Long id);

    @EntityGraph(attributePaths = "movie")
    List<Show> findByMovieId(Long movieId);
}