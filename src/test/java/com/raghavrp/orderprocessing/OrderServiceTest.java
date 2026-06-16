package com.raghavrp.orderprocessing;

import com.raghavrp.orderprocessing.dto.OrderRequest;
import com.raghavrp.orderprocessing.exception.OrderNotFoundException;
import com.raghavrp.orderprocessing.kafka.OrderEvent;
import com.raghavrp.orderprocessing.kafka.OrderProducer;
import com.raghavrp.orderprocessing.model.Order;
import com.raghavrp.orderprocessing.model.OrderStatus;
import com.raghavrp.orderprocessing.repository.OrderRepository;
import com.raghavrp.orderprocessing.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderProducer orderProducer;
    @InjectMocks OrderService orderService;

    @Test
    void shouldCreateOrderWithCreatedStatus() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId("cust-1");
        request.setProductId("prod-1");
        request.setQuantity(2);
        request.setTotalAmount(100.0);

        Order saved = Order.builder()
                .id("order-1")
                .customerId("cust-1")
                .productId("prod-1")
                .quantity(2)
                .totalAmount(100.0)
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.save(any())).thenReturn(saved);

        Order result = orderService.createOrder(request, "user");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getCustomerId()).isEqualTo("cust-1");
        verify(orderProducer).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void shouldThrowNotFoundWhenOrderMissing() {
        when(orderRepository.findById("bad-id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById("bad-id"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("bad-id");
    }

    @Test
    void shouldThrowConflictWhenCancellingCompletedOrder() {
        Order completed = Order.builder()
                .id("order-2")
                .status(OrderStatus.COMPLETED)
                .build();
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> orderService.cancelOrder("order-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a completed order");
    }
}
