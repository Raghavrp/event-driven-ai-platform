package com.raghavrp.hospital.controller;

import com.raghavrp.hospital.dto.*;
import com.raghavrp.hospital.model.AppointmentStatus;
import com.raghavrp.hospital.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Book, view, complete and cancel appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(
        summary = "Book appointment",
        description = "Books a slot for a patient with a doctor. Doctor must be available and slot must be free."
    )
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID", description = "Result cached in Redis")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all appointments for a patient", description = "Cached by patientId")
    public ResponseEntity<List<AppointmentResponse>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get all appointments for a doctor", description = "Doctor's daily schedule")
    public ResponseEntity<List<AppointmentResponse>> getByDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get appointments by status", description = "e.g. SCHEDULED, COMPLETED, CANCELLED")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<AppointmentResponse>> getByStatus(@PathVariable AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.getByStatus(status));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark appointment as COMPLETED — DOCTOR or ADMIN only")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ResponseEntity<AppointmentResponse> complete(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id, notes));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
