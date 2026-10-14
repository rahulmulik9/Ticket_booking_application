package com.rahul.apigateway.filter;

import com.rahul.apigateway.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// The first JWT check. It runs for every routed request, before the call is sent on.
// It only asks: is this a genuine, unexpired token? Roles are checked later, inside each service.
// The Authorization header is passed on unchanged, so each service verifies the token again (Option 2).
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (isPublic(method, path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Login required");
        }

        String token = header.substring(BEARER_PREFIX.length());
        if (!jwtService.isValid(token)) {
            log.debug("Gateway rejected a token for {}", path);
            return unauthorized(exchange, "Token is invalid or expired");
        }

        return chain.filter(exchange);
    }

    // Same public list as the services: login and sign-up, and browsing movies, shows and seats.
    private boolean isPublic(HttpMethod method, String path) {
        if (pathMatcher.match("/api/v1/auth/**", path)) {
            return true;
        }
        return HttpMethod.GET.equals(method)
                && (pathMatcher.match("/api/v1/movies/**", path) || pathMatcher.match("/api/v1/shows/**", path));
    }

    // 401 in the same JSON shape every service uses
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getRawPath().replace("\\", "").replace("\"", "");
        String body = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                message, path, LocalDateTime.now());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    // Runs before the routing filters, so a bad token never reaches a service.
    @Override
    public int getOrder() {
        return -1;
    }
}