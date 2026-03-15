package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface CategoryService {


    public ProductCategoryResponse addCategory(ProductCategoryRequest request);


    public ProductCategoryResponse getCategory(String id);

    Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive);

    void deleteCategory(String id);

    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request);

    ProductCategoryResponse deactivateCategory(String id);

    ProductCategoryResponse activateCategory(String id);

}
