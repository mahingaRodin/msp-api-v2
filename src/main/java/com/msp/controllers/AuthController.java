package com.msp.controllers;

import com.msp.exceptions.UserException;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.response.AuthResponse;
import com.msp.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs for user signup and login")
public class AuthController {
    private final AuthService authsService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided information. Returns authentication token upon successful registration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input - User already exists or validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signupHandler(
            @Parameter(
                    description = "User registration details",
                    required = true,
                    schema = @Schema(implementation = UserDto.class)
            )
            @Valid @RequestBody UserDto userDto
    ) throws UserException {
        return ResponseEntity.ok(
                authsService.signup(userDto)
        );
    }

    @Operation(
            summary = "User login",
            description = "Authenticates a user with email/username and password. Returns authentication token upon successful login."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials - Wrong email/username or password",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginHandler(
            @Valid @RequestBody UserDto userDto
    ) throws UserException {
        return ResponseEntity.ok(authsService.login(userDto));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody com.msp.payloads.request.VerifyOtpRequest body) {
        return ResponseEntity.ok(authsService.verifyOtp(body.getEmail(), body.getOtp()));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<com.msp.payloads.response.ApiResponse2> resendOtp(
            @Valid @RequestBody com.msp.payloads.request.ResendOtpRequest body) {
        authsService.resendOtp(body.getEmail());
        com.msp.payloads.response.ApiResponse2 res = new com.msp.payloads.response.ApiResponse2();
        res.setMessage("A new code was sent to your email.");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/activate")
    public ResponseEntity<AuthResponse> activate(
            @Valid @RequestBody com.msp.payloads.request.ActivateAccountRequest body) {
        if (!body.getPassword().equals(body.getConfirmPassword())) {
            throw new UserException("Passwords do not match");
        }
        return ResponseEntity.ok(authsService.activateAccount(body.getToken(), body.getPassword()));
    }
}