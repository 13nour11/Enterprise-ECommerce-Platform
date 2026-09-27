package com.microservices.pro.api_gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.client.simple.instances[PRODUCT-SERVER][0].uri=http://localhost:${wiremock.server.port}",
        "spring.cloud.gateway.filter.request-rate-limiter.deny-empty-key=false"
})
@AutoConfigureWireMock(port = 0)
class ApiVersioningRouteTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
        stubFor(get(urlPathMatching("/api/v1/products.*")).willReturn(okJson("[]")));
    }

    @Test
    void legacyPath_isRewrittenToV1_withDeprecationHeaders() {
        webTestClient.get().uri("/api/products/42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Deprecation", "true")
                .expectHeader().valueEquals("Sunset", "Wed, 01 Oct 2026 00:00:00 GMT");

        verify(getRequestedFor(urlEqualTo("/api/v1/products/42")));
    }

    @Test
    void legacyRootPath_isRewrittenToV1Collection() {
        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Deprecation", "true");

        verify(getRequestedFor(urlEqualTo("/api/v1/products")));
    }

    @Test
    void v1Path_isServedWithoutDeprecationHeaders() {
        webTestClient.get().uri("/api/v1/products")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Deprecation")
                .expectHeader().doesNotExist("Sunset");
    }
}
