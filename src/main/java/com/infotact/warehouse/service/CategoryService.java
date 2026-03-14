package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;


public interface CategoryService {


    public ProductCategoryResponse addCategory(ProductCategoryRequest request);


    public ProductCategoryResponse getCategory(String id);

}
