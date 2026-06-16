package com.raghavrp.hospital.service;

import com.raghavrp.hospital.dto.PatientRequest;
import com.raghavrp.hospital.exception.BusinessException;
import com.raghavrp.hospital.exception.ResourceNotFoundException;
import com.raghavrp.hospital.model.Patient;
import com.raghavrp.hospital.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PatientService — all business logic for patient management.
 *
 * Why a Service layer?
 *  - Keeps controllers thin (controllers only route, services decide)
 *  - @Transactional boundaries live here, not in controllers
 *  - Caching annotations work at this layer (Spring AOP proxy)
 *  - Business rules (duplicate check, validation) belong here
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    /**
     * Register a new patient.
     *
     * Why duplicate check?
     * A patient with the same email or phone is very likely a duplicate registration.
     * We catch this early rather than relying on DB unique constraints (better error message).
     */
    @Transactional
    public Patient registerPatient(PatientRequest request) {
        // Guard: prevent duplicate registration by email
        if (patientRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("A patient with email " + request.getEmail() + " already exists");
        }

        Patient patient = Patient.builder()
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .phone(request.getPhone())
                .email(request.getEmail())
                .bloodGroup(request.getBloodGroup())
                .address(request.getAddress())
                .build();

        Patient saved = patientRepository.save(patient);
        log.info("New patient registered: id={} name={}", saved.getId(), saved.getFullName());
        return saved;
    }

    /**
     * Get patient by ID.
     *
     * @Cacheable("patients") — Spring checks Redis first.
     *   - Cache HIT  → returns from Redis (~0.5ms), no DB call
     *   - Cache MISS → hits H2 DB, then stores result in Redis for next time
     *
     * Why cache patients? Patient data changes infrequently but is read often
     * (every appointment lookup loads the patient). Caching reduces DB load.
     */
    @Cacheable(value = "patients", key = "#id")
    public Patient getPatientById(Long id) {
        log.debug("Cache MISS — loading patient {} from DB", id);
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    /**
     * Get all patients — no cache here intentionally.
     * Full list can change frequently (new registrations), and caching large lists
     * wastes Redis memory. Better to cache individual records.
     */
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    /**
     * Search patients by name (partial, case-insensitive).
     * Used by receptionist to find a patient by typing partial name.
     */
    public List<Patient> searchByName(String name) {
        return patientRepository.findByFullNameContainingIgnoreCase(name);
    }

    /**
     * Update patient details.
     *
     * @CachePut — updates the record in both DB AND Redis cache.
     * This keeps the cache consistent without requiring a separate evict+reload.
     */
    @Transactional
    @CachePut(value = "patients", key = "#id")
    public Patient updatePatient(Long id, PatientRequest request) {
        Patient existing = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        existing.setFullName(request.getFullName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setAddress(request.getAddress());
        existing.setBloodGroup(request.getBloodGroup());

        log.info("Patient updated: id={}", id);
        return patientRepository.save(existing);
    }

    /**
     * Delete patient.
     *
     * @CacheEvict — removes the patient entry from Redis when deleted.
     * Without this, cache would return a deleted patient until TTL expires.
     */
    @Transactional
    @CacheEvict(value = "patients", key = "#id")
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient not found with id: " + id);
        }
        patientRepository.deleteById(id);
        log.info("Patient deleted: id={}", id);
    }
}
