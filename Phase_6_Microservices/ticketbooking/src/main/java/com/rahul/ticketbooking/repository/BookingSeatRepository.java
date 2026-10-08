package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.BookingSeat;
import com.rahul.ticketbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("select bs.seat.id from BookingSeat bs where bs.booking.id = :bookingId order by bs.seat.id")
    List<Long> findSeatIdsByBookingId(@Param("bookingId") Long bookingId);

    @Query("select bs.seat from BookingSeat bs where bs.booking.id = :bookingId")
    List<Seat> findSeatsByBookingId(@Param("bookingId") Long bookingId);

    // returns rows of [bookingId, seatId]
    @Query("select bs.booking.id, bs.seat.id from BookingSeat bs where bs.booking.id in :bookingIds order by bs.seat.id")
    List<Object[]> findBookingSeatPairs(@Param("bookingIds") List<Long> bookingIds);
}