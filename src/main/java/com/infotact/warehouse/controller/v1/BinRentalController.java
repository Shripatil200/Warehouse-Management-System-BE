package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.CreateBinRentalRequest;
import com.infotact.warehouse.dto.v1.response.BinRentalResponse;
import com.infotact.warehouse.service.BinRentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Bin Rental management.
 *
 * <pre>
 * Base path: /api/v1/bin-rentals
 *
 * POST   /                                          — create a new bin rental
 * GET    /                                          — list all active rentals in this warehouse
 * GET    /supplier/{supplierId}                     — list rentals for a supplier
 * PUT    /{rentalId}/close                          — close/deactivate a rental
 * POST   /{rentalId}/payments/generate?from=&to=   — generate a billing payment record
 * PUT    /payments/{paymentId}/mark-paid            — mark a payment as PAID
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/bin-rentals")
@RequiredArgsConstructor
@Tag(name = "Bin Rentals", description = "Storage bin rental management for consignment suppliers")
public class BinRentalController {

    private final BinRentalService binRentalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create a new bin rental", description = "Registers a new bin rental agreement between a consignment supplier and the warehouse.")
    public ResponseEntity<BinRentalResponse> createRental(@Valid @RequestBody CreateBinRentalRequest request) {
        return ResponseEntity.ok(binRentalService.createRental(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List all active bin rentals in the current warehouse")
    public ResponseEntity<List<BinRentalResponse>> getAllRentals() {
        return ResponseEntity.ok(binRentalService.getAllRentals());
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List all bin rentals for a specific supplier")
    public ResponseEntity<List<BinRentalResponse>> getRentalsBySupplier(@PathVariable String supplierId) {
        return ResponseEntity.ok(binRentalService.getRentalsBySupplier(supplierId));
    }

    @PutMapping("/{rentalId}/close")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Close a bin rental", description = "Marks the rental inactive and sets the end date to today.")
    public ResponseEntity<BinRentalResponse> closeRental(@PathVariable String rentalId) {
        return ResponseEntity.ok(binRentalService.closeRental(rentalId));
    }

    @PostMapping("/{rentalId}/payments/generate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Generate a billing payment for a rental period")
    public ResponseEntity<BinRentalResponse.BinRentalPaymentResponse> generatePayment(
            @PathVariable String rentalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(binRentalService.generatePayment(rentalId, from, to));
    }

    @PutMapping("/payments/{paymentId}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Mark a bin rental payment as PAID")
    public ResponseEntity<BinRentalResponse.BinRentalPaymentResponse> markPaymentPaid(@PathVariable String paymentId) {
        return ResponseEntity.ok(binRentalService.markPaymentPaid(paymentId));
    }
}
