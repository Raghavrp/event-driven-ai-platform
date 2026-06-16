package com.raghavrp.hospital.exception;

/**
 * Thrown when a business rule is violated.
 * Example: booking an appointment with an unavailable doctor,
 * or scheduling in the past.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
