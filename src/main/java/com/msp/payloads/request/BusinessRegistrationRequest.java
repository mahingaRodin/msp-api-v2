package com.msp.payloads.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BusinessRegistrationRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String ownerFirstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String ownerLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String ownerEmail;

    @Size(max = 30)
    private String ownerPhone;

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 255, message = "Business name must be between 2 and 255 characters")
    private String businessName;

    @Size(max = 255)
    private String legalName;

    @Size(max = 100)
    private String registrationNumber;

    @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO code")
    private String country;

    @Size(max = 100)
    private String industry;

    @Size(max = 2000)
    private String businessDescription;
}
