package com.rahul.cinemaservice.controller;

import com.rahul.cinemaservice.dto.CreateShowRequest;
import com.rahul.cinemaservice.dto.ShowResponse;
import com.rahul.cinemaservice.mapper.ShowMapper;
import com.rahul.cinemaservice.service.ShowService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;

@Tag(name = "Shows")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final ShowMapper showMapper;

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody CreateShowRequest request) {
        return showMapper.toResponse(showService.createShow(request));
    }

    @GetMapping("/movies/{movieId}/shows")
    public List<ShowResponse> getShowsByMovie(@PathVariable Long movieId) {
        return showService.getShowsByMovie(movieId).stream().map(showMapper::toResponse).toList();

    }

    @GetMapping("/shows/{id}")
    public ShowResponse getShowById(@PathVariable Long id) {
        return showMapper.toResponse(showService.getShowById(id));
    }
}