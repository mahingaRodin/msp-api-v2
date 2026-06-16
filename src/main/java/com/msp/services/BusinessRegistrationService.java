package com.msp.services;

import com.msp.enums.ERegistrationStatus;
import com.msp.payloads.dtos.TenantRegistrationDto;
import com.msp.payloads.request.BusinessRegistrationRequest;
import com.msp.payloads.response.ProvisioningResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BusinessRegistrationService {

    /**
     * Public endpoint — any applicant can submit.
     * Validates uniqueness of ownerEmail and businessName, persists TenantRegistration(PENDING).
     */
    TenantRegistrationDto submitRegistration(BusinessRegistrationRequest request);

    /**
     * Returns a single registration by ID.
     */
    TenantRegistrationDto getRegistration(UUID registrationId);

    /**
     * Lists registrations, optionally filtered by status. SUPER_ADMIN only.
     */
    Page<TenantRegistrationDto> listRegistrations(ERegistrationStatus status, Pageable pageable);

    /**
     * Approves a PENDING or UNDER_REVIEW registration.
     * Triggers tenant provisioning — creates Business + owner User.
     * SUPER_ADMIN only.
     */
    ProvisioningResponse approveRegistration(UUID registrationId, String adminNotes);

    /**
     * Rejects a PENDING or UNDER_REVIEW registration with a reason.
     * SUPER_ADMIN only.
     */
    TenantRegistrationDto rejectRegistration(UUID registrationId, String rejectionReason);

    /**
     * Marks a registration as UNDER_REVIEW (admin has started reviewing).
     * SUPER_ADMIN only.
     */
    TenantRegistrationDto markUnderReview(UUID registrationId);

    /**
     * Allows an applicant to update and resubmit a REJECTED registration.
     * Resets status back to PENDING for re-review.
     */
    TenantRegistrationDto resubmitRegistration(UUID registrationId, BusinessRegistrationRequest updated);

    /**
     * Appends an S3 key to the registration's document list.
     */
    void addDocumentKey(UUID registrationId, String s3Key);
}
