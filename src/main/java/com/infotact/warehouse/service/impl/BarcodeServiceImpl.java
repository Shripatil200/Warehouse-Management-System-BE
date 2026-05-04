package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.util.BarcodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BarcodeServiceImpl {
    /**
     * Logic to provide a barcode for a product SKU.
     * In a production system, you might save this to a file server (S3).
     * For now, we generate it on-the-fly.
     */
    public byte[] getBarcodeForProduct(String sku) {
        try {
            return BarcodeUtils.generateBarcodeImage(sku);
        } catch (Exception e) {
            log.error("Failed to generate barcode for SKU: {}", sku);
            return null;
        }
    }
}
