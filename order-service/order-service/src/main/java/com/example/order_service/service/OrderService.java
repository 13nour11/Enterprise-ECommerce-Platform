package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;

    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
    @TimeLimiter(name="paymentService", fallbackMethod = "timeoutFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
//    Fallback method
    public CompletableFuture<OrderResponse> createOrderAsync(OrderRequest request){
        // create order
        return CompletableFuture.supplyAsync(()->{
            PaymentResponse payment = paymentService.processPayment(
                    new PaymentRequest(request.amount())
                    );
            if(payment.transactionId() == null){
                return new OrderResponse("PENDING", "Will retry request");

            }
                    return new OrderResponse("CONFIRMED", payment.transactionId());
        });
    }

//    public OrderResponse createOrder(OrderRequest request) {
//        PaymentResponse payment = paymentService.processPayment(
//                new PaymentRequest(request.getAmount()));
//        return new OrderResponse("CONFIRMED", payment.getTransactionId());
//    }
//    // Fallback: MUST match original params + Throwable as last param
//    public OrderResponse paymentFallback(OrderRequest request, Throwable ex) {
//        log.warn("Payment failed, returning PENDING. Reason: {}", ex.getMessage());
//        return new OrderResponse("PENDING", "Will retry payment");
//    }
}