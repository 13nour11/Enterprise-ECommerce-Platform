package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Value("${payment.failure-rate:0.5}")
    private double failureRate;

//    @Value("${payment.delay-ms:0}")
    @Value("${payment.delay-ms:0}")
    private long delayMs;

    private final Random random = new Random();

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) throws InterruptedException {
        if(delayMs > 0){
            Thread.sleep(delayMs);
        }
        if(random.nextDouble() < failureRate){
            throw new RuntimeException("Payment gateway timeout");
        }

        return ResponseEntity.ok(new PaymentResponse(
                UUID.randomUUID().toString(),
                "APPROVED",
                request.amount()
        ));
    }

}
