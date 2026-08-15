package com.msp.services;

import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(UserDto userDto);
    AuthResponse login(UserDto userDto);
    AuthResponse verifyOtp(String email, String otp);
    void resendOtp(String email);
    AuthResponse activateAccount(String token, String password);
}
