package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.dto.v1.response.SupplierRevenueResponse;
import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.exception.EntityNotFoundException;
import com.infotact.warehouse.repository.BinRentalPaymentRepository;
import com.infotact.warehouse.repository.SupplierRepository;
import com.infotact.warehouse.repository.SupplierRevenueRepository;
import com.infotact.warehouse.service.SupplierRevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupplierRevenueServiceImpl implements SupplierRevenueService {

    private final SupplierRevenueRepository supplierRevenueRepository;
    private final BinRentalPaymentRepository binRentalPaymentRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseContext warehouseContext;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierRevenueResponse> getSupplierRevenue(LocalDate from, LocalDate to) {
        String warehouseId = warehouseContext.getWarehouseId();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        Map<String, BigDecimal> commissionMap = new HashMap<>();
        for (Object[] row : supplierRevenueRepository.sumCommissionPerSupplier(warehouseId, fromDt, toDt)) {
            commissionMap.put((String) row[0], (BigDecimal) row[1]);
        }

        Map<String, BigDecimal> rentalMap = new HashMap<>();
        for (Object[] row : binRentalPaymentRepository.sumRentalRevenuePerSupplier(warehouseId, from, to)) {
            rentalMap.put((String) row[0], (BigDecimal) row[1]);
        }

        Map<String, BigDecimal> spendMap = new HashMap<>();
        for (Object[] row : supplierRevenueRepository.sumSpendPerSupplier(warehouseId, fromDt, toDt)) {
            spendMap.put((String) row[0], (BigDecimal) row[1]);
        }

        java.util.Set<String> allSupplierIds = new java.util.HashSet<>();
        allSupplierIds.addAll(commissionMap.keySet());
        allSupplierIds.addAll(rentalMap.keySet());
        allSupplierIds.addAll(spendMap.keySet());

        List<SupplierRevenueResponse> results = new ArrayList<>();
        for (String supplierId : allSupplierIds) {
            Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
            if (supplier == null) continue;

            BigDecimal commission = commissionMap.getOrDefault(supplierId, BigDecimal.ZERO);
            BigDecimal rental = rentalMap.getOrDefault(supplierId, BigDecimal.ZERO);
            BigDecimal spend = spendMap.getOrDefault(supplierId, BigDecimal.ZERO);
            BigDecimal totalRevenue = commission.add(rental);
            BigDecimal netPosition = totalRevenue.subtract(spend);

            results.add(SupplierRevenueResponse.builder()
                    .supplierId(supplierId)
                    .supplierName(supplier.getName())
                    .commissionEarned(commission)
                    .binRentalEarned(rental)
                    .totalRevenueFromSupplier(totalRevenue)
                    .totalSpentOnSupplier(spend)
                    .netPosition(netPosition)
                    .build());
        }

        results.sort(java.util.Comparator.comparing(SupplierRevenueResponse::getTotalRevenueFromSupplier).reversed());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierRevenueResponse getSupplierRevenueById(String supplierId, LocalDate from, LocalDate to) {
        String warehouseId = warehouseContext.getWarehouseId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + supplierId));

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        BigDecimal commission = supplierRevenueRepository.sumCommissionForSupplier(warehouseId, supplierId, fromDt, toDt);
        BigDecimal rental = binRentalPaymentRepository.sumRentalRevenueForSupplier(warehouseId, supplierId, from, to);
        BigDecimal spend = supplierRevenueRepository.sumSpendForSupplier(warehouseId, supplierId, fromDt, toDt);

        commission = commission != null ? commission : BigDecimal.ZERO;
        rental = rental != null ? rental : BigDecimal.ZERO;
        spend = spend != null ? spend : BigDecimal.ZERO;

        BigDecimal totalRevenue = commission.add(rental);
        BigDecimal netPosition = totalRevenue.subtract(spend);

        return SupplierRevenueResponse.builder()
                .supplierId(supplierId)
                .supplierName(supplier.getName())
                .commissionEarned(commission)
                .binRentalEarned(rental)
                .totalRevenueFromSupplier(totalRevenue)
                .totalSpentOnSupplier(spend)
                .netPosition(netPosition)
                .build();
    }
}
