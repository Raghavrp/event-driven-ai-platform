package com.raghavrp.hospital.service;

import com.raghavrp.hospital.dto.*;
import com.raghavrp.hospital.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * AuthService — handles user login and JWT token generation.
 *
 * Flow:
 *  1. AuthenticationManager.authenticate() → validates username + password against DB
 *  2. If valid → load UserDetails → generate JWT
 *  3. Return token to client — client sends it as "Authorization: Bearer <token>"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {
        // This throws BadCredentialsException if username/password is wrong
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        String role = user.getAuthorities().iterator().next().getAuthority();

        log.info("User logged in: {} with role {}", request.getUsername(), role);
        return new AuthResponse(token, request.getUsername(), role, 86400000L);
    }
}
