package com.delivery.dto.response;

import com.delivery.entity.AttemptHistory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AttemptHistoryResponse(
                Long id,
                Long orderId,
                Long riderId,
                String riderName,
                Integer attemptNumber,
                String failureReason,
                String recordedBy,

                // ── GPS + Photo ──────────────────────────────────────
                BigDecimal latitude,
                BigDecimal longitude,
                String photoUrl,

                OffsetDateTime createdAt) {
        public static AttemptHistoryResponse from(AttemptHistory entity) {
                return new AttemptHistoryResponse(
                                entity.getId(),
                                entity.getOrder().getId(),
                                entity.getRider() != null ? entity.getRider().getId() : null,
                                entity.getRider() != null
                                                ? entity.getRider().getUser().getFullName()
                                                : null,
                                entity.getAttemptNumber(),
                                entity.getFailureReason(),
                                entity.getRecordedBy(),
                                entity.getLatitude(),
                                entity.getLongitude(),
                                entity.getPhotoUrl(),
                                entity.getCreatedAt());
        }
}