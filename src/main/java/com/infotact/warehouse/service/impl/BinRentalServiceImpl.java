package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.dto.v1.request.CreateBinRentalRequest;
import com.infotact.warehouse.dto.v1.response.BinRentalResponse;
import com.infotact.warehouse.entity.BinRental;
import com.infotact.warehouse.entity.BinRentalPayment;
import com.infotact.warehouse.entity.StorageBin;
import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.BinRentalPaymentStatus;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.EntityNotFoundException;
import com.infotact.warehouse.repository.BinRentalPaymentRepository;
import com.infotact.warehouse.repository.BinRentalRepository;
import com.infotact.warehouse.repository.BinRepository;
import com.infotact.warehouse.repository.SupplierRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.BinRentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinRentalServiceImpl implements BinRentalService {

    private final BinRentalRepository binRentalRepository;
    private final BinRentalPaymentRepository binRentalPaymentRepository;
    private final BinRepository binRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public BinRentalResponse createRental(CreateBinRentalRequest request) {
        String warehouseId = TenantContext.get();

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + request.getSupplierId()));

        StorageBin bin = binRepository.findByIdAndWarehouseId(request.getStorageBinId(), warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Storage bin not found in this warehouse: " + request.getStorageBinId()));

        binRentalRepository.findActiveRentalForBin(warehouseId, request.getStorageBinId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Bin " + bin.getBinCode() + " already has an active rental.");
                });

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + warehouseId));

        BinRental rental = new BinRental();
        rental.setSupplier(supplier);
        rental.setStorageBin(bin);
        rental.setWarehouse(warehouse);
        rental.setRentalRatePerDay(request.getRentalRatePerDay());
        rental.setStartDate(request.getStartDate());
        rental.setEndDate(request.getEndDate());
        rental.setActive(true);
        rental.setNotes(request.getNotes());

        BinRental saved = binRentalRepository.save(rental);
        log.info("Created bin rental {} for supplier {} on bin {}", saved.getId(), supplier.getName(), bin.getBinCode());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BinRentalResponse> getAllRentals() {
        String warehouseId = TenantContext.get();
        return binRentalRepository.findAllByWarehouseIdAndActiveTrue(warehouseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BinRentalResponse> getRentalsBySupplier(String supplierId) {
        String warehouseId = TenantContext.get();
        return binRentalRepository.findAllBySupplierIdAndWarehouseId(supplierId, warehouseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BinRentalResponse closeRental(String rentalId) {
        String warehouseId = TenantContext.get();
        BinRental rental = binRentalRepository.findByIdAndWarehouseId(rentalId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Bin rental not found: " + rentalId));

        rental.setActive(false);
        rental.setEndDate(LocalDate.now());
        BinRental saved = binRentalRepository.save(rental);
        log.info("Closed bin rental {} for supplier {}", rentalId, rental.getSupplier().getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BinRentalResponse.BinRentalPaymentResponse generatePayment(String rentalId, LocalDate from, LocalDate to) {
        String warehouseId = TenantContext.get();
        BinRental rental = binRentalRepository.findByIdAndWarehouseId(rentalId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Bin rental not found: " + rentalId));

        if (binRentalPaymentRepository.existsByBinRentalIdAndPeriodFromAndPeriodTo(rentalId, from, to)) {
            throw new BadRequestException("A payment record already exists for this rental and period.");
        }

        long totalDays = ChronoUnit.DAYS.between(from, to) + 1;
        BigDecimal totalAmount = rental.getRentalRatePerDay().multiply(BigDecimal.valueOf(totalDays));

        BinRentalPayment payment = new BinRentalPayment();
        payment.setBinRental(rental);
        payment.setPeriodFrom(from);
        payment.setPeriodTo(to);
        payment.setTotalDays((int) totalDays);
        payment.setTotalAmount(totalAmount);
        payment.setStatus(BinRentalPaymentStatus.PENDING);

        BinRentalPayment saved = binRentalPaymentRepository.save(payment);
        log.info("Generated payment {} for rental {} amount {}", saved.getId(), rentalId, totalAmount);
        return toPaymentResponse(saved);
    }

    @Override
    @Transactional
    public BinRentalResponse.BinRentalPaymentResponse markPaymentPaid(String paymentId) {
        BinRentalPayment payment = binRentalPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() == BinRentalPaymentStatus.PAID) {
            throw new BadRequestException("Payment is already marked as PAID.");
        }

        payment.setStatus(BinRentalPaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        BinRentalPayment saved = binRentalPaymentRepository.save(payment);
        log.info("Marked payment {} as PAID", paymentId);
        return toPaymentResponse(saved);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private BinRentalResponse toResponse(BinRental rental) {
        List<BinRentalResponse.BinRentalPaymentResponse> payments = List.of();
        if (rental.getPayments() != null) {
            payments = rental.getPayments().stream()
                    .map(this::toPaymentResponse)
                    .collect(Collectors.toList());
        }

        return BinRentalResponse.builder()
                .id(rental.getId())
                .supplierId(rental.getSupplier().getId())
                .supplierName(rental.getSupplier().getName())
                .storageBinId(rental.getStorageBin().getId())
                .binCode(rental.getStorageBin().getBinCode())
                .rentalRatePerDay(rental.getRentalRatePerDay())
                .startDate(rental.getStartDate())
                .endDate(rental.getEndDate())
                .active(rental.isActive())
                .notes(rental.getNotes())
                .payments(payments)
                .build();
    }

    private BinRentalResponse.BinRentalPaymentResponse toPaymentResponse(BinRentalPayment p) {
        return BinRentalResponse.BinRentalPaymentResponse.builder()
                .id(p.getId())
                .periodFrom(p.getPeriodFrom())
                .periodTo(p.getPeriodTo())
                .totalDays(p.getTotalDays())
                .totalAmount(p.getTotalAmount())
                .status(p.getStatus().name())
                .paidAt(p.getPaidAt() != null ? p.getPaidAt().toString() : null)
                .build();
    }
}
