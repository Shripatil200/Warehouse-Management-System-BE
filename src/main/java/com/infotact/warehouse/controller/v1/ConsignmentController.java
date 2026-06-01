package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.CreateConsignmentAgreementRequest;
import com.infotact.warehouse.dto.v1.request.TriggerSettlementRequest;
import com.infotact.warehouse.dto.v1.request.UpdateSettlementStatusRequest;
import com.infotact.warehouse.dto.v1.response.ConsignmentAgreementResponse;
import com.infotact.warehouse.dto.v1.response.ConsignmentSettlementResponse;
import com.infotact.warehouse.entity.enums.ConsignmentSettlementStatus;
import com.infotact.warehouse.entity.enums.ConsignmentStatus;
import com.infotact.warehouse.service.ConsignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * REST controller for the Consignment feature.
 *
 * <p>Base path: {@code /api/v1/consignments}
 *
 * <p><b>Role access summary:</b>
 * <ul>
 *   <li>MANAGER — full access (create, approve, reject, settle, mark-paid)</li>
 *   <li>OPERATOR — read-only (list and get)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/consignments")
@RequiredArgsConstructor
@Tag(name = "Consignment", description = "Manage supplier consignment agreements and settlements")
public class ConsignmentController {

    private final ConsignmentService consignmentService;

    // ─── AGREEMENTS ───────────────────────────────────────────────────────────

    /**
     * Create a new consignment agreement (starts in PENDING_APPROVAL).
     * MANAGER only.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create a consignment agreement",
            description = "Creates a new supplier consignment agreement in PENDING_APPROVAL state.")
    public ResponseEntity<ConsignmentAgreementResponse> createAgreement(
            @Valid @RequestBody CreateConsignmentAgreementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consignmentService.createAgreement(request));
    }

    /**
     * List all consignment agreements, optionally filtered by status.
     * Paginated: use ?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    @Operation(summary = "List consignment agreements (paginated)")
    public ResponseEntity<Page<ConsignmentAgreementResponse>> listAgreements(
            @RequestParam(required = false) ConsignmentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(consignmentService.listAgreements(status, pageable));
    }

    /**
     * Get a single consignment agreement by ID.
     */
    @GetMapping("/{agreementId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    @Operation(summary = "Get a consignment agreement by ID")
    public ResponseEntity<ConsignmentAgreementResponse> getAgreement(
            @PathVariable String agreementId) {
        return ResponseEntity.ok(consignmentService.getAgreement(agreementId));
    }

    /**
     * Approve a PENDING_APPROVAL agreement → ACTIVE.
     * MANAGER only.
     */
    @PatchMapping("/{agreementId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Approve a pending consignment agreement")
    public ResponseEntity<ConsignmentAgreementResponse> approveAgreement(
            @PathVariable String agreementId) {
        return ResponseEntity.ok(consignmentService.approveAgreement(agreementId));
    }

    /**
     * Reject a PENDING_APPROVAL agreement → REJECTED.
     * MANAGER only.
     */
    @PatchMapping("/{agreementId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Reject a pending consignment agreement")
    public ResponseEntity<ConsignmentAgreementResponse> rejectAgreement(
            @PathVariable String agreementId) {
        return ResponseEntity.ok(consignmentService.rejectAgreement(agreementId));
    }

    /**
     * Terminate an ACTIVE agreement → TERMINATED.
     * Triggers a final settlement before termination.
     * MANAGER only.
     */
    @PatchMapping("/{agreementId}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Terminate an active consignment agreement",
            description = "Triggers a final settlement for any unsettled sales before terminating.")
    public ResponseEntity<ConsignmentAgreementResponse> terminateAgreement(
            @PathVariable String agreementId,
            @RequestParam(required = false, defaultValue = "") String notes) {
        return ResponseEntity.ok(consignmentService.terminateAgreement(agreementId, notes));
    }

    // ─── SETTLEMENTS ──────────────────────────────────────────────────────────

    /**
     * Manually trigger a settlement for a specific agreement.
     * MANAGER only.
     */
    @PostMapping("/settlements/trigger")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Manually trigger a settlement",
            description = "Generates a settlement for all unsettled sales in the current cycle period.")
    public ResponseEntity<ConsignmentSettlementResponse> triggerSettlement(
            @Valid @RequestBody TriggerSettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consignmentService.triggerSettlement(request));
    }

    /**
     * List all settlements for a specific agreement (newest first).
     */
    @GetMapping("/{agreementId}/settlements")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    @Operation(summary = "List settlements for an agreement")
    public ResponseEntity<List<ConsignmentSettlementResponse>> listSettlementsForAgreement(
            @PathVariable String agreementId) {
        return ResponseEntity.ok(consignmentService.listSettlementsForAgreement(agreementId));
    }

    /**
     * List settlements across all agreements by status.
     * Useful for the manager's pending-approval dashboard.
     * Paginated: use ?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping("/settlements")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List settlements by status across all agreements (paginated)")
    public ResponseEntity<Page<ConsignmentSettlementResponse>> listSettlementsByStatus(
            @RequestParam(required = false) ConsignmentSettlementStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(consignmentService.listSettlementsByStatus(status, pageable));
    }

    /**
     * Approve a PENDING settlement → APPROVED.
     * MANAGER only.
     */
    @PatchMapping("/settlements/{settlementId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Approve a pending settlement")
    public ResponseEntity<ConsignmentSettlementResponse> approveSettlement(
            @PathVariable String settlementId,
            @RequestBody(required = false) UpdateSettlementStatusRequest request) {
        if (request == null) request = new UpdateSettlementStatusRequest();
        return ResponseEntity.ok(consignmentService.approveSettlement(settlementId, request));
    }

    /**
     * Mark an APPROVED settlement as PAID → payment confirmed.
     * MANAGER only.
     */
    @PatchMapping("/settlements/{settlementId}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Mark an approved settlement as paid")
    public ResponseEntity<ConsignmentSettlementResponse> markSettlementPaid(
            @PathVariable String settlementId,
            @RequestBody(required = false) UpdateSettlementStatusRequest request) {
        if (request == null) request = new UpdateSettlementStatusRequest();
        return ResponseEntity.ok(consignmentService.markSettlementPaid(settlementId, request));
    }
}
