package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.service.BookingService;
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

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Book seats for a show")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Show or seat not found"),
            @ApiResponse(responseCode = "409", description = "Seat already taken")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@AuthenticationPrincipal Long userId, @Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(userId, request);
    }

    @Operation(summary = "Get a booking by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return bookingService.getBookingById(id, userId);
    }

    @Operation(summary = "Cancel a booking and free its seats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking already cancelled")
    })
    @PostMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return bookingService.cancelBooking(id, userId);

    }

    @Operation(summary = "List my bookings")
    @GetMapping
    public List<BookingResponse> getMyBookings(@AuthenticationPrincipal Long userId) {
        return bookingService.getMyBookings(userId);
    }
}