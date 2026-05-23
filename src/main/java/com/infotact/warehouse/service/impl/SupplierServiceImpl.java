package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.JWT.JwtUtil;
import com.infotact.warehouse.config.JWT.SupplierPrincipal;
import com.infotact.warehouse.dto.v1.request.SupplierLoginRequest;
import com.infotact.warehouse.dto.v1.request.SupplierProfileUpdateRequest;
import com.infotact.warehouse.dto.v1.request.SupplierRegistrationRequest;
import com.infotact.warehouse.dto.v1.response.AuthResponse;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.SupplierRepository;
import com.infotact.warehouse.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository    supplierRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;

    // Named qualifier — the supplier-specific AuthenticationManager
    private final AuthenticationManager supplierAuthenticationManager;

    @Override
    @Transactional
    public String register(SupplierRegistrationRequest request) {
        if (supplierRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Email already registered.");
        }
        if (supplierRepository.existsByContactNumber(request.getContactNumber())) {
            throw new AlreadyExistsException("Contact number already registered.");
        }

        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setContactNumber(request.getContactNumber());
        supplier.setPassword(passwordEncoder.encode(request.getPassword()));
        supplier.setCompanyName(request.getCompanyName());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setAddress(request.getAddress());
        supplier.setWebsite(request.getWebsite());
        supplier.setStatus(SupplierStatus.ACTIVE);

        supplierRepository.save(supplier);
        log.info("New supplier registered: {}", request.getEmail());
        return "Supplier account created successfully. You may now log in.";
    }

    @Override
    public AuthResponse login(SupplierLoginRequest request) {
        try {
            supplierAuthenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password.");
        }

        Supplier supplier = supplierRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BadRequestException("Account is " + supplier.getStatus() + ". Please contact support.");
        }

        SupplierPrincipal principal = new SupplierPrincipal(supplier);
        String token = jwtUtil.generateToken(principal);

        log.info("Supplier login successful: {}", supplier.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(supplier.getEmail())
                .role("SUPPLIER")
                .supplierId(supplier.getId())
                .warehouseId(null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getMyProfile() {
        return new SupplierResponse(getAuthenticatedSupplier());
    }

    @Override
    @Transactional
    public String updateMyProfile(SupplierProfileUpdateRequest request) {
        Supplier supplier = getAuthenticatedSupplier();

        if (request.getName()          != null) supplier.setName(request.getName());
        if (request.getCompanyName()   != null) supplier.setCompanyName(request.getCompanyName());
        if (request.getGstNumber()     != null) supplier.setGstNumber(request.getGstNumber());
        if (request.getAddress()       != null) supplier.setAddress(request.getAddress());
        if (request.getWebsite()       != null) supplier.setWebsite(request.getWebsite());
        if (request.getContactNumber() != null) {
            if (supplierRepository.existsByContactNumber(request.getContactNumber())
                    && !supplier.getContactNumber().equals(request.getContactNumber())) {
                throw new AlreadyExistsException("Contact number already taken.");
            }
            supplier.setContactNumber(request.getContactNumber());
        }

        supplierRepository.save(supplier);
        return "Profile updated successfully.";
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(SupplierResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(String supplierId) {
        return supplierRepository.findById(supplierId)
                .map(SupplierResponse::new)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));
    }

    private Supplier getAuthenticatedSupplier() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return supplierRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }
}
