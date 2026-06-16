package com.msp.payloads.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateBranchRequest {

    @NotBlank(message = "Branch name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String address;

    @Size(max = 30)
    private String phone;

    private String email;

    /**
     * The store this branch belongs to.
     * Must be owned by the calling owner's tenant.
     */
    @NotNull(message = "storeId is required")
    private UUID storeId;

    private List<String> workingDays;
    private LocalTime openTime;
    private LocalTime closeTime;
}
