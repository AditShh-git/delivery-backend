package com.delivery.service.impl;

import com.delivery.dto.request.LoginRequest;
import com.delivery.dto.request.SignupRequest;
import com.delivery.dto.response.AuthResponse;
import com.delivery.entity.Company;
import com.delivery.entity.CompanyStatus;
import com.delivery.entity.Role;
import com.delivery.entity.User;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {

        log.info("Signup attempt — email: {}, role: {}", request.email(), request.role());

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        if (userRepository.existsByPhone(request.phone())) {
            throw new ApiException("Phone already registered: " + request.phone());
        }

        String roleName = request.role().toUpperCase();

        if (Role.ADMIN.equals(roleName)) {
            throw new ApiException("Cannot self-register as ADMIN");
        }

        if (Role.RIDER.equals(roleName)) {
            throw new ApiException("Riders must be created by a company or admin");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ApiException("Role not found: " + roleName));

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        user.getRoles().add(role);

        // ── COMPANY SELF-SIGNUP FLOW ───────────────────────────────────
        if (Role.COMPANY.equals(roleName)) {

            String normalizedEmail = user.getEmail();
            String normalizedPhone = user.getPhone();

            if (companyRepository.existsByEmail(normalizedEmail)) {
                throw new ApiException("Company already registered with this email");
            }

            Company company = new Company();
            company.setName(user.getFullName() + " Company");
            company.setEmail(normalizedEmail);
            company.setContact(normalizedPhone);
            company.setStatus(CompanyStatus.PENDING); // Explicit

            companyRepository.save(company);
            user.setCompany(company);

            log.info("Company created with PENDING status for {}", normalizedEmail);
        }

        userRepository.save(user);

        return generateAuthResponse(user,
                "Signup successful — welcome " + user.getFullName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt — email: {}", request.email());

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()));
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

        // ── COMPANY STATUS GATE ────────────────────────────────────────────
        // Block login if the company account has not been approved by admin.
        // This prevents PENDING or SUSPENDED companies from obtaining a JWT.
        boolean isCompanyUser = user.getRoles().stream()
                .anyMatch(r -> r.is(Role.COMPANY));

        if (isCompanyUser) {
            Company company = user.getCompany();
            if (company == null) {
                log.warn("Login blocked — COMPANY user {} has no company profile", user.getEmail());
                throw new ApiException("No company profile linked to this account. Contact admin.");
            }
            if (company.getStatus() != CompanyStatus.ACTIVE) {
                log.warn("Login blocked — company {} status is {}", company.getId(), company.getStatus());
                throw new ApiException(
                        "Company account is not active. Current status: " + company.getStatus()
                                + ". Please contact admin for approval.");
            }
        }

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