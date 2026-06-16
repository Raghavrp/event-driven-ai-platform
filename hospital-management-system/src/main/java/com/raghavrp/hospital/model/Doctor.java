package com.raghavrp.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;

/**
 * Doctor entity — stored in H2 table 'doctor'.
 * Implements Serializable for Redis caching.
 */
@Entity
@Table(name = "doctor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @NotBlank
    private String specialization;   // e.g. Cardiology, Neurology

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Email
    private String email;

    // true = doctor is accepting appointments, false = on leave / full
    @Column(nullable = false)
    private boolean available = true;
}
