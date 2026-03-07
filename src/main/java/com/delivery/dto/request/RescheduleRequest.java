package com.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RescheduleRequest(

        @NotNull(message = "New slot date is required") LocalDate newSlotDate,

        @NotBlank(message = "New slot label is required") String newSlotLabel

) {
}
