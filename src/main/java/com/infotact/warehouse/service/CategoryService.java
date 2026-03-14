package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;

import java.util.List;


public interface CategoryService {


    public ProductCategoryResponse addCategory(ProductCategoryRequest request);


    public ProductCategoryResponse getCategory(String id);

    public List<ProductCategoryResponse> getAllCategories();

}
