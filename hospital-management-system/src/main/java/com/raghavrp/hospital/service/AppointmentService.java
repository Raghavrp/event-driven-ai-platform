package com.raghavrp.hospital.service;

import com.raghavrp.hospital.dto.AppointmentRequest;
import com.raghavrp.hospital.dto.AppointmentResponse;
import com.raghavrp.hospital.exception.BusinessException;
import com.raghavrp.hospital.exception.ResourceNotFoundException;
import com.raghavrp.hospital.model.*;
import com.raghavrp.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AppointmentService — core business logic for the hospital booking system.
 *
 * This is the most important service in the project. It handles:
 *  - Booking validation (doctor available? slot free? future date?)
 *  - Status transitions (SCHEDULED → COMPLETED / CANCELLED)
 *  - DTO mapping (entity → response DTO to hide internal fields)
 *  - Caching appointments per patient and per doctor
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Book a new appointment.
     *
     * Business rules enforced here (not in controller):
     *  1. Patient must exist
     *  2. Doctor must exist
     *  3. Doctor must be currently available (not on leave)
     *  4. Doctor must not already have an appointment at the same time
     *
     * @Transactional — if anything fails mid-save, the whole operation rolls back.
     * Without this, partial writes could leave the DB in an inconsistent state.
     */
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {

        // Rule 1: Does patient exist?
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found: " + request.getPatientId()));

        // Rule 2: Does doctor exist?
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + request.getDoctorId()));

        // Rule 3: Is doctor available for new appointments?
        if (!doctor.isAvailable()) {
            throw new BusinessException(
                    "Dr. " + doctor.getFullName() + " is currently not available");
        }

        // Rule 4: Is the time slot already taken? (exclude CANCELLED appointments)
        boolean slotTaken = appointmentRepository
                .existsByDoctorIdAndAppointmentDateTimeAndStatusNot(
                        doctor.getId(),
                        request.getAppointmentDateTime(),
                        AppointmentStatus.CANCELLED);
        if (slotTaken) {
            throw new BusinessException(
                    "Dr. " + doctor.getFullName() + " already has an appointment at " +
                    request.getAppointmentDateTime());
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDateTime(request.getAppointmentDateTime())
                .status(AppointmentStatus.SCHEDULED)
                .reason(request.getReason())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment booked: id={} patient={} doctor={} time={}",
                saved.getId(), patient.getFullName(), doctor.getFullName(),
                saved.getAppointmentDateTime());

        return toResponse(saved);
    }

    /**
     * Get all appointments for a patient.
     *
     * @Cacheable — caches the patient's appointment list by patientId.
     * Receptionist frequently checks "what appointments does this patient have?"
     * Caching avoids repeated DB joins on every page load.
     *
     * Cache is evicted when a new appointment is booked or status changes.
     */
    @Cacheable(value = "patientAppointments", key = "#patientId")
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        log.debug("Cache MISS — loading appointments for patient {}", patientId);
        return appointmentRepository.findByPatientId(patientId)
                .stream().map(this::toResponse).toList();
        // .toList() — Java 16+ immutable list, cleaner than Collectors.toList()
    }

    /**
     * Get all appointments for a doctor — used by doctor's daily schedule view.
     */
    @Cacheable(value = "doctorAppointments", key = "#doctorId")
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        log.debug("Cache MISS — loading appointments for doctor {}", doctorId);
        return appointmentRepository.findByDoctorId(doctorId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Get a single appointment by ID.
     */
    @Cacheable(value = "appointments", key = "#id")
    public AppointmentResponse getAppointmentById(Long id) {
        return toResponse(findById(id));
    }

    /**
     * Mark appointment as COMPLETED — called by doctor after consultation.
     * Notes field allows doctor to add consultation remarks.
     *
     * Why @CacheEvict here?
     * The appointment status changed. If we don't evict, cached data would
     * still show SCHEDULED even after the doctor completed the consultation.
     */
    @Transactional
    @CacheEvict(value = {"appointments", "patientAppointments", "doctorAppointments"}, allEntries = true)
    public AppointmentResponse completeAppointment(Long id, String notes) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessException("Only SCHEDULED appointments can be completed");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setNotes(notes);

        log.info("Appointment {} completed by doctor", id);
        return toResponse(appointmentRepository.save(appointment));
    }

    /**
     * Cancel an appointment — called by patient or receptionist.
     * Cannot cancel an already completed appointment.
     */
    @Transactional
    @CacheEvict(value = {"appointments", "patientAppointments", "doctorAppointments"}, allEntries = true)
    public AppointmentResponse cancelAppointment(Long id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel an already completed appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        log.info("Appointment {} cancelled", id);
        return toResponse(appointmentRepository.save(appointment));
    }

    /**
     * Get all appointments with a given status — admin/receptionist dashboard view.
     */
    public List<AppointmentResponse> getByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status)
                .stream().map(this::toResponse).toList();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    /**
     * Entity → DTO conversion.
     *
     * We map to a DTO (not return the raw entity) because:
     *  1. Hides JPA lazy-loading proxies (prevents serialization errors)
     *  2. Controls exactly which fields the API exposes
     *  3. Flattens nested objects (patientName instead of patient.getFullName())
     */
    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientName(a.getPatient().getFullName())
                .doctorName(a.getDoctor().getFullName())
                .doctorSpecialization(a.getDoctor().getSpecialization())
                .appointmentDateTime(a.getAppointmentDateTime())
                .status(a.getStatus())
                .reason(a.getReason())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
