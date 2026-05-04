package com.infotact.warehouse.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class BarcodeUtils {

    /**
     * Generates a 1D Barcode (Code 128) for a given SKU.
     */
    public static byte[] generateBarcodeImage(String text) throws Exception {
        Code128Writer barcodeWriter = new Code128Writer();
        // Standard warehouse labels are usually 300x150 pixels
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, 300, 150);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}