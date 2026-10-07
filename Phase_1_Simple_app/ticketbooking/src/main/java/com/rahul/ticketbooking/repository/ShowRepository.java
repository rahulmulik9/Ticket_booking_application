package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Show;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Override
    @EntityGraph(attributePaths = "movie")
    Optional<Show> findById(Long id);

    @EntityGraph(attributePaths = "movie")
    List<Show> findByMovieId(Long movieId);
}