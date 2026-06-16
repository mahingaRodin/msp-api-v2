package com.msp.payloads.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStoreRequest {

    @NotBlank(message = "Brand name is required")
    @Size(max = 255)
    private String brand;

    @Size(max = 100)
    private String storeType;

    @Size(max = 2000)
    private String description;

    // Contact info — all optional
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;
}
