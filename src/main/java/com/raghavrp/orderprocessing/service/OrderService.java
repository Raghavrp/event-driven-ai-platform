package com.raghavrp.orderprocessing.service;

import com.raghavrp.orderprocessing.exception.OrderNotFoundException;
import com.raghavrp.orderprocessing.kafka.OrderEvent;
import com.raghavrp.orderprocessing.kafka.OrderProducer;
import com.raghavrp.orderprocessing.model.Order;
import com.raghavrp.orderprocessing.model.OrderStatus;
import com.raghavrp.orderprocessing.repository.OrderRepository;
import com.raghavrp.orderprocessing.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    /**
     * Creates order, saves to MongoDB, then publishes Kafka event.
     * The consumer will asynchronously move status CREATED -> PROCESSING -> COMPLETED.
     */
    public Order createOrder(OrderRequest request, String createdBy) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.CREATED)
                .createdBy(createdBy)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created id={} customerId={}", saved.getId(), saved.getCustomerId());

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(saved.getId())
                .customerId(saved.getCustomerId())
                .productId(saved.getProductId())
                .quantity(saved.getQuantity())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus())
                .eventType("ORDER_CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        orderProducer.publishOrderEvent(event);
        return saved;
    }

    /**
     * Redis cache with key = orderId.
     * Cache is populated on first fetch; evicted when order is cancelled.
     */
    @Cacheable(value = "orders", key = "#orderId")
    public Order getOrderById(String orderId) {
        log.debug("Cache MISS — fetching order {} from MongoDB", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    @Cacheable(value = "customerOrders", key = "#customerId")
    public List<Order> getOrdersByCustomer(String customerId) {
        log.debug("Cache MISS — fetching orders for customer {} from MongoDB", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Cancels order and evicts it from Redis cache to prevent stale reads.
     */
    @CacheEvict(value = "orders", key = "#orderId")
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        log.info("Order {} cancelled", orderId);

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .status(OrderStatus.CANCELLED)
                .eventType("ORDER_CANCELLED")
                .timestamp(LocalDateTime.now())
                .build();

        orderProducer.publishOrderEvent(event);
        return updated;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
