package com.delivery.slot;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateSlotRequest(
        @NotNull  Long      companyId,
        @NotBlank String    zone,
        @NotNull  LocalDate slotDate,
        @NotBlank String    slotLabel,
        @Min(1)   int       capacity
) {}
