package com.example.order_service.service;

import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private  final RestTemplate restTemplate;

    // gateway
    // but if direct so that : 8081
    private static final String PAYMENT_API_URL = "http://localhost:8080/api/v1/payments";

    public  PaymentService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest paymentRequest){
        return restTemplate.postForObject(PAYMENT_API_URL,paymentRequest,PaymentResponse.class);
    }

    public PaymentResponse paymentFallback(PaymentRequest request, Throwable ex) {
        log.warn("Payment Failed, returning PENDING, Reason: {}", ex.getMessage());
        return new PaymentResponse(request.amount()); // ده هيعمل UUID تلقائي
    }
}
