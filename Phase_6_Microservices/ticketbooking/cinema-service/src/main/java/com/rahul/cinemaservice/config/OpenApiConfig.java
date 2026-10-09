package com.rahul.cinemaservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cinemaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Cinema Service API")
                .version("v1")
                .description("Movies, shows and seats"));
    }
}