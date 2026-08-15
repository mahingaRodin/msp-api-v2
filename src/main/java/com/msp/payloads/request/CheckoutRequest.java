package com.msp.payloads.request;

import com.msp.enums.EPaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckoutRequest {
    @NotNull
    private UUID branchId;
    @NotNull
    private EPaymentType paymentType;
}
