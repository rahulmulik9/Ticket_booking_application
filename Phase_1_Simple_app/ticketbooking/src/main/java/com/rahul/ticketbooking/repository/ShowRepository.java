package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {
}