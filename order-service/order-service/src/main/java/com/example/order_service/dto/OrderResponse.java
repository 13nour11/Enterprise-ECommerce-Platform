package com.example.order_service.dto;

public record OrderResponse(String orderId,String status, String message) {
    public OrderResponse(String  status, String message){
        this(null,status,message);
    }
}
