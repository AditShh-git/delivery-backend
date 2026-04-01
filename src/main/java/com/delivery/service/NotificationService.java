package com.delivery.service;

public interface NotificationService {

    void sendWhatsApp(String to, String message);

    void sendOtp(String to, String otpMessage);

    void sendPreArrival(String to, String message);

    void sendOutForDelivery(com.delivery.entity.Order order);
}
