package com.delivery.projection;

public interface OrderKpiProjection {

    Long getTotalOrders();
    Long getTotalDelivered();
    Long getTotalFailed();
    Long getSlaBreached();
}
