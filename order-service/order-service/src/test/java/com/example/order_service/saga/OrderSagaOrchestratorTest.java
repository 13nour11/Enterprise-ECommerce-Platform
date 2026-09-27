package com.example.order_service.saga;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static com.example.order_service.saga.OrderSagaOrchestrator.SAGA_COMMANDS_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(kafkaTemplate);
    }

    @Test
    void startSaga_sendsReserveInventory_andMovesToInventoryReserving() {
        OrderResponse response = orchestrator.startSaga(
                new OrderRequest("PROD-001", 2, new BigDecimal("100.00"), "1"));

        String orderId = response.orderId();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(orchestrator.getSagaState(orderId)).isEqualTo(SagaState.INVENTORY_RESERVING);
        assertThat(sentCommand(orderId)).isEqualTo(new ReserveInventoryCommand(orderId, "PROD-001", 2));
    }

    @Test
    void inventoryReserved_sendsProcessPayment_andMovesToPaymentProcessing() {
        String orderId = startSaga();

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));

        assertThat(orchestrator.getSagaState(orderId)).isEqualTo(SagaState.PAYMENT_PROCESSING);
        assertThat(sentCommand(orderId)).isEqualTo(new ProcessPaymentCommand(orderId, new BigDecimal("100.00")));
    }

    @Test
    void inventoryFailed_cancelsOrder_withoutCompensation() {
        String orderId = startSaga();

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, false));

        assertThat(orchestrator.getOrderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(orchestrator.getSagaState(orderId)).isNull();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void paymentSucceeded_confirmsOrder() {
        String orderId = startSagaUntilPaymentProcessing();

        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, true));

        assertThat(orchestrator.getOrderStatus(orderId)).isEqualTo("CONFIRMED");
        assertThat(orchestrator.getSagaState(orderId)).isNull();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void paymentFailed_sendsReleaseInventoryCommand_andMovesToInventoryReleasing() {
        String orderId = startSagaUntilPaymentProcessing();

        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false));

        assertThat(orchestrator.getSagaState(orderId)).isEqualTo(SagaState.INVENTORY_RELEASING);
        assertThat(sentCommand(orderId)).isEqualTo(new ReleaseInventoryCommand(orderId));
    }

    @Test
    void inventoryReleased_cancelsOrder() {
        String orderId = startSagaUntilPaymentProcessing();
        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false));

        orchestrator.handleInventoryReleased(new InventoryReleasedEvent(orderId));

        assertThat(orchestrator.getOrderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(orchestrator.getSagaState(orderId)).isNull();
    }

    @Test
    void eventInUnexpectedState_isIgnored() {
        String orderId = startSaga();

        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false));

        assertThat(orchestrator.getSagaState(orderId)).isEqualTo(SagaState.INVENTORY_RESERVING);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    private String startSaga() {
        String orderId = orchestrator.startSaga(
                new OrderRequest("PROD-001", 2, new BigDecimal("100.00"), "1")).orderId();
        clearInvocations(kafkaTemplate);
        return orderId;
    }

    private String startSagaUntilPaymentProcessing() {
        String orderId = startSaga();
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));
        clearInvocations(kafkaTemplate);
        return orderId;
    }

    private Object sentCommand(String orderId) {
        ArgumentCaptor<Object> command = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(SAGA_COMMANDS_TOPIC), eq(orderId), command.capture());
        return command.getValue();
    }
}
