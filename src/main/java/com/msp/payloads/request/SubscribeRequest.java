package com.msp.payloads.request;

import com.msp.enums.ESubscriptionTier;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {
    @NotNull
    private ESubscriptionTier tier;
    private String cardBrand;
    private String cardHolderName;
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
}
