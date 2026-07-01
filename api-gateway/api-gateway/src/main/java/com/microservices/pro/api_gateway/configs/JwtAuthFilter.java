package com.microservices.pro.api_gateway.configs;

import com.microservices.pro.api_gateway.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import org.springframework.util.AntPathMatcher;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JWTUtil jWTUtil;

    public JwtAuthFilter(JWTUtil jWTUtil) {
        this.jWTUtil = jWTUtil;
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public record PublicRoute(HttpMethod method, String pathPattern) {}

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        if (isPublicRoute(method, path)) {
            return chain.filter(exchange);
        }
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Missing token"); // Step 2-3
        }
        try {
            Claims claims = jWTUtil.validateToken(authHeader.substring(7)); // Step 4
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            if (role == null || role.isBlank()) {
                return unauthorizedResponse(exchange, "Token does not contain role claim");
            }
            ServerHttpRequest enriched = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                        headers.add("X-User-Id", userId);
                        headers.add("X-User-Role", role);
                    }).build();
            return chain.filter(exchange.mutate().request(enriched).build()); // Step 7
        } catch (JwtException e) {
            return unauthorizedResponse(exchange, "Invalid or expired token"); // Step 5
        }


    }

    private boolean isPublicRoute(HttpMethod method, String path) {
        return PUBLIC_ROUTES.stream().anyMatch(route ->
                route.method().equals(method)
                        && pathMatcher.match(route.pathPattern(), path)
        );
    }

    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.GET, "/api/v1/products/**"),
            new PublicRoute(HttpMethod.GET, "/actuator/health"),
            new PublicRoute(HttpMethod.POST, "/actuator/info")
    );

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = """
             {
             "status": 401,
            "error": "Unauthorized",
            "message": "%s"
             }
             """.formatted(message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // runs after LoggingFilter
    }


}
