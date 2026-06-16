package com.msp.payloads.dtos;

import com.msp.enums.ERegistrationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TenantRegistrationDto {

    private UUID id;

    private String ownerFirstName;
    private String ownerLastName;
    private String ownerEmail;
    private String ownerPhone;

    private String businessName;
    private String legalName;
    private String registrationNumber;
    private String country;
    private String industry;
    private String businessDescription;

    private ERegistrationStatus status;
    private String adminNotes;
    private String rejectionReason;

    private UUID reviewedById;
    private String reviewedByName;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    /** Non-null once the registration has been approved and a Business provisioned. */
    private UUID provisionedTenantId;

    private java.util.List<String> documentS3Keys = new java.util.ArrayList<>();
}
