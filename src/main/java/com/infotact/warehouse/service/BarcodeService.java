package com.infotact.warehouse.service;

/**
 * Service interface for generating and managing scannable identifiers.
 * <p>
 * This service facilitates the creation of 1D/2D barcodes required for
 * physical warehouse operations, ensuring that every digital record (Product/Bin)
 * has a physical counterpart for mobile scanning.
 * </p>
 */
public interface BarcodeService {

    /**
     * Generates a printable barcode image for a specific Product SKU.
     * <p>
     * Used during the 'receiveShipment' phase to label incoming stock.
     * </p>
     * @param sku The unique Stock Keeping Unit of the product.
     * @return A byte array containing the PNG image data.
     */
    byte[] getBarcodeForProduct(String sku);

    /**
     * Generates a printable barcode image for a specific Storage Bin.
     * <p>
     * Used during warehouse setup or layout modification to identify
     * physical rack locations.
     * </p>
     * @param binCode The unique coordinate code of the bin (e.g., ZONE-A-01-05).
     * @return A byte array containing the PNG image data.
     */
    byte[] getBarcodeForBin(String binCode);

    /**
     * Advanced: Generates a QR code for mobile app deep-linking or batch data.
     */
    byte[] getQRCode(String data, int width, int height);
}