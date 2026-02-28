package com.delivery.dto.response;

public record RiderResponse(
        Long riderId,
        String fullName,
        String email,
        String companyName,
        boolean isAvailable
) {}
