package com.raghavrp.orderprocessing.controller;

import com.raghavrp.orderprocessing.dto.*;
import com.raghavrp.orderprocessing.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    /**
     * POST /api/auth/login
     * Body: { "username": "user", "password": "password" }
     * Returns JWT token valid for 24 hours.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);
        log.info("User {} logged in", request.getUsername());

        return ResponseEntity.ok(new AuthResponse(token, request.getUsername(), 86400000L));
    }
}
