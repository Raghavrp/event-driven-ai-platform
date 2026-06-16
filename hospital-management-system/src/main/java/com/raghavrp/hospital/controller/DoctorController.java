package com.raghavrp.hospital.controller;

import com.raghavrp.hospital.dto.DoctorRequest;
import com.raghavrp.hospital.model.Doctor;
import com.raghavrp.hospital.service.DoctorService;
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
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Manage doctors and their availability")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @Operation(summary = "Add a new doctor — ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> add(@Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.addDoctor(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID", description = "Result is cached in Redis")
    public ResponseEntity<Doctor> getById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @GetMapping
    @Operation(summary = "Get all doctors")
    public ResponseEntity<List<Doctor>> getAll() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/available")
    @Operation(summary = "Get available doctors", description = "Cached list — doctors accepting appointments")
    public ResponseEntity<List<Doctor>> getAvailable() {
        return ResponseEntity.ok(doctorService.getAvailableDoctors());
    }

    @GetMapping("/specialization/{spec}")
    @Operation(summary = "Find doctors by specialization")
    public ResponseEntity<List<Doctor>> getBySpecialization(@PathVariable String spec) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialization(spec));
    }

    @PatchMapping("/{id}/toggle-availability")
    @Operation(summary = "Toggle doctor availability (on leave ↔ active) — ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.toggleAvailability(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update doctor details — ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, request));
    }
}
