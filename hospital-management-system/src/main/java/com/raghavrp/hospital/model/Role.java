package com.raghavrp.hospital.model;

public enum Role {
    ADMIN,          // Full access — manage doctors, patients, all appointments
    DOCTOR,         // View own appointments, update appointment status
    RECEPTIONIST    // Book / cancel appointments, manage patients
}
