package com.delivery.service;

public interface WhatsAppWebhookService {

    /**
     * Processes an inbound WhatsApp reply action for the given order.
     *
     * @param orderId the order being acted upon
     * @param action  the rider/customer reply (e.g. CONFIRM, CANCEL, SLOT_9_12)
     * @return a human-readable response message to send back via WhatsApp
     */
    String handleWebhookAction(Long orderId, String action);
}
