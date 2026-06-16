package com.raghavrp.hospital;

import com.raghavrp.hospital.dto.AppointmentRequest;
import com.raghavrp.hospital.exception.BusinessException;
import com.raghavrp.hospital.exception.ResourceNotFoundException;
import com.raghavrp.hospital.model.*;
import com.raghavrp.hospital.repository.*;
import com.raghavrp.hospital.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @Mock DoctorRepository doctorRepository;
    @InjectMocks AppointmentService appointmentService;

    @Test
    void shouldThrowWhenPatientNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(99L);
        req.setDoctorId(1L);
        req.setAppointmentDateTime(LocalDateTime.now().plusDays(1));
        req.setReason("Checkup");

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void shouldThrowWhenDoctorUnavailable() {
        Patient patient = Patient.builder().id(1L).fullName("Test Patient").build();
        Doctor doctor = Doctor.builder().id(1L).fullName("Dr. Test").available(false).build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(1L);
        req.setDoctorId(1L);
        req.setAppointmentDateTime(LocalDateTime.now().plusDays(1));
        req.setReason("Checkup");

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void shouldThrowWhenSlotAlreadyTaken() {
        Patient patient = Patient.builder().id(1L).fullName("Test Patient").build();
        Doctor doctor = Doctor.builder().id(1L).fullName("Dr. Test").available(true).build();
        LocalDateTime slot = LocalDateTime.now().plusDays(1);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatusNot(
                1L, slot, AppointmentStatus.CANCELLED)).thenReturn(true);

        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(1L);
        req.setDoctorId(1L);
        req.setAppointmentDateTime(slot);
        req.setReason("Checkup");

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an appointment");
    }

    @Test
    void shouldBookSuccessfully() {
        Patient patient = Patient.builder().id(1L).fullName("Ramesh Sharma").build();
        Doctor doctor = Doctor.builder().id(1L).fullName("Dr. Smith")
                .specialization("Cardiology").available(true).build();
        LocalDateTime slot = LocalDateTime.now().plusDays(1);

        Appointment saved = Appointment.builder()
                .id(10L).patient(patient).doctor(doctor)
                .appointmentDateTime(slot).status(AppointmentStatus.SCHEDULED)
                .reason("Chest pain").build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatusNot(
                any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any())).thenReturn(saved);

        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(1L);
        req.setDoctorId(1L);
        req.setAppointmentDateTime(slot);
        req.setReason("Chest pain");

        var response = appointmentService.bookAppointment(req);

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.getPatientName()).isEqualTo("Ramesh Sharma");
        assertThat(response.getDoctorName()).isEqualTo("Dr. Smith");
    }
}
