package com.infotact.warehouse.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Barcode Utility for Industry-Standard Symbologies.
 */
public class BarcodeUtils {

    /**
     * Generates a 1D Barcode (Code 128) - Best for shelf/bin identifiers.
     */
    public static byte[] generate1DBarcode(String text, int width, int height) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", bos);
            return bos.toByteArray();
        }
    }

    /**
     * Generates a 2D QR Code - Best for products with Batch/Expiry metadata.
     */
    public static byte[] generateQRCode(String text, int width, int height) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();

        // Industry Best Practice: High error correction for rugged warehouse environments
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", bos);
            return bos.toByteArray();
        }
    }
}