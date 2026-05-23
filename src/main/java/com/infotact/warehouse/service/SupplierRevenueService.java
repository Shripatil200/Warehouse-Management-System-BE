package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.response.SupplierRevenueResponse;

import java.time.LocalDate;
import java.util.List;

public interface SupplierRevenueService {

    /**
     * Returns revenue and spend summary for every supplier that had activity
     * within the given date range, scoped to the authenticated user's warehouse.
     *
     * @param from start of the reporting period (inclusive)
     * @param to   end of the reporting period (inclusive)
     */
    List<SupplierRevenueResponse> getSupplierRevenue(LocalDate from, LocalDate to);

    /**
     * Returns revenue and spend summary for a single supplier.
     *
     * @param supplierId the supplier's UUID
     * @param from       start of the reporting period (inclusive)
     * @param to         end of the reporting period (inclusive)
     */
    SupplierRevenueResponse getSupplierRevenueById(String supplierId, LocalDate from, LocalDate to);
}
