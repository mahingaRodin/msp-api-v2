package com.msp.payloads.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivateAccountRequest {
    @NotBlank
    private String token;
    @NotBlank @Size(min = 6)
    private String password;
    @NotBlank @Size(min = 6)
    private String confirmPassword;
}
