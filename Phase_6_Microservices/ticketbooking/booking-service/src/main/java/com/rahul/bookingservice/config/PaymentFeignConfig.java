package com.rahul.bookingservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Used only by PaymentClient. Not annotated @Configuration, for the same reason as CinemaFeignConfig.
public class PaymentFeignConfig {

    // Payment's endpoints need a login, so we pass the user's own token along.
    // The call runs on the same thread as the incoming request, so the request is still available here.
    @Bean
    public RequestInterceptor forwardTokenInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null) {
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        };
    }
}