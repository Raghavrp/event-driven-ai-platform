package com.raghavrp.orderprocessing.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "productId is required")
    private String productId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;

    @PositiveOrZero(message = "totalAmount must be zero or positive")
    private double totalAmount;
}
