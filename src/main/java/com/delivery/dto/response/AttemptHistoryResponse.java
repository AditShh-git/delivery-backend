package com.delivery.dto.response;

import com.delivery.entity.AttemptHistory;

import java.time.OffsetDateTime;

public record AttemptHistoryResponse(
        Long id,
        Long orderId,
        Long riderId,
        String riderName,
        Integer attemptNumber,
        String failureReason,
        String recordedBy,
        OffsetDateTime createdAt
) {
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
                entity.getCreatedAt()
        );
    }
}