package com.infotact.warehouse.config.JWT;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service to load user-specific data from the database.
 * <p>
 * Validates that the account exists and is currently in 'ACTIVE' status
 * before granting access.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UsersDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        // ACCOUNT STATUS CHECK
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Your account status is " + user.getStatus() + ". Login restricted.");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name()) // Maps e.g. "ADMIN" to ROLE_ADMIN
                .build();
    }
}