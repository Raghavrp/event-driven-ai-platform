package com.raghavrp.hospital.repository;

import com.raghavrp.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Find all doctors for a given specialization
    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    // Find only available doctors (not on leave)
    List<Doctor> findByAvailableTrue();

    // Find available doctors in a specific specialization
    List<Doctor> findBySpecializationIgnoreCaseAndAvailableTrue(String specialization);
}
