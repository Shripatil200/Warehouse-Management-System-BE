package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateBinRentalRequest {

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    @NotBlank(message = "Storage bin ID is required")
    private String storageBinId;

    @NotNull(message = "Rental rate per day is required")
    @DecimalMin(value = "0.01", message = "Rental rate must be greater than zero")
    private BigDecimal rentalRatePerDay;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private String notes;
}
