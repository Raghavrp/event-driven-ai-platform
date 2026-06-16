package com.raghavrp.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Patient entity — stored in H2 table 'patient'.
 * Implements Serializable so it can be stored in Redis cache.
 */
@Entity
@Table(name = "patient")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @NotNull
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Email
    private String email;

    private String bloodGroup;   // e.g. O+, A-, B+

    private String address;
}
