package com.raghavrp.orderprocessing.kafka;

import com.raghavrp.orderprocessing.model.OrderStatus;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private String eventId;
    private String orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private double totalAmount;
    private OrderStatus status;
    private String eventType;   // ORDER_CREATED, ORDER_STATUS_UPDATED
    private LocalDateTime timestamp;
}
