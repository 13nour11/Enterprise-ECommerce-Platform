package com.example.notification_service.messaging;

import com.example.notification_service.dto.OrderCreatedEvent;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

@Component
public class OrderCreatedEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(OrderCreatedEventListener.class);
    @KafkaListener(
            topics = "order-created",
            groupId = "notification-service"
    )
    public void handle(OrderCreatedEvent event) {

        log.info(
                """ 
                ======================================== 
                NOTIFICATION SERVICE 
                Sending order confirmation email... 
                Order ID: %s 
                Customer ID: %s 
                Total Amount: %s 
                ======================================== 
                """.formatted(
                        event.orderId(),
                        event.customerId(),
                        event.totalAmount()
                )
        );
    }
}
