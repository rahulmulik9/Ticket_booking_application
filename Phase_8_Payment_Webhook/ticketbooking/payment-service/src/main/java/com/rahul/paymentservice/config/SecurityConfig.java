package com.rahul.paymentservice.config;

import com.rahul.paymentservice.security.JwtAuthenticationFilter;
import com.rahul.paymentservice.security.RestAccessDeniedHandler;
import com.rahul.paymentservice.security.RestAuthenticationEntryPoint;
import com.rahul.paymentservice.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // the gateway has no JWT, it proves who it is with the X-Signature header
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/payment").permitAll()
                        // most specific rule first, because the first matching rule wins
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/refund").hasRole("ADMIN")
                        // any logged-in user
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        // safety net: anything not listed above needs login
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)        // 401
                        .accessDeniedHandler(accessDeniedHandler))   // 403
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}