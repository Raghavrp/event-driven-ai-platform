package com.raghavrp.hospital.controller;

import com.raghavrp.hospital.dto.PatientRequest;
import com.raghavrp.hospital.model.Patient;
import com.raghavrp.hospital.service.PatientService;
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
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Register and manage hospital patients")
@SecurityRequirement(name = "bearerAuth")   // tells Swagger UI to send JWT for this controller
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Register patient", description = "ADMIN or RECEPTIONIST only")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<Patient> register(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.registerPatient(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID", description = "Result is cached in Redis")
    public ResponseEntity<Patient> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @GetMapping
    @Operation(summary = "Get all patients")
    public ResponseEntity<List<Patient>> getAll() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by name")
    public ResponseEntity<List<Patient>> search(@RequestParam String name) {
        return ResponseEntity.ok(patientService.searchByName(name));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient details")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<Patient> update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.updatePatient(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient — ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
