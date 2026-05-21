package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.SupplierProfileUpdateRequest;
import com.infotact.warehouse.dto.v1.request.SupplierRegistrationRequest;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String register(SupplierRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Email already registered.");
        }
        if (userRepository.existsByContactNumber(request.getContactNumber())) {
            throw new AlreadyExistsException("Contact number already registered.");
        }

        User supplier = new User();
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setContactNumber(request.getContactNumber());
        supplier.setPassword(passwordEncoder.encode(request.getPassword()));
        supplier.setRole(Role.SUPPLIER);
        supplier.setStatus(UserStatus.ACTIVE);
        supplier.setWarehouse(null);
        supplier.setCompanyName(request.getCompanyName());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setAddress(request.getAddress());
        supplier.setWebsite(request.getWebsite());

        userRepository.save(supplier);
        log.info("New supplier registered: {}", request.getEmail());
        return "Supplier account created successfully. You may now log in.";
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getMyProfile() {
        return new SupplierResponse(getAuthenticatedSupplier());
    }

    @Override
    @Transactional
    public String updateMyProfile(SupplierProfileUpdateRequest request) {
        User supplier = getAuthenticatedSupplier();

        if (request.getName() != null)          supplier.setName(request.getName());
        if (request.getContactNumber() != null) {
            if (userRepository.existsByContactNumber(request.getContactNumber()) &&
                    !supplier.getContactNumber().equals(request.getContactNumber())) {
                throw new AlreadyExistsException("Contact number already taken.");
            }
            supplier.setContactNumber(request.getContactNumber());
        }
        if (request.getCompanyName() != null) supplier.setCompanyName(request.getCompanyName());
        if (request.getGstNumber()   != null) supplier.setGstNumber(request.getGstNumber());
        if (request.getAddress()     != null) supplier.setAddress(request.getAddress());
        if (request.getWebsite()     != null) supplier.setWebsite(request.getWebsite());

        userRepository.save(supplier);
        return "Supplier profile updated successfully.";
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAllSuppliers(Pageable pageable) {
        return userRepository.findByRole(Role.SUPPLIER, pageable).map(SupplierResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(String supplierId) {
        User supplier = userRepository.findById(supplierId)
                .filter(u -> u.getRole() == Role.SUPPLIER)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
        return new SupplierResponse(supplier);
    }

    private User getAuthenticatedSupplier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile not found."));
    }
}
