package com.raghavrp.hospital.service;

import com.raghavrp.hospital.dto.DoctorRequest;
import com.raghavrp.hospital.exception.ResourceNotFoundException;
import com.raghavrp.hospital.model.Doctor;
import com.raghavrp.hospital.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DoctorService — manages doctor records and availability.
 *
 * Caching strategy for doctors:
 *  - Individual doctors cached by ID (frequently fetched in appointment flow)
 *  - Available doctors list cached (receptionist fetches this repeatedly)
 *  - Cache cleared when a doctor's availability changes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional
    public Doctor addDoctor(DoctorRequest request) {
        Doctor doctor = Doctor.builder()
                .fullName(request.getFullName())
                .specialization(request.getSpecialization())
                .phone(request.getPhone())
                .email(request.getEmail())
                .available(request.isAvailable())
                .build();

        Doctor saved = doctorRepository.save(doctor);
        log.info("Doctor added: id={} name={} specialization={}", saved.getId(), saved.getFullName(), saved.getSpecialization());
        return saved;
    }

    /**
     * Get doctor by ID.
     * Cached per doctor ID — reduces DB calls in the appointment booking flow
     * where we validate the doctor exists on every booking.
     */
    @Cacheable(value = "doctors", key = "#id")
    public Doctor getDoctorById(Long id) {
        log.debug("Cache MISS — loading doctor {} from DB", id);
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Get all currently available doctors.
     *
     * Cached as a list under key "available".
     * Why? Receptionist opens the booking screen → this list loads → should be fast.
     * Cache is evicted when any doctor's availability changes (see toggleAvailability).
     */
    @Cacheable(value = "availableDoctors", key = "'available'")
    public List<Doctor> getAvailableDoctors() {
        log.debug("Cache MISS — loading available doctors from DB");
        return doctorRepository.findByAvailableTrue();
    }

    /**
     * Find doctors by specialization — useful when patient says "I need a cardiologist".
     */
    public List<Doctor> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(specialization);
    }

    /**
     * Toggle doctor availability (on leave ↔ active).
     *
     * @CacheEvict with allEntries=true — clears the entire "availableDoctors" cache
     * because the available list has changed. Also evicts the individual doctor cache.
     *
     * Why allEntries? The cached list "available" needs to be rebuilt to reflect the change.
     */
    @Transactional
    @CacheEvict(value = {"doctors", "availableDoctors"}, allEntries = true)
    public Doctor toggleAvailability(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));

        doctor.setAvailable(!doctor.isAvailable());
        Doctor updated = doctorRepository.save(doctor);
        log.info("Doctor {} availability set to {}", id, updated.isAvailable());
        return updated;
    }

    @Transactional
    @CachePut(value = "doctors", key = "#id")
    public Doctor updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));

        doctor.setFullName(request.getFullName());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setPhone(request.getPhone());
        doctor.setEmail(request.getEmail());
        doctor.setAvailable(request.isAvailable());

        return doctorRepository.save(doctor);
    }
}
