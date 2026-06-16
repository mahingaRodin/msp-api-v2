package com.msp.payloads.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CustomerRegistrationResponse {
    private UUID customerId;
    private String email;
    private String message;
}
