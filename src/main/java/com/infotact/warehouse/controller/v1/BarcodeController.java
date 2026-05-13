package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.service.BarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Enhanced Barcode Controller for Industry-Standard Operations.
 * <p>
 * Features:
 * <ul>
 *     <li><b>HTTP Caching:</b> Offloads CPU load for static bin/SKU labels.</li>
 *     <li><b>Role-Based Access:</b> Restricts label generation to authorized personnel.</li>
 *     <li><b>QR Support:</b> Dynamic endpoint for complex data payloads.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/barcode")
@RequiredArgsConstructor
@Tag(name = "Hardware Integration", description = "Endpoints for thermal label printers and mobile scanners")
public class BarcodeController {

    private final BarcodeService barcodeService;

    @Operation(summary = "Get Product Label (1D)", description = "Generates a 1D barcode optimized for SKU identification.")
    @GetMapping(value = "/product/{sku}", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<byte[]> getProductBarcode(@PathVariable String sku) {
        byte[] image = barcodeService.getBarcodeForProduct(sku);
        if (image == null) return ResponseEntity.notFound().build();

        // Industry standard: Cache SKU labels for 24 hours to save server resources
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                .body(image);
    }

    @Operation(summary = "Get Bin Label (1D)", description = "Generates a location barcode for physical rack labeling.")
    @GetMapping(value = "/bin/{binCode}", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> getBinBarcode(@PathVariable String binCode) {
        byte[] image = barcodeService.getBarcodeForBin(binCode);
        if (image == null) return ResponseEntity.notFound().build();

        // Professional practice: Bin labels are permanent; cache for 1 year
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .body(image);
    }

    @Operation(summary = "Generate 2D QR Code", description = "Generates a high-density QR code for batch or expiry data.")
    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> getQRCode(
            @RequestParam String data,
            @RequestParam(defaultValue = "250") int width,
            @RequestParam(defaultValue = "250") int height) {

        byte[] image = barcodeService.getQRCode(data, width, height);
        return image != null ? ResponseEntity.ok().body(image) : ResponseEntity.badRequest().build();
    }
}