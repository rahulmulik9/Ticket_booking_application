package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
}