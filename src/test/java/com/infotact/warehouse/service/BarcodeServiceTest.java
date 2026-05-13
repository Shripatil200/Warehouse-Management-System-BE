package com.infotact.warehouse.service;

import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.BarcodeAuditRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.service.impl.BarcodeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BarcodeServiceImpl}.
 *
 * Verifies that the ZXing-based barcode generation works correctly
 * for known products and fails gracefully for unknown SKUs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BarcodeService Unit Tests")
class BarcodeServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private BarcodeAuditRepository auditRepository;

    @InjectMocks
    private BarcodeServiceImpl barcodeService;

    private Product buildProduct(String sku) {
        Warehouse wh = new Warehouse();
        wh.setId("wh-001");

        Product p = new Product();
        p.setId("prod-001");
        p.setSku(sku);
        p.setName("Test Product");
        p.setWarehouse(wh);
        return p;
    }

    @Test
    @DisplayName("generateBarcode - should return non-empty byte array for valid product SKU")
    void generateBarcode_ShouldReturnBytes_ForValidSku() throws Exception {
        // Arrange
        String sku = "SKU-TEST-001";
        when(productRepository.findBySku(sku)).thenReturn(Optional.of(buildProduct(sku)));

        // Act
        byte[] result = barcodeService.generateBarcodeForSku(sku);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generateBarcode - should throw ResourceNotFoundException for unknown SKU")
    void generateBarcode_ShouldThrow_WhenSkuNotFound() {
        // Arrange
        when(productRepository.findBySku("UNKNOWN")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> barcodeService.generateBarcodeForSku("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
