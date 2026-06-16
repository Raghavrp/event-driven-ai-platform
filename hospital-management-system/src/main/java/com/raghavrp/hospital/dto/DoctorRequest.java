package com.raghavrp.hospital.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DoctorRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private boolean available = true;
}
