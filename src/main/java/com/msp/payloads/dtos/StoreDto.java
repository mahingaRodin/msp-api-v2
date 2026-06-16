package com.msp.payloads.dtos;

import com.msp.enums.EStoreStatus;
import com.msp.models.StoreContact;
import com.msp.models.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StoreDto {
    private UUID id;
    private String brand;
    private UserDto storeAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    private String storeType;
    private EStoreStatus status;
    private StoreContact contact;
    private UUID tenantId;
}
