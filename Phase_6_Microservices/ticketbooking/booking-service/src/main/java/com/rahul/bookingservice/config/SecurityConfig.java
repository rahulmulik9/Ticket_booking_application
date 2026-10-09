package com.rahul.bookingservice.config;

import com.rahul.bookingservice.security.InternalApiKeyFilter;
import com.rahul.bookingservice.security.JwtAuthenticationFilter;
import com.rahul.bookingservice.security.RestAccessDeniedHandler;
import com.rahul.bookingservice.security.RestAuthenticationEntryPoint;
import com.rahul.bookingservice.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity   // switches on @PreAuthorize in MovieService and ShowService
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   @Value("${internal.api-key}") String internalApiKey) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public: docs and health
                        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // broad rule by URL. @PreAuthorize on the service methods is the fine-grained second check.
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings").hasAnyRole("USER")
                        // safety net: anything not listed above needs login
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)        // 401
                        .accessDeniedHandler(accessDeniedHandler))   // 403
                .addFilterBefore(new InternalApiKeyFilter(internalApiKey), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}