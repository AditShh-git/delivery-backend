package com.delivery.dto.request;

public record UpdateRiderRequest(
        String vehicleType,
        String licensePlate,
        String zone,
        Boolean isAvailable
) {}
