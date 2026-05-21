package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.CreateConsignmentAgreementRequest;
import com.infotact.warehouse.dto.v1.request.TriggerSettlementRequest;
import com.infotact.warehouse.dto.v1.request.UpdateSettlementStatusRequest;
import com.infotact.warehouse.dto.v1.response.ConsignmentAgreementResponse;
import com.infotact.warehouse.dto.v1.response.ConsignmentSettlementResponse;
import com.infotact.warehouse.entity.ConsignmentAgreement;
import com.infotact.warehouse.entity.ConsignmentSale;
import com.infotact.warehouse.entity.ConsignmentSettlement;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.SellingOrderItem;
import com.infotact.warehouse.entity.enums.ConsignmentSettlementStatus;
import com.infotact.warehouse.entity.enums.ConsignmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsignmentService {

    // ── Agreement Management ──────────────────────────────────────────────────

    ConsignmentAgreementResponse createAgreement(CreateConsignmentAgreementRequest request);

    ConsignmentAgreementResponse approveAgreement(String agreementId);

    ConsignmentAgreementResponse rejectAgreement(String agreementId);

    ConsignmentAgreementResponse terminateAgreement(String agreementId, String managerNotes);

    ConsignmentAgreementResponse getAgreement(String agreementId);

    List<ConsignmentAgreementResponse> listAgreements(ConsignmentStatus status);

    // ── Sale Recording (called from OrderService) ─────────────────────────────

    /**
     * Records a consignment sale for a packed order item.
     * Returns the persisted {@link ConsignmentSale} so that the caller
     * (OrderServiceImpl.verifyAndPack) can update the order item's profit field
     * with the computed warehouseShare.
     */
    ConsignmentSale recordConsignmentSale(SellingOrderItem orderItem, Product product, LocalDateTime soldAt);

    // ── Settlement ────────────────────────────────────────────────────────────

    ConsignmentSettlementResponse triggerSettlement(TriggerSettlementRequest request);

    /**
     * Internal method used by both the manual trigger and the scheduler.
     * Returns null if there are no unsettled sales in the current period.
     */
    ConsignmentSettlement generateSettlementInternal(ConsignmentAgreement agreement, String notes);

    ConsignmentSettlementResponse approveSettlement(String settlementId, UpdateSettlementStatusRequest request);

    ConsignmentSettlementResponse markSettlementPaid(String settlementId, UpdateSettlementStatusRequest request);

    List<ConsignmentSettlementResponse> listSettlementsForAgreement(String agreementId);

    List<ConsignmentSettlementResponse> listSettlementsByStatus(ConsignmentSettlementStatus status);
}
