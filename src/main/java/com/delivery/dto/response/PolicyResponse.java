package com.delivery.dto.response;

import com.delivery.entity.DeliveryType;

import java.math.BigDecimal;

public record PolicyResponse(
        String productCategory,
        DeliveryType deliveryType,
        String missedSlotAction,
        Integer maxReschedules,
        BigDecimal penaltyAmount
) {}