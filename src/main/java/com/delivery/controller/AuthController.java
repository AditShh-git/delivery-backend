package com.delivery.controller;

import com.delivery.dto.request.LoginRequest;
import com.delivery.dto.request.SignupRequest;
import com.delivery.dto.response.AuthResponse;
import com.delivery.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "User Signup", description = "Register a new user")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        log.info("POST /api/auth/signup — email: {}", request.email());
        AuthResponse response = authService.signup(request);
        log.info("Signup response sent — email: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate a user and get JWT token")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login — email: {}", request.email());
        AuthResponse response = authService.login(request);
        log.info("Login response sent — email: {}", request.email());
        return ResponseEntity.ok(response);
    }
}