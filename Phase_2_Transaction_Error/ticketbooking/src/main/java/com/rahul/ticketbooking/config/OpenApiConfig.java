package com.rahul.ticketbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketBookingOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Ticket Booking API")
                .version("v1")
                .description("BookMyShow-style ticket booking system"));
    }
}