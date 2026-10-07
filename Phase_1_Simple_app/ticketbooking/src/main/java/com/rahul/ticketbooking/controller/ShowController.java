package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.CreateShowRequest;
import com.rahul.ticketbooking.dto.ShowResponse;
import com.rahul.ticketbooking.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping("/movies/{movieId}/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@PathVariable Long movieId, @RequestBody CreateShowRequest request) {
        return ShowResponse.from(showService.createShow(movieId, request));
    }

    @GetMapping("/movies/{movieId}/shows")
    public List<ShowResponse> getShowsByMovie(@PathVariable Long movieId) {
        return showService.getShowsByMovie(movieId).stream()
                .map(ShowResponse::from)
                .toList();
    }

    @GetMapping("/shows/{id}")
    public ShowResponse getShowById(@PathVariable Long id) {
        return ShowResponse.from(showService.getShowById(id));
    }
}