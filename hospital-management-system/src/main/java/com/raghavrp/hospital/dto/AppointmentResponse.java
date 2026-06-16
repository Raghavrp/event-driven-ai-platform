package com.raghavrp.hospital.dto;

import com.raghavrp.hospital.model.AppointmentStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * We return a DTO instead of the raw Appointment entity.
 * This avoids leaking JPA lazy-loading proxies and internal fields to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private String patientName;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String reason;
    private String notes;
    private LocalDateTime createdAt;
}
