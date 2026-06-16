package com.raghavrp.orderprocessing.exception;

import lombok.*;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
