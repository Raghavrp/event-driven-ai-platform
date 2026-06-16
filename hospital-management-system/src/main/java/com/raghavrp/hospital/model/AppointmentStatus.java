package com.raghavrp.hospital.model;

public enum AppointmentStatus {
    SCHEDULED,    // Booking confirmed, not yet visited
    COMPLETED,    // Doctor has seen the patient
    CANCELLED,    // Cancelled by patient or staff
    NO_SHOW       // Patient did not show up
}
