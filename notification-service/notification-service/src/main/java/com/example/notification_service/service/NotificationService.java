package com.example.notification_service.service;

import com.example.notification_service.dto.OrderCancelledEvent;
import com.example.notification_service.dto.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String STRING_VALUE_DESERIALIZER =
            "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer";

    private final JsonMapper jsonMapper;

    public NotificationService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service",
            properties = STRING_VALUE_DESERIALIZER
    )
    public void handlePaymentCompleted(String rawEvent) {
        if (!rawEvent.contains("PaymentCompleted")) {
            return;
        }

        PaymentCompletedEvent event = jsonMapper.readValue(rawEvent, PaymentCompletedEvent.class);
        log.info("[NOTIFICATION] Sending confirmation email for order: {}", event.orderId());
        sendConfirmationEmail(event.orderId(), event.transactionId());
        log.info("[NOTIFICATION] Confirmation sent for order: {}", event.orderId());
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "inventory-events",
            groupId = "notification-service-cancel",
            properties = STRING_VALUE_DESERIALIZER
    )
    public void handleOrderCancelled(String rawEvent) {
        if (!rawEvent.contains("OrderCancelled")) {
            return;
        }

        OrderCancelledEvent event = jsonMapper.readValue(rawEvent, OrderCancelledEvent.class);
        log.info("[NOTIFICATION] Sending cancellation notification for order: {}", event.orderId());
        sendCancellationNotification(event.orderId(), event.reason());
    }

    @DltHandler
    public void handleDlt(String rawEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[NOTIFICATION] DLT: Event from topic '{}' exhausted all retries. "
                + "Manual intervention required. Event: {}", topic, rawEvent);
    }

    private void sendConfirmationEmail(String orderId, String transactionId) {
        log.info("[EMAIL] Order {} confirmed. Transaction: {}", orderId, transactionId);
    }

    private void sendCancellationNotification(String orderId, String reason) {
        log.info("[EMAIL] Order {} cancelled. Reason: {}", orderId, reason);
    }
}