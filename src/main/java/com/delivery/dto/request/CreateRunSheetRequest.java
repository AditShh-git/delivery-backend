package com.delivery.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateRunSheetRequest(
        @NotNull Long riderId,
        @NotNull String zone,
        @NotNull LocalDate slotDate
) {}
