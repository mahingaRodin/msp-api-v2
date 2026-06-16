package com.msp.payloads.dtos;

import com.msp.enums.EBusinessStatus;
import com.msp.enums.ESubscriptionTier;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BusinessDto {

    private UUID id;
    private UUID tenantId;

    private String businessName;
    private String legalName;
    private String registrationNumber;
    private String country;
    private String industry;
    private String description;

    private ESubscriptionTier subscriptionTier;
    private EBusinessStatus status;

    private UUID ownerUserId;
    private String ownerEmail;
    private String ownerFullName;

    private LocalDateTime trialEndsAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
