package com.infotact.warehouse.service;


public interface BarcodeService {
    /**
     * Logic to provide a barcode for a product SKU.
     * In a production system, you might save this to a file server (S3).
     * For now, we generate it on-the-fly.
     */
    public byte[] getBarcodeForProduct(String sku);
}
