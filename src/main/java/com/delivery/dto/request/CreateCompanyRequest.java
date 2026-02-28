package com.delivery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateCompanyRequest(

        @NotBlank
        String name,

        @NotBlank
        String contact,

        @NotBlank
        @Email
        String email,

        String missedSlotAction,

        Integer maxReschedules,

        BigDecimal penaltyAmount,

        Map<String, List<String>> pickupChecklist,

        @NotBlank
        String productCategory

) {}
