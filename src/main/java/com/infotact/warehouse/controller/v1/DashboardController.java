package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.response.DashboardSummaryResponse;
import com.infotact.warehouse.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for retrieving warehouse analytics and operational metrics.
 * <p>
 * This controller serves as the primary data source for the application's landing page.
 * It provides a consolidated view of inventory levels, order statuses, and performance alerts.
 * Data is scoped to the authenticated user's assigned warehouse.
 * </p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "4. Dashboard & Analytics", description = "Consolidated data for real-time warehouse monitoring")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retrieves the high-level summary for the dashboard.
     * <p>
     * Logic: Aggregates counts for pending orders, low-stock products, and recent sales.
     * This endpoint is highly optimized using denormalized database links and caching
     * to ensure fast load times for the management team.
     * </p>
     *
     * @return A consolidated response containing metrics for stock, orders, and alerts.
     */
    @Operation(
            summary = "Get warehouse summary",
            description = "Fetches a comprehensive snapshot of warehouse operations, including stock alerts and order statuses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden: User does not have the required role")
    })
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}