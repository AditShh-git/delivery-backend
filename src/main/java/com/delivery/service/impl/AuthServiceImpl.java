package com.delivery.service.impl;

import com.delivery.dto.request.LoginRequest;
import com.delivery.dto.request.SignupRequest;
import com.delivery.dto.response.AuthResponse;
import com.delivery.entity.Company;
import com.delivery.entity.Role;
import com.delivery.exception.ApiException;
import com.delivery.exception.EmailAlreadyExistsException;
import com.delivery.repository.*;
import com.delivery.security.JwtUtils;
import com.delivery.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder   passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils          jwtUtils;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Signup attempt — email: {}, role: {}", request.email(), request.role());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Signup failed — email already exists: {}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        if (userRepository.existsByPhone(request.phone())) {
            log.warn("Signup failed — phone already exists: {}", request.phone());
            throw new ApiException("Phone already registered: " + request.phone());
        }

        if ("ADMIN".equalsIgnoreCase(request.role())) {
            log.warn("Signup blocked — ADMIN self-registration attempt by: {}", request.email());
            throw new ApiException("Cannot self-register as ADMIN");
        }

        Role role = roleRepository.findByName(request.role().toUpperCase())
                .orElseThrow(() -> {
                    log.warn("Signup failed — role not found: {}", request.role());
                    return new ApiException("Role not found: " + request.role());
                });

        com.delivery.entity.User user = new com.delivery.entity.User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.getRoles().add(role);

        if ("COMPANY".equalsIgnoreCase(request.role())) {

            String normalizedEmail = request.email().trim().toLowerCase();
            String normalizedPhone = request.phone().trim();

            if (companyRepository.existsByEmail(normalizedEmail)) {
                throw new ApiException("Company already registered with this email");
            }

            Company company = new Company();
            company.setName(request.fullName().trim() + " Company");
            company.setEmail(normalizedEmail);
            company.setContact(normalizedPhone);
            // deliveryModel default remains SCHEDULED

            companyRepository.save(company);
            user.setCompany(company);

            log.info("Company profile created for: {}", request.email());
        }

        userRepository.save(user);
        log.info("User saved — email: {}, role: {}", user.getEmail(), role.getName());

        if ("RIDER".equalsIgnoreCase(request.role())) {
            throw new ApiException("Riders must be created by a company or admin");
        }

        log.info("Signup successful — email: {}, role: {}", user.getEmail(), role.getName());
        return generateAuthResponse(user, "Signup successful — welcome " + user.getFullName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt — email: {}", request.email());

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed — bad credentials for: {}", request.email());
            throw new ApiException("Invalid email or password");
        } catch (DisabledException e) {
            log.warn("Login failed — account disabled: {}", request.email());
            throw new ApiException("Account is disabled");
        } catch (LockedException e) {
            log.warn("Login failed — account locked: {}", request.email());
            throw new ApiException("Account is locked");
        }

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found: {}", request.email());
                    return new ApiException("User not found: " + request.email());
                });

        log.info("Login successful — email: {}, roles: {}",
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).toList());

        return generateAuthResponse(user, "Login successful — welcome back " + user.getFullName());
    }

    private AuthResponse generateAuthResponse(com.delivery.entity.User user, String message) {
        List<String> roles = user.getRoles().stream()
                .map(r -> "ROLE_" + r.getName())
                .toList();

        var springUser = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(roles.stream().map(SimpleGrantedAuthority::new).toList())
                .build();

        String token = jwtUtils.generateToken(springUser);

        log.debug("Token generated for: {} | roles: {}", user.getEmail(), roles);

        return new AuthResponse(message, token, user.getEmail(), roles);
    }
}