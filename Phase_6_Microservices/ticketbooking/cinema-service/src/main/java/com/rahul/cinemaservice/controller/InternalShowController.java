package com.rahul.cinemaservice.controller;

import com.rahul.cinemaservice.dto.InternalShowResponse;
import com.rahul.cinemaservice.dto.SeatActionRequest;
import com.rahul.cinemaservice.mapper.ShowMapper;
import com.rahul.cinemaservice.service.SeatService;
import com.rahul.cinemaservice.service.ShowService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Only other services call these. The Gateway has no route for /internal/**,
// and InternalApiKeyFilter rejects any call without the shared key.
@Hidden
@RestController
@RequestMapping("/internal/shows")
@RequiredArgsConstructor
public class InternalShowController {

    private final ShowService showService;
    private final SeatService seatService;
    private final ShowMapper showMapper;

    @GetMapping("/{showId}")
    public InternalShowResponse getShow(@PathVariable Long showId) {
        return showMapper.toInternalResponse(showService.getShowById(showId));
    }

    @PostMapping("/{showId}/seats/reserve")
    @ResponseStatus(HttpStatus.OK)
    public void reserveSeats(@PathVariable Long showId, @Valid @RequestBody SeatActionRequest request) {
        seatService.reserveSeats(showId, request.getSeatIds());
    }

    @PostMapping("/{showId}/seats/release")
    @ResponseStatus(HttpStatus.OK)
    public void releaseSeats(@PathVariable Long showId, @Valid @RequestBody SeatActionRequest request) {
        seatService.releaseSeats(showId, request.getSeatIds());
    }
}