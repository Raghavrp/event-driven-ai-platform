package com.raghavrp.hospital.exception;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Standard error response body returned for all errors.
 * Consistent format makes it easy for clients to handle errors programmatically.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
