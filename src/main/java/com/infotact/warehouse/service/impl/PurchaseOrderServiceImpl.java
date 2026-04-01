package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.PurchaseOrder;
import com.infotact.warehouse.entity.PurchaseOrderItem;
import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.exception.EntityNotFoundException;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.PurchaseOrderRepository;
import com.infotact.warehouse.repository.SupplierRepository;
import com.infotact.warehouse.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setOrderDate(LocalDateTime.now());
        po.setStatus("PLACED");

        List<PurchaseOrderItem> items = request.items().stream().map(itemRequest -> {
            Product product = productRepository.findBySku(itemRequest.sku())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + itemRequest.sku()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(po);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            return item;
        }).toList();

        po.setItems(items);
        return poRepository.save(po);
    }

    @Override
    public PurchaseOrder getPurchaseOrder(String id) {
        log.info("Fetching Purchase Order with ID: {}", id);
        return poRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with ID: " + id));
    }

    @Override
    public List<PurchaseOrder> getAllPurchaseOrders(String status) {
        log.info("Fetching all Purchase Orders with status: {}", status);

        // If a status is provided, filter by it; otherwise, return all orders.
        if (status != null && !status.isBlank()) {
            return poRepository.findAllByStatus(status);
        }
        return poRepository.findAll();
    }
}