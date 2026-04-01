package com.delivery.projection;

import com.delivery.entity.OrderStatus;

import java.time.LocalDate;

public interface RiderKpiOrderProjection {

    Long getId();

    String getZone();

    String getSlotLabel();

    LocalDate getSlotDate();

    String getDeliveryAddress();

    String getProductCategory();

    String getCustomerName(); // map via JPQL

    OrderStatus getStatus();

}
