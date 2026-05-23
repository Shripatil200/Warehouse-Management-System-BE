package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.dto.v1.request.CreateConsignmentAgreementRequest;
import com.infotact.warehouse.dto.v1.request.TriggerSettlementRequest;
import com.infotact.warehouse.dto.v1.request.UpdateSettlementStatusRequest;
import com.infotact.warehouse.dto.v1.response.ConsignmentAgreementResponse;
import com.infotact.warehouse.dto.v1.response.ConsignmentSettlementResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.ConsignmentSettlementStatus;
import com.infotact.warehouse.entity.enums.ConsignmentStatus;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.ConsignmentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service handling all consignment business logic.
 *
 * Supplier System v4.0: Suppliers are now a dedicated {@link Supplier} entity
 * stored in the suppliers table, resolved via {@link SupplierRepository}.
 * {@link ConsignmentAgreement#getSupplier()} returns a {@link Supplier}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsignmentServiceImpl implements ConsignmentService {

    private final ConsignmentAgreementRepository agreementRepo;
    private final ConsignmentSaleRepository       saleRepo;
    private final ConsignmentSettlementRepository settlementRepo;
    private final SupplierRepository              supplierRepository;
    private final ProductRepository               productRepo;
    private final WarehouseRepository             warehouseRepo;

    @Transactional
    public ConsignmentAgreementResponse createAgreement(CreateConsignmentAgreementRequest req) {
        String warehouseId = TenantContext.get();

        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Supplier not found: " + req.getSupplierId()));

        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + warehouseId));

        ConsignmentAgreement agreement = new ConsignmentAgreement();
        agreement.setSupplier(supplier);
        agreement.setWarehouse(warehouse);
        agreement.setWarehouseCommissionPct(req.getWarehouseCommissionPct());
        agreement.setEffectiveFrom(req.getEffectiveFrom());
        agreement.setEffectiveTo(req.getEffectiveTo());
        agreement.setSettlementCycleDays(req.getSettlementCycleDays());
        agreement.setNotes(req.getNotes());
        agreement.setStatus(ConsignmentStatus.PENDING_APPROVAL);
        agreement.setAgreementCode(generateAgreementCode());

        List<ConsignmentProduct> cpList = new ArrayList<>();
        for (CreateConsignmentAgreementRequest.ConsignmentProductRequest pr : req.getProducts()) {
            Product product = productRepo.findByIdAndWarehouseId(pr.getProductId(), warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + pr.getProductId()));
            ConsignmentProduct cp = new ConsignmentProduct();
            cp.setAgreement(agreement);
            cp.setProduct(product);
            cp.setMrp(pr.getMrp());
            cp.setFloorPrice(pr.getFloorPrice());
            cp.setActive(true);
            cpList.add(cp);
        }
        agreement.setConsignmentProducts(cpList);

        ConsignmentAgreement saved = agreementRepo.save(agreement);
        log.info("Consignment agreement {} created for supplier {}", saved.getAgreementCode(), supplier.getCompanyName());
        return mapToAgreementResponse(saved);
    }

    @Transactional
    public ConsignmentAgreementResponse approveAgreement(String agreementId) {
        String warehouseId = TenantContext.get();
        ConsignmentAgreement agreement = findAgreement(agreementId, warehouseId);
        if (agreement.getStatus() != ConsignmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Agreement " + agreementId + " is not in PENDING_APPROVAL state");
        }
        agreement.setStatus(ConsignmentStatus.ACTIVE);
        for (ConsignmentProduct cp : agreement.getConsignmentProducts()) {
            Product p = cp.getProduct();
            p.setConsignment(true);
            p.setConsignmentAgreement(agreement);
            productRepo.save(p);
        }
        ConsignmentAgreement saved = agreementRepo.save(agreement);
        log.info("Consignment agreement {} approved and now ACTIVE", saved.getAgreementCode());
        return mapToAgreementResponse(saved);
    }

    @Transactional
    public ConsignmentAgreementResponse rejectAgreement(String agreementId) {
        String warehouseId = TenantContext.get();
        ConsignmentAgreement agreement = findAgreement(agreementId, warehouseId);
        if (agreement.getStatus() != ConsignmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only PENDING_APPROVAL agreements can be rejected");
        }
        agreement.setStatus(ConsignmentStatus.REJECTED);
        return mapToAgreementResponse(agreementRepo.save(agreement));
    }

    @Transactional
    public ConsignmentAgreementResponse terminateAgreement(String agreementId, String managerNotes) {
        String warehouseId = TenantContext.get();
        ConsignmentAgreement agreement = findAgreement(agreementId, warehouseId);
        if (agreement.getStatus() != ConsignmentStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE agreements can be terminated");
        }
        generateSettlementInternal(agreement, managerNotes);
        for (ConsignmentProduct cp : agreement.getConsignmentProducts()) {
            Product p = cp.getProduct();
            p.setConsignment(false);
            p.setConsignmentAgreement(null);
            productRepo.save(p);
        }
        agreement.setStatus(ConsignmentStatus.TERMINATED);
        return mapToAgreementResponse(agreementRepo.save(agreement));
    }

    @Transactional(readOnly = true)
    public List<ConsignmentAgreementResponse> listAgreements(ConsignmentStatus status) {
        String warehouseId = TenantContext.get();
        List<ConsignmentAgreement> agreements = status != null
                ? agreementRepo.findByStatusAndWarehouseId(status, warehouseId)
                : agreementRepo.findAllByWarehouseId(warehouseId);
        return agreements.stream().map(this::mapToAgreementResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsignmentAgreementResponse getAgreement(String agreementId) {
        String warehouseId = TenantContext.get();
        return mapToAgreementResponse(findAgreement(agreementId, warehouseId));
    }

    @Transactional
    public ConsignmentSale recordConsignmentSale(SellingOrderItem orderItem, Product product, LocalDateTime soldAt) {
        ConsignmentAgreement agreement = product.getConsignmentAgreement();
        if (agreement == null) {
            log.warn("Product {} flagged as consignment but has no agreement — skipping sale record", product.getId());
            return null;
        }
        BigDecimal unitPrice    = orderItem.getSellPriceAtTimeOfOrder();
        int        qty          = orderItem.getQuantity();
        BigDecimal grossRevenue = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal commPct      = agreement.getWarehouseCommissionPct();
        BigDecimal warehouseShare = grossRevenue.multiply(commPct)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal supplierShare = grossRevenue.subtract(warehouseShare);

        ConsignmentSale sale = new ConsignmentSale();
        sale.setAgreement(agreement);
        sale.setOrderItem(orderItem);
        sale.setProduct(product);
        sale.setQuantity(qty);
        sale.setUnitSellPrice(unitPrice);
        sale.setGrossRevenue(grossRevenue);
        sale.setWarehouseCommissionPctSnapshot(commPct);
        sale.setWarehouseShare(warehouseShare);
        sale.setSupplierShare(supplierShare);
        sale.setSoldAt(soldAt);
        sale.setSettled(false);

        ConsignmentSale saved = saleRepo.save(sale);
        log.debug("Consignment sale recorded: product={} qty={} supplierShare={}", product.getId(), qty, supplierShare);
        return saved;
    }

    @Transactional
    public ConsignmentSettlementResponse triggerSettlement(TriggerSettlementRequest req) {
        String warehouseId = TenantContext.get();
        ConsignmentAgreement agreement = findAgreement(req.getAgreementId(), warehouseId);
        if (agreement.getStatus() != ConsignmentStatus.ACTIVE) {
            throw new IllegalStateException("Settlement can only be triggered for ACTIVE agreements");
        }
        ConsignmentSettlement settlement = generateSettlementInternal(agreement, req.getManagerNotes());
        if (settlement == null) {
            throw new IllegalStateException("No unsettled sales found for this agreement in the current period");
        }
        return mapToSettlementResponse(settlement);
    }

    @Transactional
    public ConsignmentSettlement generateSettlementInternal(ConsignmentAgreement agreement, String notes) {
        LocalDate today       = LocalDate.now();
        LocalDate periodEnd   = today;
        Optional<LocalDate> lastEnd = settlementRepo.findLastSettledPeriodEnd(agreement.getId());
        LocalDate periodStart = lastEnd.map(d -> d.plusDays(1)).orElse(agreement.getEffectiveFrom());

        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime to   = periodEnd.atTime(23, 59, 59);

        List<ConsignmentSale> unsettledSales =
                saleRepo.findUnsettledSalesForPeriod(agreement.getId(), from, to);
        if (unsettledSales.isEmpty()) {
            log.info("No unsettled sales for agreement {} between {} and {}",
                    agreement.getAgreementCode(), from, to);
            return null;
        }

        BigDecimal totalGross = BigDecimal.ZERO, totalWarehouse = BigDecimal.ZERO, totalSupplier = BigDecimal.ZERO;
        int totalUnits = 0;
        for (ConsignmentSale s : unsettledSales) {
            totalGross     = totalGross.add(s.getGrossRevenue());
            totalWarehouse = totalWarehouse.add(s.getWarehouseShare());
            totalSupplier  = totalSupplier.add(s.getSupplierShare());
            totalUnits    += s.getQuantity();
        }

        ConsignmentSettlement settlement = new ConsignmentSettlement();
        settlement.setAgreement(agreement);
        settlement.setWarehouse(agreement.getWarehouse());
        settlement.setSettlementNumber(generateSettlementNumber());
        settlement.setPeriodFrom(periodStart);
        settlement.setPeriodTo(periodEnd);
        settlement.setTotalGrossRevenue(totalGross);
        settlement.setTotalWarehouseShare(totalWarehouse);
        settlement.setTotalSupplierPayout(totalSupplier);
        settlement.setTotalUnitsSold(totalUnits);
        settlement.setStatus(ConsignmentSettlementStatus.PENDING);
        settlement.setManagerNotes(notes);

        ConsignmentSettlement saved = settlementRepo.save(settlement);
        List<String> saleIds = unsettledSales.stream().map(ConsignmentSale::getId).collect(Collectors.toList());
        saleRepo.markAsSettled(saleIds, saved.getId());

        log.info("Settlement {} generated: supplierPayout={} for agreement {}",
                saved.getSettlementNumber(), totalSupplier, agreement.getAgreementCode());
        return saved;
    }

    @Transactional
    public ConsignmentSettlementResponse approveSettlement(String settlementId, UpdateSettlementStatusRequest req) {
        String warehouseId = TenantContext.get();
        ConsignmentSettlement settlement = findSettlement(settlementId, warehouseId);
        if (settlement.getStatus() != ConsignmentSettlementStatus.PENDING) {
            throw new IllegalStateException("Only PENDING settlements can be approved");
        }
        settlement.setStatus(ConsignmentSettlementStatus.APPROVED);
        if (req.getManagerNotes() != null) settlement.setManagerNotes(req.getManagerNotes());
        return mapToSettlementResponse(settlementRepo.save(settlement));
    }

    @Transactional
    public ConsignmentSettlementResponse markSettlementPaid(String settlementId, UpdateSettlementStatusRequest req) {
        String warehouseId = TenantContext.get();
        ConsignmentSettlement settlement = findSettlement(settlementId, warehouseId);
        if (settlement.getStatus() != ConsignmentSettlementStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED settlements can be marked as PAID");
        }
        settlement.setStatus(ConsignmentSettlementStatus.PAID);
        settlement.setPaidAt(LocalDateTime.now());
        if (req.getManagerNotes() != null) settlement.setManagerNotes(req.getManagerNotes());
        return mapToSettlementResponse(settlementRepo.save(settlement));
    }

    @Transactional(readOnly = true)
    public List<ConsignmentSettlementResponse> listSettlementsForAgreement(String agreementId) {
        return settlementRepo.findByAgreementIdOrderByPeriodFromDesc(agreementId)
                .stream().map(this::mapToSettlementResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConsignmentSettlementResponse> listSettlementsByStatus(ConsignmentSettlementStatus status) {
        String warehouseId = TenantContext.get();
        return settlementRepo.findByStatusAndWarehouseId(status, warehouseId)
                .stream().map(this::mapToSettlementResponse).collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ConsignmentAgreement findAgreement(String id, String warehouseId) {
        return agreementRepo.findById(id)
                .filter(a -> a.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new EntityNotFoundException("Consignment agreement not found: " + id));
    }

    private ConsignmentSettlement findSettlement(String id, String warehouseId) {
        return settlementRepo.findById(id)
                .filter(s -> s.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new EntityNotFoundException("Settlement not found: " + id));
    }

    private ConsignmentAgreementResponse mapToAgreementResponse(ConsignmentAgreement a) {
        List<ConsignmentAgreementResponse.ConsignmentProductResponse> products =
                a.getConsignmentProducts() == null ? List.of() :
                        a.getConsignmentProducts().stream().map(cp ->
                                ConsignmentAgreementResponse.ConsignmentProductResponse.builder()
                                        .id(cp.getId())
                                        .productId(cp.getProduct().getId())
                                        .productName(cp.getProduct().getName())
                                        .sku(cp.getProduct().getSku())
                                        .mrp(cp.getMrp())
                                        .floorPrice(cp.getFloorPrice())
                                        .active(cp.isActive())
                                        .build()
                        ).collect(Collectors.toList());

        return ConsignmentAgreementResponse.builder()
                .id(a.getId())
                .agreementCode(a.getAgreementCode())
                .supplierId(a.getSupplier().getId())
                .supplierName(a.getSupplier().getCompanyName())   // Supplier.getCompanyName()
                .warehouseCommissionPct(a.getWarehouseCommissionPct())
                .status(a.getStatus())
                .effectiveFrom(a.getEffectiveFrom())
                .effectiveTo(a.getEffectiveTo())
                .settlementCycleDays(a.getSettlementCycleDays())
                .notes(a.getNotes())
                .products(products)
                .build();
    }

    private ConsignmentSettlementResponse mapToSettlementResponse(ConsignmentSettlement s) {
        Map<String, ConsignmentSettlementResponse.ProductBreakdown> breakdownMap = new LinkedHashMap<>();
        if (s.getSales() != null) {
            for (ConsignmentSale sale : s.getSales()) {
                String pid = sale.getProduct().getId();
                breakdownMap.compute(pid, (k, existing) -> {
                    if (existing == null) {
                        return ConsignmentSettlementResponse.ProductBreakdown.builder()
                                .productId(pid)
                                .productName(sale.getProduct().getName())
                                .unitsSold(sale.getQuantity())
                                .grossRevenue(sale.getGrossRevenue())
                                .warehouseShare(sale.getWarehouseShare())
                                .supplierShare(sale.getSupplierShare())
                                .build();
                    }
                    existing.setUnitsSold(existing.getUnitsSold() + sale.getQuantity());
                    existing.setGrossRevenue(existing.getGrossRevenue().add(sale.getGrossRevenue()));
                    existing.setWarehouseShare(existing.getWarehouseShare().add(sale.getWarehouseShare()));
                    existing.setSupplierShare(existing.getSupplierShare().add(sale.getSupplierShare()));
                    return existing;
                });
            }
        }
        return ConsignmentSettlementResponse.builder()
                .id(s.getId())
                .settlementNumber(s.getSettlementNumber())
                .agreementId(s.getAgreement().getId())
                .agreementCode(s.getAgreement().getAgreementCode())
                .supplierName(s.getAgreement().getSupplier().getCompanyName())  // Supplier.getCompanyName()
                .periodFrom(s.getPeriodFrom())
                .periodTo(s.getPeriodTo())
                .totalGrossRevenue(s.getTotalGrossRevenue())
                .totalWarehouseShare(s.getTotalWarehouseShare())
                .totalSupplierPayout(s.getTotalSupplierPayout())
                .totalUnitsSold(s.getTotalUnitsSold())
                .status(s.getStatus())
                .managerNotes(s.getManagerNotes())
                .paidAt(s.getPaidAt())
                .productBreakdowns(new ArrayList<>(breakdownMap.values()))
                .build();
    }

    private String generateAgreementCode() {
        // UUID suffix guarantees uniqueness across restarts and cluster nodes
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("CONS-%d-%s", LocalDate.now().getYear(), suffix);
    }

    private String generateSettlementNumber() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("SETL-%d-%s", LocalDate.now().getYear(), suffix);
    }
}