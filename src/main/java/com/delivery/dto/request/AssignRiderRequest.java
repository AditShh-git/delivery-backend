package com.delivery.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignRiderRequest(
        @NotNull Long riderId,
        boolean isAdminReassign
) {}
