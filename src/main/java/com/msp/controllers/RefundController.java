package com.msp.controllers;

import com.msp.payloads.dtos.RefundDto;
import com.msp.services.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/refunds")
@Tag(name = "Refund Management", description = "Endpoints for managing customer refunds and returns")
@SecurityRequirement(name = "Bearer Authentication")
public class RefundController {
    private final RefundService refundService;

    @Operation(
            summary = "Create a new refund",
            description = "Creates a new refund for an order. Requires CASHIER or MANAGER role. Validates that the order exists and is eligible for refund."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refund created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefundDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data - Amount exceeds order total or validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions (CASHIER or MANAGER required)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found or no active shift report for cashier",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Refund already processed for this order",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<RefundDto> createRefund(
            @Parameter(
                    description = "Refund details including order ID, amount, and reason",
                    required = true,
                    schema = @Schema(implementation = RefundDto.class)
            )
            @Valid @RequestBody RefundDto refundDto
    ) throws Exception {
        RefundDto refund = refundService.createRefund(refundDto);
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/branch/{branchId}/pending")
    public ResponseEntity<Page<RefundDto>> pendingReturns(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) throws Exception {
        return ResponseEntity.ok(refundService.getPendingReturnsForBranch(branchId, page, size));
    }

    @PostMapping("/{id}/approve-return")
    public ResponseEntity<RefundDto> approveReturn(@PathVariable UUID id) throws Exception {
        return ResponseEntity.ok(refundService.approveReturnAndRefund(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RefundDto> rejectReturn(
            @PathVariable UUID id,
            @RequestBody(required = false) java.util.Map<String, String> body) throws Exception {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(refundService.rejectRefundRequest(id, reason));
    }

    @Operation(
            summary = "Get all refunds",
            description = "Retrieves a list of all refunds in the system. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refunds retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RefundDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN or MANAGER role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<Page<RefundDto>> getAllRefunds(int page, int size) throws Exception {
        Page<RefundDto> refunds = refundService.getAllRefunds(page, size);
        return ResponseEntity.ok(refunds);
    }

    @Operation(
            summary = "Get refunds by cashier",
            description = "Retrieves all refunds processed by a specific cashier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refunds retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RefundDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid cashier ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cashier not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<Page<RefundDto>> getRefundByCashierId(
            @Parameter(
                    name = "cashierId",
                    description = "UUID of the cashier who processed the refunds",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID cashierId,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size
            ) throws Exception {
        Page<RefundDto> refunds = refundService.getRefundByCashier(cashierId,page,size);
        return ResponseEntity.ok(refunds);
    }

    @Operation(
            summary = "Get refunds by branch",
            description = "Retrieves all refunds processed at a specific branch"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refunds retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RefundDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid branch ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Branch not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Page<RefundDto>> getRefundByBranchId(
            @Parameter(
                    name = "branchId",
                    description = "UUID of the branch where refunds were processed",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size
    ) throws Exception {
        Page<RefundDto> refunds = refundService.getRefundByBranch(branchId,page,size);
        return ResponseEntity.ok(refunds);
    }

    @Operation(
            summary = "Get refunds by shift report",
            description = "Retrieves all refunds associated with a specific shift report"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refunds retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RefundDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid shift ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shift report not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<Page<RefundDto>> getRefundByShiftId(
            @Parameter(
                    name = "shiftId",
                    description = "UUID of the shift report",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID shiftId,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size
    ) throws Exception {
        Page<RefundDto> refunds = refundService.getRefundByShiftReport(shiftId,page,size);
        return ResponseEntity.ok(refunds);
    }

    @Operation(
            summary = "Get refunds by cashier and date range",
            description = "Retrieves refunds processed by a specific cashier within a specified date range"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refunds retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RefundDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid cashier ID format or date range parameters",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cashier not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/cashier/{cashierId}/range")
    public ResponseEntity<Page<RefundDto>> getRefundByCashierAndDateRange(
            @Parameter(
                    name = "cashierId",
                    description = "UUID of the cashier",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID cashierId,

            @Parameter(
                    name = "startDate",
                    description = "Start date for the range (ISO format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z')",
                    required = true,
                    example = "2024-01-01T00:00:00.000Z"
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(
                    name = "endDate",
                    description = "End date for the range (ISO format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z')",
                    required = true,
                    example = "2024-01-31T23:59:59.999Z"
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size
    ) throws Exception {
        Page<RefundDto> refund = refundService.getRefundByCashierAndDateRange(
                cashierId,
                startDate,
                endDate,
                page,
                size
        );
        return ResponseEntity.ok(refund);
    }

    @Operation(
            summary = "Get refund by ID",
            description = "Retrieves detailed information of a specific refund by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refund found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefundDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid refund ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Refund not found with the given ID",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<RefundDto> getRefundById(
            @Parameter(
                    name = "id",
                    description = "UUID of the refund to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id
    ) throws Exception {
        RefundDto refund = refundService.getRefundById(id);
        return ResponseEntity.ok(refund);
    }
}