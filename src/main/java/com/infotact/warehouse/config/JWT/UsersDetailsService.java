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
 * Spring Security {@link UserDetailsService} for warehouse staff ({@code users} table).
 * Spring Security {@link UserDetailsService} for warehouse {@link User} accounts.
 */
@Service
@RequiredArgsConstructor
public class UsersDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Login restricted. Account status: " + user.getStatus());
        }

        if (user.getRole() == com.infotact.warehouse.entity.enums.Role.EMPLOYEE) {
            throw new DisabledException("Your account is put on hold. Please contact Admin.");
        }

        return new UserPrincipal(user);
    }
}
