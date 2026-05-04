package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.service.BarcodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/barcode")
@RequiredArgsConstructor
public class BarcodeController {

    private final BarcodeService barcodeService;

    @GetMapping(value = "/{sku}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProductBarcode(@PathVariable String sku) {
        byte[] image = barcodeService.getBarcodeForProduct(sku);
        return ResponseEntity.ok().body(image);
    }
}