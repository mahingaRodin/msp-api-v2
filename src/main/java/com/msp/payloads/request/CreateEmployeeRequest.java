package com.msp.payloads.request;

import com.msp.enums.EUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 30)
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * Allowed: ROLE_STORE_MANAGER, ROLE_BRANCH_MANAGER, ROLE_BRANCH_CASHIER.
     * ROLE_STORE_ADMIN and ROLE_SUPER_ADMIN are forbidden.
     */
    @NotNull(message = "Role is required")
    private EUserRole role;

    /**
     * Required when role = ROLE_BRANCH_MANAGER or ROLE_BRANCH_CASHIER.
     */
    private UUID branchId;
}
