package com.infotact.warehouse.config.JWT;

import com.infotact.warehouse.entity.Supplier;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal for {@link Supplier} accounts.
 * <p>
 * Suppliers are global — they have no warehouse context, so there is no
 * {@code warehouseId} here. The authority is always {@code ROLE_SUPPLIER}.
 * </p>
 */
@Getter
public class SupplierPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public SupplierPrincipal(Supplier supplier) {
        this.id          = supplier.getId();
        this.email       = supplier.getEmail();
        this.password    = supplier.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                     { return password; }
    @Override public String getUsername()                                     { return email; }
    @Override public boolean isAccountNonExpired()                            { return true; }
    @Override public boolean isAccountNonLocked()                             { return true; }
    @Override public boolean isCredentialsNonExpired()                        { return true; }
    @Override public boolean isEnabled()                                      { return true; }
}
