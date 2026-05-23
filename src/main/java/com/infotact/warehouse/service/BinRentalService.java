package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.CreateBinRentalRequest;
import com.infotact.warehouse.dto.v1.response.BinRentalResponse;

import java.util.List;

public interface BinRentalService {

    /** Create a new bin rental agreement for a supplier. */
    BinRentalResponse createRental(CreateBinRentalRequest request);

    /** List all active bin rentals in the current warehouse. */
    List<BinRentalResponse> getAllRentals();

    /** List all rentals for a specific supplier in the current warehouse. */
    List<BinRentalResponse> getRentalsBySupplier(String supplierId);

    /** Deactivate a rental (supplier vacated the bin). */
    BinRentalResponse closeRental(String rentalId);

    /** Manually generate a billing payment for a rental covering a date range. */
    BinRentalResponse.BinRentalPaymentResponse generatePayment(String rentalId, java.time.LocalDate from, java.time.LocalDate to);

    /** Mark a specific payment as PAID. */
    BinRentalResponse.BinRentalPaymentResponse markPaymentPaid(String paymentId);
}
