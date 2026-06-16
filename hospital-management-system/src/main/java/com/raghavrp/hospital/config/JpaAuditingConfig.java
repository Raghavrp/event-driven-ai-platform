package com.raghavrp.hospital.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing — automatically populates @CreatedDate and
 * @LastModifiedDate fields on entities without any manual code.
 *
 * Used by: Appointment.createdAt and Appointment.updatedAt
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
