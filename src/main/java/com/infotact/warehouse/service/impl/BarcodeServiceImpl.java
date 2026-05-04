package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.service.BarcodeService;
import com.infotact.warehouse.util.BarcodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link BarcodeService} using the ZXing library.
 * <p>
 * Current logic generates images on-the-fly to minimize storage overhead.
 * For high-scale production, this could be extended to cache images in
 * a cloud storage provider (e.g., AWS S3).
 * </p>
 */
@Slf4j
@Service
public class BarcodeServiceImpl implements BarcodeService {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBarcodeForProduct(String sku) {
        try {
            log.debug("Generating Product barcode for SKU: {}", sku);
            return BarcodeUtils.generateBarcodeImage(sku);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to generate barcode for SKU {}. Reason: {}", sku, e.getMessage());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBarcodeForBin(String binCode) {
        try {
            log.debug("Generating Bin barcode for Location: {}", binCode);
            // Reuses the 1D Code 128 rendering logic for bin codes[cite: 2]
            return BarcodeUtils.generateBarcodeImage(binCode);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to generate barcode for Bin {}. Reason: {}", binCode, e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] getQRCode(String data, int width, int height) {
        return new byte[0];
    }
}