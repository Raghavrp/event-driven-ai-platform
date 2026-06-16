package com.raghavrp.orderprocessing.kafka;

import com.raghavrp.orderprocessing.model.OrderStatus;
import com.raghavrp.orderprocessing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;

    /**
     * Listens to order-events topic.
     * Manual acknowledgment ensures offset is committed only after successful processing.
     * This prevents message loss on consumer crash.
     */
    @KafkaListener(
            topics = OrderProducer.ORDER_TOPIC,
            groupId = "order-processing-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
            OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received event type={} orderId={} partition={} offset={}",
                event.getEventType(), event.getOrderId(), partition, offset);

        try {
            processEvent(event);
            acknowledgment.acknowledge();   // commit offset only on success
        } catch (Exception ex) {
            log.error("Error processing event orderId={}: {}", event.getOrderId(), ex.getMessage());
            // Do NOT acknowledge — Kafka will redeliver the message
        }
    }

    private void processEvent(OrderEvent event) {
        if ("ORDER_CREATED".equals(event.getEventType())) {
            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                log.info("Order {} moved to PROCESSING", event.getOrderId());

                // Simulate async processing delay then complete
                simulateProcessingAndComplete(event.getOrderId());
            });
        }
    }

    private void simulateProcessingAndComplete(String orderId) {
        // In production: trigger payment, inventory check, etc.
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            log.info("Order {} COMPLETED", orderId);
        });
    }
}
