package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.response.ProductProfitResponse;
import com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse;

import java.util.List;

public interface ProfitReportService {

    List<ProfitPeriodResponse> getWeeklyProfit(int year);

    List<ProfitPeriodResponse> getMonthlyProfit(int year);

    List<ProfitPeriodResponse> getYearlyProfit();

    List<ProductProfitResponse> getWeeklyProfitByProduct(String productId, int year);

    List<ProductProfitResponse> getMonthlyProfitByProduct(String productId, int year);

    List<ProductProfitResponse> getYearlyProfitByProduct(String productId);

    /**
     * Returns all products ranked by total profit for the given year.
     * Pass {@code month = 0} for the full-year summary; pass 1–12 to scope to a month.
     */
    List<ProductProfitResponse> getTopProductsByProfit(int year, int month);
}
