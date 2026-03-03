package com.delivery.utils;

import com.delivery.exception.ApiException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    public String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElseThrow(() -> new ApiException("Authenticated user has no role assigned"));
    }
}
