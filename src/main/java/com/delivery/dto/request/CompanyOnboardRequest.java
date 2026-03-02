package com.delivery.dto.request;

import com.delivery.entity.DeliveryModel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CompanyOnboardRequest(

        @NotNull
        DeliveryModel deliveryModel,

        @NotEmpty
        List<String> productCategories,

        String missedSlotAction,
        Integer maxReschedules,
        BigDecimal penaltyAmount,
        Map<String, List<String>> pickupChecklist
) {}