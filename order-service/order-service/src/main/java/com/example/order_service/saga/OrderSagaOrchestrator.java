package com.example.order_service.saga;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@KafkaListener(topics = OrderSagaOrchestrator.SAGA_RESULTS_TOPIC, groupId = "order-saga-orchestrator")
public class OrderSagaOrchestrator {

    public static final String SAGA_COMMANDS_TOPIC = "saga-commands";
    public static final String SAGA_RESULTS_TOPIC = "saga-results";

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> orderAmounts = new ConcurrentHashMap<>();
    private final Map<String, String> orderStatuses = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderSagaOrchestrator(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public OrderResponse startSaga(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        orderAmounts.put(orderId, request.amount());
        orderStatuses.put(orderId, "PENDING");
        transition(orderId, SagaState.STARTED);

        kafkaTemplate.send(SAGA_COMMANDS_TOPIC, orderId,
                new ReserveInventoryCommand(orderId, request.productId(), request.quantity()));
        transition(orderId, SagaState.INVENTORY_RESERVING);

        return new OrderResponse(orderId, "PENDING", "Order received -- processing...");
    }

    @KafkaHandler
    public void handleInventoryResult(InventoryResultEvent event) {
        String orderId = event.orderId();
        if (!isInState(orderId, SagaState.INVENTORY_RESERVING)) {
            return;
        }

        if (event.success()) {
            transition(orderId, SagaState.INVENTORY_RESERVED);
            kafkaTemplate.send(SAGA_COMMANDS_TOPIC, orderId,
                    new ProcessPaymentCommand(orderId, orderAmounts.get(orderId)));
            transition(orderId, SagaState.PAYMENT_PROCESSING);
        } else {
            transition(orderId, SagaState.INVENTORY_RESERVE_FAILED);
            finish(orderId, "CANCELLED");
        }
    }

    @KafkaHandler
    public void handlePaymentResult(PaymentResultEvent event) {
        String orderId = event.orderId();
        if (!isInState(orderId, SagaState.PAYMENT_PROCESSING)) {
            return;
        }

        if (event.success()) {
            transition(orderId, SagaState.COMPLETED);
            finish(orderId, "CONFIRMED");
        } else {
            transition(orderId, SagaState.PAYMENT_FAILED);
            kafkaTemplate.send(SAGA_COMMANDS_TOPIC, orderId, new ReleaseInventoryCommand(orderId));
            transition(orderId, SagaState.INVENTORY_RELEASING);
        }
    }

    @KafkaHandler
    public void handleInventoryReleased(InventoryReleasedEvent event) {
        String orderId = event.orderId();
        if (!isInState(orderId, SagaState.INVENTORY_RELEASING)) {
            return;
        }

        transition(orderId, SagaState.CANCELLED);
        finish(orderId, "CANCELLED");
    }

    SagaState getSagaState(String orderId) {
        return sagaStates.get(orderId);
    }

    String getOrderStatus(String orderId) {
        return orderStatuses.get(orderId);
    }

    private boolean isInState(String orderId, SagaState expected) {
        SagaState current = sagaStates.get(orderId);
        if (current != expected) {
            log.warn("[SAGA] Unexpected state {} for order {}", current, orderId);
            return false;
        }
        return true;
    }

    private void transition(String orderId, SagaState newState) {
        SagaState old = sagaStates.put(orderId, newState);
        log.info("[SAGA] {} {} -> {}", orderId, old, newState);
    }

    private void finish(String orderId, String status) {
        orderStatuses.put(orderId, status);
        sagaStates.remove(orderId);
        orderAmounts.remove(orderId);
        log.info("[SAGA] Order {} {}", orderId, status);
    }
}
