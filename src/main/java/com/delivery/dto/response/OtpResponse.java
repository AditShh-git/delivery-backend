package com.delivery.dto.response;

public record OtpResponse(
        String message,
        boolean verified) {
}
