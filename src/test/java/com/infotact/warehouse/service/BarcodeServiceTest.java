package com.infotact.warehouse.service;

import com.infotact.warehouse.service.impl.BarcodeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link BarcodeServiceImpl}.
 *
 * BarcodeServiceImpl delegates directly to BarcodeUtils (ZXing).
 * No repository calls — tests verify the output byte array is valid.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BarcodeService Unit Tests")
class BarcodeServiceTest {

    @InjectMocks
    private BarcodeServiceImpl barcodeService;

    @Test
    @DisplayName("getBarcodeForProduct - should return non-empty byte array for a valid SKU string")
    void getBarcodeForProduct_ShouldReturnBytes_ForValidSku() {
        byte[] result = barcodeService.getBarcodeForProduct("SKU-TEST-001");

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("getBarcodeForBin - should return non-empty byte array for a valid bin code")
    void getBarcodeForBin_ShouldReturnBytes_ForValidBinCode() {
        byte[] result = barcodeService.getBarcodeForBin("A-01-BIN-001");

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("getBarcodeForProduct - should return null for blank input (error handled internally)")
    void getBarcodeForProduct_ShouldHandleBlankInput_Gracefully() {
        // BarcodeServiceImpl catches exceptions internally and returns null
        byte[] result = barcodeService.getBarcodeForProduct("");

        // The service logs the error and returns null — no exception thrown
        assertThat(result).isNull();
    }
}