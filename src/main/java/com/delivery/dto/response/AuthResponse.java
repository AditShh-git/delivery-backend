package com.delivery.dto.response;

import java.util.List;

public record AuthResponse(
        String message,
        String token,
        String email,
        List<String> roles
) {}
