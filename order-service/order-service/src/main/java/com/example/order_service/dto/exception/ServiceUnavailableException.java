package com.example.order_service.dto.exception;

public class ServiceUnavailableException extends RuntimeException{

    public ServiceUnavailableException(String message){
        super(message);
    }
}
