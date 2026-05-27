package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.dto.v1.response.ProductProfitResponse;
import com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse;
import com.infotact.warehouse.repository.ProfitReportRepository;
import com.infotact.warehouse.service.ProfitReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfitReportServiceImpl implements ProfitReportService {

    private final ProfitReportRepository profitReportRepository;
    private final WarehouseContext warehouseContext;

    @Override
    @Transactional(readOnly = true)
    public List<ProfitPeriodResponse> getWeeklyProfit(int year) {
        return profitReportRepository.findWeeklyProfit(warehouseId(), year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfitPeriodResponse> getMonthlyProfit(int year) {
        return profitReportRepository.findMonthlyProfit(warehouseId(), year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfitPeriodResponse> getYearlyProfit() {
        return profitReportRepository.findYearlyProfit(warehouseId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductProfitResponse> getWeeklyProfitByProduct(String productId, int year) {
        return profitReportRepository.findWeeklyProfitByProduct(productId, warehouseId(), year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductProfitResponse> getMonthlyProfitByProduct(String productId, int year) {
        return profitReportRepository.findMonthlyProfitByProduct(productId, warehouseId(), year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductProfitResponse> getYearlyProfitByProduct(String productId) {
        return profitReportRepository.findYearlyProfitByProduct(productId, warehouseId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductProfitResponse> getTopProductsByProfit(int year, int month) {
        return profitReportRepository.findTopProductsByProfit(warehouseId(), year, month);
    }

    private String warehouseId() {
        String id = warehouseContext.getWarehouseId();
        if (id == null) {
            throw new IllegalStateException("Warehouse context missing — cannot resolve warehouse.");
        }
        return id;
    }
}
