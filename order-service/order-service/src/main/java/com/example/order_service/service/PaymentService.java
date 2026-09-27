package com.example.order_service.service;

import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentService {
    private  final RestTemplate restTemplate;

    // gateway
    // but if direct so that : 8081
    private final String paymentApiUrl;

    public  PaymentService(RestTemplate restTemplate,
                           @Value("${payment.api-url:http://localhost:8080/api/v1/payments}") String paymentApiUrl){
        this.restTemplate = restTemplate;
        this.paymentApiUrl = paymentApiUrl;
    }

    public PaymentResponse processPayment(PaymentRequest paymentRequest){
        return restTemplate.postForObject(paymentApiUrl,paymentRequest,PaymentResponse.class);
    }
}
