package com.delivery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRiderRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        String fullName,

        @Email
        @NotBlank
        String email,

        @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
        String phone,

        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[@#$%^&+=]).{8,}$",
                message = "Password must be 8+ chars, include upper, lower, number, special char"
        )
        String password,

        @NotBlank
        String vehicleType,

        @NotBlank
        String licensePlate,

        @NotBlank
        String zone,

        Long companyId
) {}
