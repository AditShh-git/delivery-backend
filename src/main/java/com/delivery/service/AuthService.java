package com.delivery.service;

import com.delivery.dto.request.LoginRequest;
import com.delivery.dto.request.SignupRequest;
import com.delivery.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}
