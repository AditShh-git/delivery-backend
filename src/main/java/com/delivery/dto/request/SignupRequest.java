package com.delivery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank(message = "Password is required")

        @Size(
                min = 8,
                message = "Password must be at least 8 characters long"
        )

        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$",
                message = "Password must contain uppercase, lowercase, digit and special character"
        )

        String password,

        @NotBlank
        String fullName,

        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Phone must be valid 10 digit Indian number"
        )
        String phone,

        @NotBlank
        String role
) {}
