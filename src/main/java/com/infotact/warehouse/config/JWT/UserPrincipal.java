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
 * Carries the {@code warehouseId} claim so the service layer can enforce
 * multi-tenant data isolation without extra DB queries. Since every {@link User}
 * now always has a warehouse (nullable = false), {@code warehouseId} is never null here.
 * </p>
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final String email;
    private final String password;
    private final String warehouseId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.email       = user.getEmail();
        this.password    = user.getPassword();
        this.warehouseId = user.getWarehouse().getId();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                     { return password; }
    @Override public String getUsername()                                     { return email; }
    @Override public boolean isAccountNonExpired()                            { return true; }
    @Override public boolean isAccountNonLocked()                             { return true; }
    @Override public boolean isCredentialsNonExpired()                        { return true; }
    @Override public boolean isEnabled()                                      { return true; }
}
