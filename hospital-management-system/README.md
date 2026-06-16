# Hospital Management System

A clean, production-style **Spring Boot** REST API for managing a hospital's patients, doctors, and appointments.

Built to demonstrate: **JWT Security · H2 Database · Redis Caching · Swagger UI · Exception Handling · JPA Auditing · Role-Based Access Control**

---

## What This Project Does

```
┌─────────────────────────────────────────────────────────────────────┐
│                   HOSPITAL MANAGEMENT SYSTEM                         │
│                                                                       │
│  ┌────────────┐   JWT    ┌──────────────────────────────────────┐   │
│  │   Client   │────────► │           REST Controllers            │   │
│  │ (Swagger / │◄────────  Auth · Patient · Doctor · Appointment  │   │
│  │  Postman)  │          └──────────────┬───────────────────────┘   │
│  └────────────┘                         │                            │
│                               ┌─────────▼──────────┐               │
│                               │      Services       │               │
│                               │  Business Logic +   │               │
│                               │  Cache Annotations  │               │
│                               └────┬──────────┬────┘               │
│                                    │          │                      │
│                          ┌─────────▼──┐  ┌───▼──────────┐          │
│                          │  H2 (JPA)  │  │  Redis Cache  │          │
│                          │  Database  │  │  (10 min TTL) │          │
│                          └────────────┘  └───────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 | Latest LTS — records, sealed classes, modern APIs |
| Framework | Spring Boot 3.3.5 | Auto-configuration, embedded Tomcat |
| Database | H2 (in-memory) | Zero setup — great for demos; swap to MySQL easily |
| ORM | Spring Data JPA + Hibernate | Auto-generate SQL, simple repository queries |
| Cache | Redis + Spring Cache | `@Cacheable` reduces DB calls for frequent reads |
| Security | Spring Security + JWT | Stateless auth, role-based access |
| API Docs | Swagger / SpringDoc OpenAPI | Interactive UI to test all endpoints |
| Build | Maven | Dependency management |

---

## Features

### Patient Management
- Register new patient (duplicate email check)
- Search by name (partial, case-insensitive)
- Update patient details
- Delete patient (ADMIN only)
- **Redis cached** by patient ID

### Doctor Management
- Add / update doctors
- Toggle availability (on leave ↔ active)
- Find by specialization
- **Redis cached** available doctors list

### Appointment Booking
- Book appointment with business rule validation:
  - Doctor must be available
  - Time slot must be free
  - Date must be in the future
- View appointments by patient / doctor / status
- Complete appointment (doctor adds consultation notes)
- Cancel appointment
- **Redis cached** by patient ID and doctor ID

### Security
- JWT authentication (24-hour token)
- Role-based access: `ADMIN` · `DOCTOR` · `RECEPTIONIST`
- Method-level security with `@PreAuthorize`

### Developer Experience
- Swagger UI for exploring and testing all endpoints
- H2 Console to inspect the database visually
- Seed data loaded automatically on startup
- JPA Auditing (`createdAt`, `updatedAt` auto-filled)

---

## Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- Redis (optional — app falls back to simple cache if Redis is unavailable)

### Run the application
```bash
cd hospital-management-system
mvn spring-boot:run
```

### Access points

| URL | Purpose |
|---|---|
| `http://localhost:8081/swagger-ui.html` | **Swagger UI** — test all APIs here |
| `http://localhost:8081/h2-console` | **H2 Database Console** — view tables |
| `http://localhost:8081/actuator/health` | Health check |

---

## How to Use (Step by Step)

### Step 1 — Login and get JWT token

Open Swagger UI → `POST /api/auth/login`

```json
{ "username": "admin", "password": "admin123" }
```

Copy the `token` from the response.

### Step 2 — Authorize in Swagger

Click **"Authorize"** button (top right) → paste the token → click Authorize.

All subsequent API calls will include the JWT automatically.

### Step 3 — Book an appointment

```
POST /api/appointments
{
  "patientId": 1,
  "doctorId": 1,
  "appointmentDateTime": "2026-09-15T10:30:00",
  "reason": "Annual checkup"
}
```

### Step 4 — View appointments

```
GET /api/appointments/patient/1   → all appointments for patient 1
GET /api/appointments/doctor/1    → doctor's schedule
GET /api/appointments/status/SCHEDULED → all upcoming
```

### Step 5 — Complete appointment (as doctor)

```
PATCH /api/appointments/1/complete?notes=Patient+is+recovering+well
```

---

## Demo Users (pre-loaded)

| Username | Password | Role | Permissions |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Full access |
| `drsmith` | `doctor123` | DOCTOR | View/complete appointments |
| `receptionist` | `recept123` | RECEPTIONIST | Book/cancel, manage patients |

---

## API Reference

