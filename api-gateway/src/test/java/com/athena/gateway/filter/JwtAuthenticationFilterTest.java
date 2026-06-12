package com.athena.gateway.filter;

import com.athena.common.security.AuthHeaders;
import com.athena.common.security.JwtService;
import com.athena.common.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

    private final WebFilterChain capturingChain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Test
    void publicAuthPath_passesThroughWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/login").build());

        filter().filter(exchange, capturingChain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthHeaders.USER_ID)).isNull();
    }

    @Test
    void protectedPath_withoutBearer_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/users/1").build());

        filter().filter(exchange, capturingChain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_withValidToken_forwardsIdentityHeaders() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseAndValidate("good-token", TokenType.ACCESS)).thenReturn(claims);
        when(jwtService.extractSubject(claims)).thenReturn("user-123");
        when(jwtService.extractRoles(claims)).thenReturn(List.of("USER", "ADMIN"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                        // Attempt to spoof identity — must be overwritten.
                        .header(AuthHeaders.USER_ID, "attacker")
                        .build());

        filter().filter(exchange, capturingChain).block();

        HttpHeaders forwardedHeaders = forwarded.get().getRequest().getHeaders();
        assertThat(forwardedHeaders.getFirst(AuthHeaders.USER_ID)).isEqualTo("user-123");
        assertThat(forwardedHeaders.getFirst(AuthHeaders.USER_ROLES)).isEqualTo("USER,ADMIN");
    }

    @Test
    void protectedPath_withInvalidToken_returns401() {
        when(jwtService.parseAndValidate(eq("bad"), eq(TokenType.ACCESS)))
                .thenThrow(new JwtException("invalid"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/progress/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad")
                        .build());

        filter().filter(exchange, capturingChain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
