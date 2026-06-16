package com.msp.payloads.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlResult {
    private String url;
    private long expiresAtEpochMilli;
}