### Authentication
| Method | URL | Access | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Get JWT token |

### Patients
| Method | URL | Role | Description |
|---|---|---|---|
| POST | `/api/patients` | ADMIN, RECEPT | Register patient |
| GET | `/api/patients/{id}` | ALL | Get by ID (cached) |
| GET | `/api/patients` | ALL | Get all |
| GET | `/api/patients/search?name=` | ALL | Search by name |
| PUT | `/api/patients/{id}` | ADMIN, RECEPT | Update |
| DELETE | `/api/patients/{id}` | ADMIN | Delete |

### Doctors
| Method | URL | Role | Description |
|---|---|---|---|
| POST | `/api/doctors` | ADMIN | Add doctor |
| GET | `/api/doctors/{id}` | ALL | Get by ID (cached) |
| GET | `/api/doctors/available` | ALL | Available doctors (cached) |
| GET | `/api/doctors/specialization/{spec}` | ALL | By specialization |
| PATCH | `/api/doctors/{id}/toggle-availability` | ADMIN | Toggle on leave |
| PUT | `/api/doctors/{id}` | ADMIN | Update doctor |

### Appointments
| Method | URL | Role | Description |
|---|---|---|---|
| POST | `/api/appointments` | ADMIN, RECEPT | Book appointment |
| GET | `/api/appointments/{id}` | ALL | Get by ID |
| GET | `/api/appointments/patient/{id}` | ALL | Patient's appointments (cached) |
| GET | `/api/appointments/doctor/{id}` | ALL | Doctor's schedule (cached) |
| GET | `/api/appointments/status/{status}` | ADMIN, RECEPT | Filter by status |
| PATCH | `/api/appointments/{id}/complete` | ADMIN, DOCTOR | Complete |
| PATCH | `/api/appointments/{id}/cancel` | ALL ROLES | Cancel |

---

## Project Structure

```
hospital-management-system/
├── src/main/java/com/raghavrp/hospital/
│   ├── HospitalManagementApplication.java   ← main class
│   ├── config/
│   │   ├── SwaggerConfig.java               ← OpenAPI + JWT auth button
│   │   └── JpaAuditingConfig.java           ← auto createdAt/updatedAt
│   ├── controller/                          ← thin layer, only routing
│   │   ├── AuthController.java
│   │   ├── PatientController.java
│   │   ├── DoctorController.java
│   │   └── AppointmentController.java
│   ├── service/                             ← business logic + caching
│   │   ├── AuthService.java
│   │   ├── PatientService.java
│   │   ├── DoctorService.java
│   │   └── AppointmentService.java          ← most important service
│   ├── model/                               ← JPA entities
│   │   ├── Patient.java
│   │   ├── Doctor.java
│   │   ├── Appointment.java
│   │   ├── AppUser.java                     ← implements UserDetails
│   │   ├── AppointmentStatus.java
│   │   ├── Gender.java
│   │   └── Role.java
│   ├── repository/                          ← Spring Data JPA
│   ├── dto/                                 ← request/response objects
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java      ← central error handling
│   │   ├── ResourceNotFoundException.java
│   │   ├── BusinessException.java
│   │   └── ErrorResponse.java
│   └── security/
│       ├── SecurityConfig.java
│       ├── JwtService.java
│       └── JwtAuthFilter.java
└── src/main/resources/
    ├── application.yml                      ← all config
    └── data.sql                             ← seed data
```

---

## Key Design Decisions

| Decision | Explanation |
|---|---|
| **H2 over MySQL** | Zero setup for demo/interview. Change `application.yml` datasource to use MySQL/PostgreSQL in production |
| **Redis caching** | Patients and doctors are read-heavy but change rarely — caching reduces DB load |
| **DTO pattern** | Never expose JPA entities directly — prevents lazy-load issues and controls the API contract |
| **@Transactional on writes** | All service write methods are transactional — partial failures roll back completely |
| **@PreAuthorize** | Method-level security — each API clearly declares who can call it |
| **GlobalExceptionHandler** | No try-catch in controllers — all exceptions handled in one central place |
| **JPA Auditing** | `createdAt`/`updatedAt` filled automatically — no boilerplate in service layer |

---

## Caching Summary

| Cache Name | Key | TTL | Evicted When |
|---|---|---|---|
| `patients` | patientId | 10 min | Patient updated or deleted |
| `doctors` | doctorId | 10 min | Doctor updated |
| `availableDoctors` | "available" | 10 min | Any doctor availability changes |
| `appointments` | appointmentId | 10 min | Status changes |
| `patientAppointments` | patientId | 10 min | New booking or cancellation |
| `doctorAppointments` | doctorId | 10 min | New booking or cancellation |

---

*Part of the event-driven-ai-platform repository — Raghwendra Pratap Singh*
