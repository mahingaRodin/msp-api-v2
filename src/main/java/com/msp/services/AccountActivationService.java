package com.msp.services;

import com.msp.enums.EActivationPurpose;
import com.msp.models.User;

public interface AccountActivationService {
    void sendActivation(User user, EActivationPurpose purpose);
    User activate(String token, String password);
}
