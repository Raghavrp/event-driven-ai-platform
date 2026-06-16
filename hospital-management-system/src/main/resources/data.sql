-- Seed data loaded automatically by Spring Boot on startup
-- This gives the demo some initial data to work with

-- Default admin user (password = "admin123" BCrypt encoded)
INSERT INTO app_user (id, username, password, role, full_name, email)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'ADMIN', 'System Admin', 'admin@hospital.com');

-- Default doctor user (password = "doctor123")
INSERT INTO app_user (id, username, password, role, full_name, email)
VALUES (2, 'drsmith', '$2a$10$8K1p/a0dhrxSA3VG5e9oSOl0lPGgwQjJ3HUGJIfNhMTgvfJ7GJWQO', 'DOCTOR', 'Dr. John Smith', 'dr.smith@hospital.com');

-- Default receptionist (password = "recept123")
INSERT INTO app_user (id, username, password, role, full_name, email)
VALUES (3, 'receptionist', '$2a$10$TKh8H1.PfunDpnuMEGrkFOuF3CGVaRJHbFEa03bMDGMFCopBRFE.y', 'RECEPTIONIST', 'Jane Doe', 'reception@hospital.com');

-- Sample doctors
INSERT INTO doctor (id, full_name, specialization, phone, email, available)
VALUES (1, 'Dr. John Smith', 'Cardiology', '9876543210', 'dr.smith@hospital.com', true);

INSERT INTO doctor (id, full_name, specialization, phone, email, available)
VALUES (2, 'Dr. Priya Nair', 'Neurology', '9876543211', 'dr.nair@hospital.com', true);

INSERT INTO doctor (id, full_name, specialization, phone, email, available)
VALUES (3, 'Dr. Ravi Kumar', 'Orthopedics', '9876543212', 'dr.ravi@hospital.com', false);

-- Sample patients
INSERT INTO patient (id, full_name, date_of_birth, gender, phone, email, blood_group, address)
VALUES (1, 'Ramesh Sharma', '1985-04-12', 'MALE', '9123456789', 'ramesh@gmail.com', 'O+', 'Delhi, India');

INSERT INTO patient (id, full_name, date_of_birth, gender, phone, email, blood_group, address)
VALUES (2, 'Sunita Verma', '1992-07-25', 'FEMALE', '9123456790', 'sunita@gmail.com', 'A+', 'Mumbai, India');

INSERT INTO patient (id, full_name, date_of_birth, gender, phone, email, blood_group, address)
VALUES (3, 'Arjun Mehta', '2001-11-03', 'MALE', '9123456791', 'arjun@gmail.com', 'B+', 'Pune, India');
