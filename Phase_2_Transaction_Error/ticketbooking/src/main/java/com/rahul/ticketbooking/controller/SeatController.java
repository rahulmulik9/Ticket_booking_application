package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.SeatResponse;
import com.rahul.ticketbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/shows/{showId}/seats")
    public List<SeatResponse> getSeatsByShow(@PathVariable Long showId) {
        return seatService.getSeatsByShow(showId).stream()
                .map(SeatResponse::from)
                .toList();
    }
}