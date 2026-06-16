package com.raghavrp.orderprocessing.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private long expiresIn;
}
