package com.delivery.dto.response;

import com.delivery.entity.RunSheet;
import com.delivery.entity.RunSheetOrder;

import java.time.LocalDate;
import java.util.List;

public record RunSheetResponse(
        Long id,
        Long riderId,
        String riderName,
        String zone,
        LocalDate slotDate,
        String status,
        List<RunSheetOrderItem> orders
) {

    public record RunSheetOrderItem(
            Long id,
            Long orderId,
            Integer sequenceNum,
            String deliveryAddress,
            String zone,
            Double deliveryLat,
            Double deliveryLng
    ) {
        public static RunSheetOrderItem from(RunSheetOrder rso) {
            return new RunSheetOrderItem(
                    rso.getId(),
                    rso.getOrder().getId(),
                    rso.getSequenceNum(),
                    rso.getOrder().getDeliveryAddress(),
                    rso.getOrder().getZone(),
                    rso.getOrder().getDeliveryLat(),
                    rso.getOrder().getDeliveryLng()
            );
        }
    }

    public static RunSheetResponse from(RunSheet sheet) {
        String riderName = sheet.getRider().getUser() != null
                ? sheet.getRider().getUser().getFullName()
                : null;

        List<RunSheetOrderItem> items = sheet.getOrders().stream()
                .map(RunSheetOrderItem::from)
                .toList();

        return new RunSheetResponse(
                sheet.getId(),
                sheet.getRider().getId(),
                riderName,
                sheet.getZone(),
                sheet.getSlotDate(),
                sheet.getStatus().name(),
                items
        );
    }
}
