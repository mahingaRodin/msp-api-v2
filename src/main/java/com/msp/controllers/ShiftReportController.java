package com.msp.controllers;

import com.msp.payloads.dtos.ShiftReportDto;
import com.msp.services.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shift-reports")
@Tag(name = "Shift Report Management", description = "Endpoints for managing cashier shift reports and tracking work hours")
@SecurityRequirement(name = "Bearer Authentication")
public class ShiftReportController {
    private final ShiftService shiftReportService;

    @Operation(
            summary = "Start a new shift",
            description = "Starts a new shift for the currently authenticated cashier. Records start time and initializes shift data. Requires CASHIER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift started successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShiftReportDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot start shift - Cashier already has an active shift",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - CASHIER role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cashier or Branch not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping("/start")
    public ResponseEntity<ShiftReportDto> startShift() throws Exception {
        return ResponseEntity.ok(
                shiftReportService.startShift()
        );
    }

    @Operation(
            summary = "End current shift",
            description = "Ends the active shift for the currently authenticated cashier. Records end time and finalizes shift totals. Requires CASHIER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift ended successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShiftReportDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot end shift - No active shift found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - CASHIER role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PatchMapping("/end")
    public ResponseEntity<ShiftReportDto> endShift() throws Exception {
        return ResponseEntity.ok(
                shiftReportService.endShift(null, null)
        );
    }

    @Operation(
            summary = "Get current shift progress",
            description = "Retrieves the current active shift details for the authenticated cashier, including sales totals, refunds, and duration"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current shift details retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShiftReportDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No active shift found for current cashier",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - CASHIER role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/current")
    public ResponseEntity<ShiftReportDto> getCurrentShiftProgress() throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getCurrentShiftProgress(null)
        );
    }

    @Operation(
            summary = "Get shift report by cashier and date",
            description = "Retrieves a specific shift report for a cashier on a particular date"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift report retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShiftReportDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid cashier ID or date format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shift report not found for the given cashier and date",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/cashier/{cashierId}/by-date")
    public ResponseEntity<ShiftReportDto> getShiftReportByDate(
            @Parameter(
                    name = "cashierId",
                    description = "UUID of the cashier",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID cashierId,

            @Parameter(
                    name = "date",
                    description = "Date to retrieve shift report for (ISO format: yyyy-MM-dd)",
                    required = true,
                    example = "2024-01-15",
                    schema = @Schema(type = "string", format = "date")
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime date
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftByCashierAndDate(cashierId, date)
        );
    }

    @Operation(
            summary = "Get all shift reports by cashier",
            description = "Retrieves all shift reports for a specific cashier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift reports retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ShiftReportDto.class))
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
    public ResponseEntity<List<ShiftReportDto>> getShiftReportByCashier(
            @Parameter(
                    name = "cashierId",
                    description = "UUID of the cashier",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID cashierId
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftReportByCashierId(cashierId)
        );
    }

    @Operation(
            summary = "Get all shift reports by branch",
            description = "Retrieves all shift reports for a specific branch"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift reports retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ShiftReportDto.class))
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
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_MANAGER','ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_SUPER_ADMIN')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ShiftReportDto>> getShiftReportByBranch(
            @Parameter(
                    name = "branchId",
                    description = "UUID of the branch",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID branchId
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftReportByBranchId(branchId)
        );
    }

    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_MANAGER','ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_SUPER_ADMIN')")
    @GetMapping("/branch/{branchId}/open")
    public ResponseEntity<List<ShiftReportDto>> getOpenShifts(@PathVariable UUID branchId) {
        List<ShiftReportDto> open = shiftReportService.getShiftReportByBranchId(branchId).stream()
                .filter(s -> s.getShiftEnd() == null)
                .toList();
        return ResponseEntity.ok(open);
    }

    @Operation(
            summary = "Get shift report by ID",
            description = "Retrieves a specific shift report by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift report found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShiftReportDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid shift report ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shift report not found with the given ID",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ShiftReportDto> getShiftReportById(
            @Parameter(
                    name = "id",
                    description = "UUID of the shift report to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftReportById(id)
        );
    }


    @Operation(
            summary = "Get all shift reports with pagination",
            description = "Retrieves a paginated list of all shift reports in the system. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shift reports retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
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
    public ResponseEntity<Page<ShiftReportDto>> getAllShiftReports(
            @Parameter(
                    name = "page",
                    description = "Page number (0-based)",
                    example = "0"
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    name = "size",
                    description = "Number of items per page",
                    example = "10"
            )
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    name = "sortBy",
                    description = "Field to sort by",
                    example = "shiftStart",
                    schema = @Schema(allowableValues = {"id", "shiftStart", "shiftEnd", "totalSales", "totalRefunds"})
            )
            @RequestParam(defaultValue = "shiftStart") String sortBy,

            @Parameter(
                    name = "direction",
                    description = "Sort direction",
                    example = "DESC",
                    schema = @Schema(allowableValues = {"ASC", "DESC"})
            )
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ShiftReportDto> shiftReports = shiftReportService.getAllShiftReports(pageable);
        return ResponseEntity.ok(shiftReports);
    }
}