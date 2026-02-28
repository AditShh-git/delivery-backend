package com.delivery.dto.response;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String code,
        String message,
        int status,
        OffsetDateTime timestamp
) {}
