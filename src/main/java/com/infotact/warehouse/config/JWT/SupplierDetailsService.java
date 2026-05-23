package com.infotact.warehouse.config.JWT;

import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import com.infotact.warehouse.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security {@link UserDetailsService} for {@link Supplier} accounts.
 * <p>
 * Used by {@link SupplierAuthProvider} so suppliers authenticate against the
 * {@code suppliers} table, completely independent of warehouse staff in {@code users}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SupplierDetailsService implements UserDetailsService {

    private final SupplierRepository supplierRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Supplier supplier = supplierRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Supplier not found: " + email));

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new DisabledException("Supplier account is " + supplier.getStatus());
        }

        return new SupplierPrincipal(supplier);
    }
}
