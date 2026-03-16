package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    public final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request){

        log.info("Creating new Product.");
        return new ResponseEntity<>(productService.addProduct(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}") // e.g., /api/v1/products/2c8f2ce6...
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id){
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/sku/{sku}") // Changed this! e.g., /api/v1/products/sku/SONY-WH1000-B
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku){
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {

        log.info("REST request to update product with id: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }




}
