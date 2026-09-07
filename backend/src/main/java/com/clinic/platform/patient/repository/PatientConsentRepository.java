package com.clinic.platform.patient.repository;

import com.clinic.platform.patient.domain.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {
    List<PatientConsent> findByTenantIdAndPatientIdOrderByCreatedAtDesc(UUID tenantId, UUID patientId);
}
