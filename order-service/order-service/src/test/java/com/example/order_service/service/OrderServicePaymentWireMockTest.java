package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.messaging.OrderEventPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "payment.api-url=http://localhost:${wiremock.server.port}/api/v1/payments",
        "spring.cloud.openfeign.client.config.INVENTORY-SERVICE.url=http://localhost:${wiremock.server.port}",
        "eureka.client.enabled=false"
})
@AutoConfigureWireMock(port = 0)
class OrderServicePaymentWireMockTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("paymentService").reset();
        stubFor(get(urlPathEqualTo("/api/v1/inventory/check"))
                .willReturn(okJson("""
                        {"productId":"PROD-001","requestedQuantity":1,"available":true,"remainingStock":99}
                        """)));
    }

    @Test
    void createOrder_returnsConfirmed_whenPaymentApproved() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(okJson("""
                        {"transactionId":"TXN-001","amount":100.00}
                        """)));

        OrderResponse response = orderService.createOrderAsync(
                new OrderRequest("PROD-001", 1, new BigDecimal("100.00"), "1")).join();

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.message()).isEqualTo("TXN-001");
    }

    @Test
    void createOrder_returnsPending_whenPaymentServiceUnavailable() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse().withStatus(503)));

        OrderResponse response = orderService.createOrderAsync(
                new OrderRequest("PROD-001", 1, new BigDecimal("100.00"), "1")).join();

        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void createOrder_sendsCorrectPayload_toPaymentService() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(okJson("""
                        {"transactionId":"TXN-002","amount":250.00}
                        """)));

        orderService.createOrderAsync(
                new OrderRequest("PROD-002", 2, new BigDecimal("250.00"), "1")).join();

        verify(postRequestedFor(urlEqualTo("/api/v1/payments"))
                .withRequestBody(equalToJson("""
                        {"amount":250.00}
                        """)));
    }
}
