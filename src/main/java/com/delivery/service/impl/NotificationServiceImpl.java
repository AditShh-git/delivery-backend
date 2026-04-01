package com.delivery.service.impl;

import com.delivery.service.NotificationService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.whatsappFrom}")
    private String from;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized");
    }

    @Override
    public void sendWhatsApp(String to, String message) {
        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber(from),
                    message
            ).create();

            log.info("WhatsApp sent to {}: {}", to, message);

        } catch (Exception ex) {
            log.error("Failed to send WhatsApp to {}", to, ex);
        }
    }

    @Override
    public void sendOtp(String to, String otpMessage) {
        sendWhatsApp(to, otpMessage);
    }

    @Override
    public void sendPreArrival(String to, String message) {
        sendWhatsApp(to, message);
    }

    @Override
    public void sendOutForDelivery(com.delivery.entity.Order order) {
        String message = """
            Your order will be delivered tomorrow.

            Please confirm your availability:
            1️⃣ SLOT_9_12 (9AM–12PM)
            2️⃣ SLOT_12_3 (12PM–3PM)
            3️⃣ SLOT_3_6 (3PM–6PM)
            """;

        log.info("Sending WhatsApp notification for order {}: \n{}", order.getId(), message);
    }
}
