package com.athena.gateway.filter;

import com.athena.common.security.AuthHeaders;
import com.athena.common.security.JwtService;
import com.athena.common.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/auth/", "/actuator/");

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isPublic(path)) {
            return chain.filter(withoutClientIdentity(exchange));
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        try {
            Claims claims = jwtService.parseAndValidate(authorization.substring(BEARER_PREFIX.length()), TokenType.ACCESS);
            String userId = jwtService.extractSubject(claims);
            String roles = String.join(",", jwtService.extractRoles(claims));
            return chain.filter(withIdentity(exchange, userId, roles));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected request to {}: {}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired access token");
        }
    }

    private boolean isPublic(String path) {
        if (path.equals("/actuator")) {
            return true;
        }
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private ServerWebExchange withoutClientIdentity(ServerWebExchange exchange) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(AuthHeaders.USER_ID);
                    headers.remove(AuthHeaders.USER_ROLES);
                })
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, String userId, String roles) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(AuthHeaders.USER_ID, userId);
                    headers.set(AuthHeaders.USER_ROLES, roles);
                })
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status":401,"error":"Unauthorized","message":"%s","path":"%s"}"""
                .formatted(message, exchange.getRequest().getURI().getPath());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
