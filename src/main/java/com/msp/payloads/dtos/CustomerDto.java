package com.msp.payloads.dtos;

import com.msp.enums.EUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private UUID id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private EUserRole role;
    private String phone;

    /** How many stores this customer has interacted with. Used in profile view. */
    private long storeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
