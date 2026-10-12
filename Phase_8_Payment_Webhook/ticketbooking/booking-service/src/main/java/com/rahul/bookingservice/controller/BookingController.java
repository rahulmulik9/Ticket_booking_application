package com.rahul.bookingservice.controller;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.dto.BookingResponse;
import com.rahul.bookingservice.mapper.BookingMapper;
import com.rahul.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @AuthenticationPrincipal Long userId is the user id the JWT filter put into the security context.
@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Book seats for a show. Send a unique Idempotency-Key header. Poll GET /bookings/{id} for the final status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking accepted, status is PENDING"),
            @ApiResponse(responseCode = "400", description = "Invalid body or missing Idempotency-Key"),
            @ApiResponse(responseCode = "422", description = "Idempotency-Key reused with a different request")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@AuthenticationPrincipal Long userId,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody BookingRequest request) {
        return bookingMapper.toResponse(bookingService.createBooking(userId, idempotencyKey, request));
    }
    @Operation(summary = "List my bookings, newest first")
    @GetMapping
    public List<BookingResponse> getMyBookings(@AuthenticationPrincipal Long userId) {
        return bookingService.getMyBookings(userId).stream().map(bookingMapper::toResponse).toList();
    }

    @Operation(summary = "Get one of my bookings")
    @GetMapping("/{id}")
    public BookingResponse getBooking(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingMapper.toResponse(bookingService.getBooking(userId, id));
    }

    @Operation(summary = "Cancel one of my bookings and free its seats")
    @PostMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingMapper.toResponse(bookingService.cancelBooking(userId, id));
    }
}