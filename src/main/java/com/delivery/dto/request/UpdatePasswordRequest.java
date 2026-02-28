package com.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePasswordRequest(

        @NotBlank
        String oldPassword,

        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[@#$%^&+=]).{8,}$",
                message = "Password must be 8+ chars, include upper, lower, number, special char"
        )
        String newPassword
) {}
