package com.delivery.entity;

public enum DeliveryModel {
    INSTANT,        // food, medicine, grocery — assign immediately
    PARCEL,         // eCommerce — WhatsApp morning of slotDate
    PICKUP_RETURN   // customer collects from store
}
