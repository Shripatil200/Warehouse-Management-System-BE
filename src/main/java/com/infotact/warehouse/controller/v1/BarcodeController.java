package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.service.BarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/barcode")
@RequiredArgsConstructor
@Tag(name = "Barcode Operations", description = "Endpoints for generating scannable warehouse labels")
public class BarcodeController {

    private final BarcodeService barcodeService;

    @Operation(summary = "Get Product Label", description = "Generates a 1D barcode for a product SKU.")
    @GetMapping(value = "/product/{sku}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProductBarcode(@PathVariable String sku) {
        byte[] image = barcodeService.getBarcodeForProduct(sku);
        return image != null ? ResponseEntity.ok().body(image) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get Bin Label", description = "Generates a 1D barcode for a specific storage bin code.")
    @GetMapping(value = "/bin/{binCode}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBinBarcode(@PathVariable String binCode) {
        byte[] image = barcodeService.getBarcodeForBin(binCode);
        return image != null ? ResponseEntity.ok().body(image) : ResponseEntity.notFound().build();
    }
}