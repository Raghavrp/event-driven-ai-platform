package com.raghavrp.hospital.repository;

import com.raghavrp.hospital.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Spring Data JPA generates the SQL automatically from the method name
    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhone(String phone);

    // Search patients by name (case-insensitive partial match)
    List<Patient> findByFullNameContainingIgnoreCase(String name);
}
