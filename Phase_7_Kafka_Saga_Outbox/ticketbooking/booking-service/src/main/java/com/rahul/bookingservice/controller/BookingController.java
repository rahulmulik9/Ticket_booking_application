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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// @AuthenticationPrincipal Long userId is the user id the JWT filter put into the security context.
@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Book seats for a show")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking confirmed"),
            @ApiResponse(responseCode = "404", description = "Show or seat not found"),
            @ApiResponse(responseCode = "409", description = "A seat is already booked"),
            @ApiResponse(responseCode = "503", description = "Cinema service is unavailable"),
            @ApiResponse(responseCode = "402", description = "Payment was declined"),
            @ApiResponse(responseCode = "503", description = "Cinema or Payment service is unavailable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@AuthenticationPrincipal Long userId, @Valid @RequestBody BookingRequest request) {
        return bookingMapper.toResponse(bookingService.createBooking(userId, request));
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