package com.delivery.dto.request;

import com.delivery.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateStatusRequest(
                @NotNull OrderStatus status,

                // ── Required when status = FAILED ────────────────────────────────────
                String failureReason,

                // GPS coordinates — rider's location at the time of failed attempt.
                // Must not be null when status = FAILED (enforced in OrderServiceImpl).
                BigDecimal latitude,
                BigDecimal longitude,

                // Photo proof URL (e.g., S3/CDN link uploaded by the rider app).
                // Must not be blank when status = FAILED (enforced in OrderServiceImpl).
                String photoUrl) {
}
