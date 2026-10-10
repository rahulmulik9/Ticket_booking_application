package com.rahul.bookingservice.config;

import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.exception.SeatNotAvailableException;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

// Used only by CinemaClient. It is NOT annotated @Configuration on purpose:
// a @Configuration class would apply to every Feign client, and the Payment client
// would then get Cinema's internal key and Cinema's 404 and 409 meanings.
public class CinemaFeignConfig {

    // Every call to Cinema carries the shared internal key. Cinema's /internal/** accepts nothing else.
    @Bean
    public RequestInterceptor internalKeyInterceptor(@Value("${internal.api-key}") String apiKey) {
        return template -> template.header("X-Internal-Key", apiKey);
    }

    // Turns Cinema's HTTP answers into our own exceptions, so GlobalExceptionHandler
    // can answer the caller with the right status.
    @Bean
    public ErrorDecoder cinemaErrorDecoder() {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> switch (response.status()) {
            case 404 -> new ResourceNotFoundException("Show or seat not found");
            case 409 -> new SeatNotAvailableException("One or more seats are already booked");
            default -> fallback.decode(methodKey, response);   // becomes a FeignException, answered as 503
        };
    }
}