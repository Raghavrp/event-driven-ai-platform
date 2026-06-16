package com.raghavrp.hospital.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI configuration.
 *
 * @SecurityScheme — tells Swagger UI to show an "Authorize" button
 * where you can paste your JWT token. After that, all API calls
 * from the UI will automatically include "Authorization: Bearer <token>".
 *
 * Visit: http://localhost:8081/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Hospital Management System API",
        version = "1.0",
        description = "REST API for managing patients, doctors, and appointments",
        contact = @Contact(
            name = "Raghwendra Pratap Singh",
            email = "raghwendra14all@gmail.com"
        )
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste your JWT token here (get it from POST /api/auth/login)"
)
public class SwaggerConfig {
    // Configuration is done via annotations — no bean methods needed
}
