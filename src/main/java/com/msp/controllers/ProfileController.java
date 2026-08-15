package com.msp.controllers;

import com.msp.exceptions.UserException;
import com.msp.mappers.UserMapper;
import com.msp.models.User;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.request.ChangePasswordRequest;
import com.msp.payloads.request.ProfileUpdateRequest;
import com.msp.payloads.response.ApiResponse2;
import com.msp.repositories.UserRepository;
import com.msp.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(UserMapper.toDTO(userService.getCurrentUser()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDto> update(@RequestBody ProfileUpdateRequest body) {
        User user = userService.getCurrentUser();
        if (body.getFirstName() != null) user.setFirstName(body.getFirstName());
        if (body.getLastName() != null) user.setLastName(body.getLastName());
        if (body.getPhone() != null) user.setPhone(body.getPhone());
        if (body.getProfilePicture() != null) user.setProfilePicture(body.getProfilePicture());
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(UserMapper.toDTO(userRepo.save(user)));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse2> changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        User user = userService.getCurrentUser();
        if (!passwordEncoder.matches(body.getCurrentPassword(), user.getPassword())) {
            throw new UserException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(body.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Password updated");
        return ResponseEntity.ok(res);
    }
}
