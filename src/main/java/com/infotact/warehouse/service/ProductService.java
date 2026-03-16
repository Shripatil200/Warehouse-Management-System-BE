package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse addProduct(ProductRequest request);

    ProductResponse getProductById(String id);

    ProductResponse getProductBySku(String sku);




}
