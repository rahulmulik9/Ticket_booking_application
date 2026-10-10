package com.rahul.cinemaservice.repository;

import com.rahul.cinemaservice.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Spring Data builds: where lower(title) like lower('%name%')
    // The trigram index in V1 matches exactly this query.
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}