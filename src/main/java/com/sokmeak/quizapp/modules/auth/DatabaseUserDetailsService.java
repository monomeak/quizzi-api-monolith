package com.sokmeak.quizapp.modules.auth;

import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Teaches Spring Security how to look an account up in app_user. Without this the
 * AuthenticationManager has nothing to authenticate against.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                // Message is only for our logs; the login endpoint never echoes it back.
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' was not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                // The "ROLE_" prefix is what hasRole(...) and the JWT filter both expect.
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
