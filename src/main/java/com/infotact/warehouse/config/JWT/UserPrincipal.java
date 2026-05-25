package com.infotact.warehouse.config.JWT;

import com.infotact.warehouse.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal for warehouse {@link User} accounts.
 * <p>
 * Carries the {@code id} and {@code warehouseId} claims so the service/controller layer can enforce
 * warehouse-scoped data isolation and task assignments without extra DB queries.
 * </p>
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final String id; // Added to fix the controller 'getUserId' compilation errors
    private final String email;
    private final String password;
    private final String warehouseId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id          = user.getId(); // Map the entity ID here
        this.email       = user.getEmail();
        this.password    = user.getPassword();
        this.warehouseId = user.getWarehouse().getId();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public String getUserId() {
        return this.id;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                     { return password; }
    @Override public String getUsername()                                     { return email; }
    @Override public boolean isAccountNonExpired()                            { return true; }
    @Override public boolean isAccountNonLocked()                             { return true; }
    @Override public boolean isCredentialsNonExpired()                        { return true; }
    @Override public boolean isEnabled()                                      { return true; }
}