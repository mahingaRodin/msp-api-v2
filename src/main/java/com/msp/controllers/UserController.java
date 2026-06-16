package com.msp.controllers;

import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.mappers.UserMapper;
import com.msp.models.User;
import com.msp.payloads.dtos.UpdateUserDto;
import com.msp.payloads.dtos.UserDto;
import com.msp.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user profiles and information")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Get all users with pagination and optional status filter",
            description = "Retrieves a paginated list of users. Can filter by status (ACTIVE, SUSPENDED, DISCHARGED). Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_ADMIN")
    })
    @GetMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Filter by status (ACTIVE, SUSPENDED, DISCHARGED)", example = "ACTIVE")
            @RequestParam(required = false) EUserStatus status
    ) {
        Page<UserDto> usersPage = userService.getAllUsers(page, size, sortBy, status);
        return ResponseEntity.ok(usersPage);
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable UUID id
    ) throws UserException {
        User user = userService.getUserById(id);
        if (user == null) {
            throw new UserException("User Not Found!");
        }
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @Operation(summary = "Check if user can be safely deleted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}/can-delete")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Boolean> canDeleteUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.canDeleteUser(id));
    }

    @Operation(summary = "Create new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> createUser(
            @RequestBody @Valid UserDto userDto
    ) throws UserException {
        User created = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toDTO(created));
    }

    @Operation(summary = "Update user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserDto userDto
    ) throws UserException {
        User updated = userService.updateUser(id, userDto);
        return ResponseEntity.ok(UserMapper.toDTO(updated));
    }

    @Operation(summary = "Discharge (fire) a user", description = "Permanently deactivates user. Store/Branch links retained for history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User discharged"),
            @ApiResponse(responseCode = "400", description = "User already discharged"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/discharge")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> dischargeUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason
    ) throws UserException {
        User discharged = userService.dischargeUser(id, reason);
        return ResponseEntity.ok(UserMapper.toDTO(discharged));
    }

    @Operation(summary = "Suspend a user", description = "Temporarily blocks user access. Can be reactivated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User suspended"),
            @ApiResponse(responseCode = "400", description = "User already suspended or discharged"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> suspendUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason
    ) throws UserException {
        User suspended = userService.suspendUser(id, reason);
        return ResponseEntity.ok(UserMapper.toDTO(suspended));
    }

    @Operation(summary = "Activate/Reactivate a user", description = "Restores SUSPENDED user to ACTIVE. Cannot reactivate DISCHARGED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User activated"),
            @ApiResponse(responseCode = "400", description = "User already active or discharged"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> activateUser(
            @PathVariable UUID id
    ) throws UserException {
        User activated = userService.activateUser(id);
        return ResponseEntity.ok(UserMapper.toDTO(activated));
    }

    @Operation(summary = "Delete user permanently", description = "Only allowed for users not linked to stores/branches. Use discharge for linked users.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "400", description = "User linked to store/branch - use discharge instead"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id
    ) throws UserException {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get total users count",
            description = "Returns total number of users, optionally filtered by status"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    @GetMapping("/count")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Long> getUsersCount(
            @Parameter(description = "Filter by status (ACTIVE, SUSPENDED, DISCHARGED)")
            @RequestParam(required = false) EUserStatus status
    ) {

        Page<UserDto> usersPage =
                userService.getAllUsers(0, 1, "id", status);

        long totalUsers = usersPage.getTotalElements();

        return ResponseEntity.ok(totalUsers);
    }
}