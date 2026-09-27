package com.microservices.pro.api_gateway.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.gateway.filter.request-rate-limiter.deny-empty-key=false"
})
class GatewaySecurityTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void publicProductRead_withoutToken_passesSecurity() {
        webTestClient.get().uri("/api/v1/products")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
    }

    @Test
    void protectedRoute_withoutToken_returns401() {
        webTestClient.get().uri("/api/v1/inventory/check?productId=PROD-001&quantity=1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void adminRoute_withCustomerRole_returns403() {
        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .post().uri("/api/v1/products")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminRoute_withAdminRole_passesSecurity() {
        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .post().uri("/api/v1/products")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
    }
}
