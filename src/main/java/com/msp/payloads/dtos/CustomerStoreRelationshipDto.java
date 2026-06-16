package com.msp.payloads.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What a store sees when viewing a customer in their portal.
 * Contains the customer's global profile + store-specific relationship data.
 */
@Data
public class CustomerStoreRelationshipDto {

    // ── Global customer identity ─────────────────────────────────────────────
    private UUID customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // ── Store-specific relationship data ─────────────────────────────────────
    private UUID relationshipId;
    private UUID storeId;
    private String storeName;
    private LocalDateTime firstInteractionAt;
    private LocalDateTime lastInteractionAt;
    private String notes;
}
