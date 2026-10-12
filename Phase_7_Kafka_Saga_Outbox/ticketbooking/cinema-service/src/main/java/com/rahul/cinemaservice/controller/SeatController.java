package com.rahul.cinemaservice.controller;

import com.rahul.cinemaservice.dto.SeatResponse;
import com.rahul.cinemaservice.mapper.SeatMapper;
import com.rahul.cinemaservice.service.SeatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Seats")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;
    private final SeatMapper seatMapper;

    @GetMapping("/shows/{showId}/seats")
    public List<SeatResponse> getSeatsByShow(@PathVariable Long showId) {
        return seatService.getSeatsByShow(showId).stream().map(seatMapper::toResponse).toList();
    }
}