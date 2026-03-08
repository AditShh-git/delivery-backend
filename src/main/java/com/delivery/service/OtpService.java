package com.delivery.service;

import com.delivery.dto.response.OtpResponse;

public interface OtpService {

    /**
     * Called by rider on arrival at customer's address.
     * Generates a random 6-digit OTP, BCrypt-hashes it, saves it,
     * and simulates SMS delivery (logs the plaintext for testing).
     *
     * @param orderId order the OTP is for
     * @param riderId must be the assigned rider for this order
     */
    OtpResponse sendOtp(Long orderId, Long riderId);

    /**
     * Called by rider after receiving the OTP from the customer.
     * Validates expiry, verified-flag, and wrong-attempt count before
     * BCrypt-matching. Increments wrongAttempts on failure; locks after 3.
     *
     * @param orderId order the OTP belongs to
     * @param riderId must be the assigned rider for this order
     * @param rawOtp  plaintext OTP entered by the rider
     */
    OtpResponse verifyOtp(Long orderId, Long riderId, String rawOtp);
}
