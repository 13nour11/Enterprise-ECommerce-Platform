package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.messaging.OrderEventPublisher;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest(properties = {
        "payment.api-url=http://localhost:${wiremock.server.port}/api/v1/payments",
        "spring.cloud.openfeign.client.config.INVENTORY-SERVICE.url=http://localhost:${wiremock.server.port}",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:${wiremock.server.port}/token",
        "management.zipkin.tracing.endpoint=http://localhost:${wiremock.server.port}/api/v2/spans",
        "eureka.client.enabled=false"
})
@AutoConfigureWireMock(port = 0)
@AutoConfigureObservability
class OrderTracePropagationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private Tracer tracer;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @Test
    void createOrder_keepsTraceContext_onDownstreamCalls() {
        stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson("""
                        {"access_token":"client-credentials-token","token_type":"Bearer","expires_in":300}
                        """)));
        stubFor(get(urlPathEqualTo("/api/v1/inventory/check"))
                .willReturn(okJson("""
                        {"productId":"PROD-001","requestedQuantity":1,"available":true,"remainingStock":99}
                        """)));
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(okJson("""
                        {"transactionId":"TXN-005","amount":100.00}
                        """)));

        String traceId = Observation.createNotStarted("test-order", observationRegistry).observe(() -> {
            orderService.createOrderAsync(new OrderRequest("PROD-001", 1, new BigDecimal("100.00"), "1")).join();
            return tracer.currentSpan().context().traceId();
        });

        verify(getRequestedFor(urlPathEqualTo("/api/v1/inventory/check"))
                .withHeader("traceparent", containing(traceId)));
        verify(postRequestedFor(urlEqualTo("/api/v1/payments"))
                .withHeader("traceparent", containing(traceId)));
    }
}
