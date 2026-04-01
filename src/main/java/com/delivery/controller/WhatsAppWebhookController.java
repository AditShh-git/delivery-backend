package com.delivery.controller;

import com.delivery.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook", description = "Endpoint for processing WhatsApp delivery replies")
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService webhookService;

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/whatsapp")
    @Operation(summary = "Handle WhatsApp Reply", description = "Process incoming user actions from WhatsApp messages")
    public ResponseEntity<String> handleReply(
            @RequestParam Long orderId,
            @RequestParam String action,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {

        // HTTP-layer auth guard — intentionally kept in controller
        if (!webhookSecret.equals(secret)) {
            log.warn("Rejected WhatsApp webhook — invalid secret");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        log.info("WhatsApp webhook received — orderId={}, action={}", orderId, action);
        return ResponseEntity.ok(webhookService.handleWebhookAction(orderId, action));
    }
}