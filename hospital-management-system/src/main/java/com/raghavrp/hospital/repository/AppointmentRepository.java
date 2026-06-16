package com.raghavrp.hospital.repository;

import com.raghavrp.hospital.model.Appointment;
import com.raghavrp.hospital.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // All appointments for a specific patient
    List<Appointment> findByPatientId(Long patientId);

    // All appointments for a specific doctor
    List<Appointment> findByDoctorId(Long doctorId);

    // Appointments by status (e.g., all SCHEDULED appointments)
    List<Appointment> findByStatus(AppointmentStatus status);

    // Check if a doctor already has an appointment at a given time (conflict check)
    boolean existsByDoctorIdAndAppointmentDateTimeAndStatusNot(
            Long doctorId, LocalDateTime dateTime, AppointmentStatus status);

    // Custom JPQL query — appointments for a doctor between two timestamps
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDateTime BETWEEN :from AND :to")
    List<Appointment> findByDoctorAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
