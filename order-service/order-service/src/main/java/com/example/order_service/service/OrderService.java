package com.example.order_service.service;

import com.example.order_service.dto.*;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryClient inventoryClient;  // injected by Feign


    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
    @TimeLimiter(name="paymentService", fallbackMethod = "timeoutFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
//    Fallback method
//    public CompletableFuture<OrderResponse> createOrderAsync(OrderRequest request){
//        // create order
//
//        return CompletableFuture.supplyAsync(()->{
//            PaymentResponse payment = paymentService.processPayment(
//                    new PaymentRequest(request.amount())
//                    );
//            if(payment.transactionId() == null){
//                return new OrderResponse("PENDING", "Will retry request");
//
//            }
//            return new OrderResponse("CONFIRMED", payment.transactionId());
//        });
//    }
    public CompletableFuture<OrderResponse> createOrderAsync(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Check inventory BEFORE payment
            StockCheckResponse stock = inventoryClient.checkStock(
                    request.productId(), request.quantity());
            if (!stock.available()) {
                return new OrderResponse("REJECTED",
                        "Insufficient stock: only " + stock.remainingStock() + " available");
            }
            // Step 2: Process payment (only if stock is OK)
            PaymentResponse payment = paymentService.processPayment(
                    new PaymentRequest(request.amount()));
            return new OrderResponse("CONFIRMED", payment.transactionId());
        });
    }

    public OrderResponse createOrder(OrderRequest request) {
        PaymentResponse payment = paymentService.processPayment(
                new PaymentRequest(request.amount()));
        return new OrderResponse("CONFIRMED", payment.transactionId());
    }

//    // Fallback: MUST match original params + Throwable as last param
    public CompletableFuture<OrderResponse> paymentFallback(OrderRequest request, Throwable ex) {
        log.warn("Payment failed, returning PENDING. Reason: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse("PENDING", "Will retry payment"));
    }

    // Bulkhead fallback — لازم يرجع CompletableFuture<OrderResponse>
    public CompletableFuture<OrderResponse> bulkheadFallback(OrderRequest request, BulkheadFullException ex) {
        log.warn("[BULKHEAD] Concurrent limit reached: {}", ex.getMessage());
        return CompletableFuture.completedFuture(
                new OrderResponse("QUEUED", "System busy — your order is queued")
        );
    }

    // TimeLimiter fallback — called on TimeoutException
    public CompletableFuture<OrderResponse> timeoutFallback(
            OrderRequest request, TimeoutException ex) {
        log.warn("[TIMEOUT] Payment exceeded 2s limit: {}", ex.getMessage());
        return CompletableFuture.completedFuture(
                new OrderResponse("PENDING", "Payment timed out"));
    }

}