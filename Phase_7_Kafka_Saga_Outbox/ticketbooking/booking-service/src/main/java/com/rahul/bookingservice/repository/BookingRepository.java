package com.rahul.bookingservice.repository;

import com.rahul.bookingservice.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // The user id is part of the lookup, so someone else's booking simply "does not exist".
    // The seats come in the same query, because open-in-view is off.
    @EntityGraph(attributePaths = "bookingSeats")
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "bookingSeats")
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // SELECT ... FOR UPDATE. Two cancels of the same booking cannot run together:
    // the second one waits, then sees CANCELLED.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id and b.userId = :userId")
    Optional<Booking> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}