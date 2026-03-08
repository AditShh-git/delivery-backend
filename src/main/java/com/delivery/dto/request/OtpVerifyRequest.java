package com.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(

                @NotBlank(message = "OTP is required") @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits") String otp

) {
}
