package com.infotact.warehouse.config.JWT;

import com.infotact.warehouse.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of {@link UserDetails} for the InfoTact WMS.
 * <p>
 * This class carries the 'warehouseId' claim, allowing the Service layer
 * to enforce multi-tenant data isolation without redundant database queries.
 * </p>
 */
@Getter
public class UserPrincipal implements UserDetails {
    private final String email;
    private final String password;
    private final String warehouseId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.warehouseId = (user.getWarehouse() != null) ? user.getWarehouse().getId() : null;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}