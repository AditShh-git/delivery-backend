package com.delivery.dto.request;

import com.delivery.entity.DeliveryModel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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

        @NotBlank
        String zone,

        @NotNull
        DeliveryModel deliveryModel,

        String missedSlotAction,
        Integer maxReschedules,
        BigDecimal penaltyAmount,
        Map<String, List<String>> pickupChecklist,

        @NotEmpty
        List<String> productCategories
) {}
