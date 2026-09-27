package com.microservices.pro.api_gateway.configs;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserHeadersFilterTest {

    private final UserHeadersFilter filter = new UserHeadersFilter();

    @Test
    void authenticatedRequest_getsUserHeadersFromToken() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-123")
                .claim("realm_access", Map.of("roles", List.of("customer")))
                .build();
        ServerWebExchange exchange = exchange().mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt)))
                .build();

        HttpHeaders headers = forwardedHeaders(exchange);

        assertThat(headers.get("X-User-Id")).containsExactly("user-123");
        assertThat(headers.get("X-User-Role")).containsExactly("customer");
    }

    @Test
    void anonymousRequest_hasSpoofedUserHeadersRemoved() {
        HttpHeaders headers = forwardedHeaders(exchange());

        assertThat(headers.containsKey("X-User-Id")).isFalse();
        assertThat(headers.containsKey("X-User-Role")).isFalse();
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                .header("X-User-Id", "spoofed")
                .header("X-User-Role", "admin"));
    }

    private HttpHeaders forwardedHeaders(ServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = next -> {
            forwarded.set(next);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        return forwarded.get().getRequest().getHeaders();
    }
}
