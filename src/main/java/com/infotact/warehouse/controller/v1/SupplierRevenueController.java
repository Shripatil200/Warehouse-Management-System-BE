package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.response.SupplierRevenueResponse;
import com.infotact.warehouse.service.SupplierRevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Supplier Revenue reporting.
 *
 * <pre>
 * Base path: /api/v1/reports/supplier-revenue
 *
 * GET /          ?from=&to=          — all suppliers in the warehouse for the period
 * GET /{supplierId}?from=&to=        — single supplier detail for the period
 * </pre>
 *
 * All endpoints restricted to ADMIN and MANAGER roles.
 */
@RestController
@RequestMapping("/api/v1/reports/supplier-revenue")
@RequiredArgsConstructor
@Tag(name = "Supplier Revenue Reports", description = "Aggregated revenue and spend metrics per supplier")
public class SupplierRevenueController {

    private final SupplierRevenueService supplierRevenueService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "All-supplier revenue report",
            description = "Returns commission earned, bin rental revenue, total spend, and net position for every supplier active within the given date range."
    )
    public ResponseEntity<List<SupplierRevenueResponse>> getAllSupplierRevenue(
            @RequestParam(defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null)   to   = LocalDate.now();

        return ResponseEntity.ok(supplierRevenueService.getSupplierRevenue(from, to));
    }

    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Single-supplier revenue report",
            description = "Returns commission, bin rental revenue, spend, and net position for a specific supplier."
    )
    public ResponseEntity<SupplierRevenueResponse> getSupplierRevenueById(
            @PathVariable String supplierId,
            @RequestParam(defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null)   to   = LocalDate.now();

        return ResponseEntity.ok(supplierRevenueService.getSupplierRevenueById(supplierId, from, to));
    }
}
