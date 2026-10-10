package com.rahul.bookingservice.client;

import com.rahul.bookingservice.config.CinemaFeignConfig;
import com.rahul.bookingservice.dto.InternalShowResponse;
import com.rahul.bookingservice.dto.SeatActionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// name = the service's spring.application.name. Eureka turns it into a real address.
// Feign writes the HTTP code for us from this interface.
@FeignClient(name = "cinema-service", configuration = CinemaFeignConfig.class)
public interface CinemaClient {

    @GetMapping("/internal/shows/{showId}")
    InternalShowResponse getShow(@PathVariable("showId") Long showId);

    @PostMapping("/internal/shows/{showId}/seats/reserve")
    void reserveSeats(@PathVariable("showId") Long showId, @RequestBody SeatActionRequest request);

    @PostMapping("/internal/shows/{showId}/seats/release")
    void releaseSeats(@PathVariable("showId") Long showId, @RequestBody SeatActionRequest request);
}