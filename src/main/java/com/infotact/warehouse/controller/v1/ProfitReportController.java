package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.response.ProductProfitResponse;
import com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse;
import com.infotact.warehouse.service.ProfitReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for profit reports.
 *
 * <pre>
 * Base path: /api/v1/reports/profit
 *
 * GET /weekly?year=2026                       — warehouse-wide weekly breakdown
 * GET /monthly?year=2026                      — warehouse-wide monthly breakdown
 * GET /yearly                                 — warehouse-wide across all years
 * GET /products/{productId}/weekly?year=2026  — per-product weekly breakdown
 * GET /products/{productId}/monthly?year=2026 — per-product monthly breakdown
 * GET /products/{productId}/yearly            — per-product across all years
 * GET /summary?year=2026&month=0              — top products ranked by profit
 * </pre>
 *
 * All endpoints restricted to ADMIN and MANAGER roles.
 */
@RestController
@RequestMapping("/api/v1/reports/profit")
@RequiredArgsConstructor
@Tag(name = "Profit Reports", description = "Warehouse and per-product profit reporting")
public class ProfitReportController {

    private final ProfitReportService profitReportService;

    private int currentYear() {
        return LocalDate.now().getYear();
    }

    // ── Warehouse-wide ────────────────────────────────────────────────────────

    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Weekly profit breakdown",
            description = "Returns profit split by owned vs consignment for each week of the given year. Defaults to the current year."
    )
    public ResponseEntity<List<ProfitPeriodResponse>> weeklyProfit(
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = currentYear();
        return ResponseEntity.ok(profitReportService.getWeeklyProfit(year));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Monthly profit breakdown",
            description = "Returns profit split by owned vs consignment for each month of the given year. Defaults to the current year."
    )
    public ResponseEntity<List<ProfitPeriodResponse>> monthlyProfit(
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = currentYear();
        return ResponseEntity.ok(profitReportService.getMonthlyProfit(year));
    }

    @GetMapping("/yearly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Yearly profit breakdown",
            description = "Returns profit split by owned vs consignment across all years on record."
    )
    public ResponseEntity<List<ProfitPeriodResponse>> yearlyProfit() {
        return ResponseEntity.ok(profitReportService.getYearlyProfit());
    }

    // ── Per product ───────────────────────────────────────────────────────────

    @GetMapping("/products/{productId}/weekly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Weekly profit for a specific product")
    public ResponseEntity<List<ProductProfitResponse>> weeklyProfitByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = currentYear();
        return ResponseEntity.ok(profitReportService.getWeeklyProfitByProduct(productId, year));
    }

    @GetMapping("/products/{productId}/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Monthly profit for a specific product")
    public ResponseEntity<List<ProductProfitResponse>> monthlyProfitByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = currentYear();
        return ResponseEntity.ok(profitReportService.getMonthlyProfitByProduct(productId, year));
    }

    @GetMapping("/products/{productId}/yearly")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Yearly profit for a specific product")
    public ResponseEntity<List<ProductProfitResponse>> yearlyProfitByProduct(
            @PathVariable String productId) {
        return ResponseEntity.ok(profitReportService.getYearlyProfitByProduct(productId));
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Top products by profit",
            description = "Returns all products ranked by total profit descending. Pass month=0 for the full year."
    )
    public ResponseEntity<List<ProductProfitResponse>> topProducts(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        if (year == 0) year = currentYear();
        return ResponseEntity.ok(profitReportService.getTopProductsByProfit(year, month));
    }
}
