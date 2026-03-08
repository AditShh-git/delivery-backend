package com.delivery.service.impl;

import com.delivery.dto.response.OtpResponse;
import com.delivery.entity.DeliveryOtp;
import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.exception.ApiException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.DeliveryOtpRepository;
import com.delivery.repository.OrderRepository;
import com.delivery.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_WRONG_ATTEMPTS = 3;

    private final OrderRepository orderRepository;
    private final DeliveryOtpRepository deliveryOtpRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt from SecurityConfig

    // ─── SEND OTP ──────────────────────────────────────────────────────────
    //
    // Strategy: Option-B — always insert a new row; guard always uses latest row.
    // Previous unverified OTPs are naturally superseded because findTopBy...Desc
    // returns only the newest one.

    @Override
    @Transactional
    public OtpResponse sendOtp(Long orderId, Long riderId) {

        log.info("OTP send requested — orderId: {}, riderId: {}", orderId, riderId);

        Order order = findOrder(orderId);

        // ── Guard: order must be IN_TRANSIT ────────────────────────────────
        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new ApiException(
                    "OTP can only be sent when order is IN_TRANSIT. Current status: "
                            + order.getStatus());
        }

        // ── Guard: caller must be the assigned rider ────────────────────────
        validateRiderAssignment(order, riderId);

        // ── Generate raw 6-digit OTP ────────────────────────────────────────
        String rawOtp = generateRawOtp();

        // ── Hash & persist ──────────────────────────────────────────────────
        DeliveryOtp otp = new DeliveryOtp();
        otp.setOrder(order);
        otp.setOtpHash(passwordEncoder.encode(rawOtp));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setVerified(false);
        otp.setWrongAttempts(0);

        deliveryOtpRepository.save(otp);

        // ── Simulated SMS (log only — swap for real provider later) ─────────
        log.info("[SMS-SIMULATION] OTP for order {}: {} (expires in {} min)",
                orderId, rawOtp, OTP_EXPIRY_MINUTES);

        return new OtpResponse("OTP sent successfully. Valid for " + OTP_EXPIRY_MINUTES + " minutes.", false);
    }

    // ─── VERIFY OTP ────────────────────────────────────────────────────────
    //
    // Check order: verified → expired → wrongAttempts ≥ 3 → BCrypt match.
    // Once verified, expiry is no longer checked (correct per the spec).

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public OtpResponse verifyOtp(Long orderId, Long riderId, String rawOtp) {

        log.info("OTP verify requested — orderId: {}, riderId: {}", orderId, riderId);

        Order order = findOrder(orderId);

        // ── Guard: caller must be the assigned rider ────────────────────────
        validateRiderAssignment(order, riderId);

        // ── Fetch latest OTP for this order ─────────────────────────────────
        DeliveryOtp otp = deliveryOtpRepository
                .findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ApiException(
                        "No OTP found for order " + orderId + ". Rider must call /otp/send first."));

        // ── Check 1: Already verified — reject re-verification ───────────────
        if (otp.isVerified()) {
            throw new ApiException("OTP has already been verified for order " + orderId + ".");
        }

        // ── Check 2: Expired ─────────────────────────────────────────────────
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            throw new ApiException(
                    "OTP has expired. Please ask the rider to send a new OTP.");
        }

        // ── Check 3: Too many wrong attempts — permanently locked ────────────
        if (otp.getWrongAttempts() >= MAX_WRONG_ATTEMPTS) {
            throw new ApiException(
                    "OTP is locked after " + MAX_WRONG_ATTEMPTS
                            + " wrong attempts. Rider must send a new OTP.");
        }

        // ── BCrypt comparison ─────────────────────────────────────────────────
        if (passwordEncoder.matches(rawOtp, otp.getOtpHash())) {

            otp.setVerified(true);
            otp.setVerifiedAt(LocalDateTime.now()); // audit trail
            deliveryOtpRepository.save(otp);

            log.info("OTP verified successfully — orderId: {}", orderId);
            return new OtpResponse("OTP verified. Delivery can now be completed.", true);

        } else {

            int newAttempts = otp.getWrongAttempts() + 1;
            otp.setWrongAttempts(newAttempts);
            deliveryOtpRepository.save(otp);

            int remaining = MAX_WRONG_ATTEMPTS - newAttempts;

            if (newAttempts >= MAX_WRONG_ATTEMPTS) {
                log.warn("OTP locked — orderId: {} reached {} wrong attempts", orderId, MAX_WRONG_ATTEMPTS);
                throw new ApiException(
                        "Incorrect OTP. OTP is now locked after " + MAX_WRONG_ATTEMPTS
                                + " wrong attempts. Rider must send a new OTP.");
            }

            log.warn("Wrong OTP attempt {}/{} — orderId: {}", newAttempts, MAX_WRONG_ATTEMPTS, orderId);
            throw new ApiException(
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }
    }

    // ─── PRIVATE HELPERS ───────────────────────────────────────────────────

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private void validateRiderAssignment(Order order, Long riderId) {
        if (order.getRider() == null) {
            throw new ApiException("No rider is assigned to order " + order.getId() + ".");
        }
        if (!order.getRider().getUser().getId().equals(riderId)) {
            throw new ApiException("You are not the assigned rider for order " + order.getId() + ".");
        }
    }

    private String generateRawOtp() {
        // SecureRandom ensures cryptographic quality randomness
        SecureRandom random = new SecureRandom();
        int code = 100_000 + random.nextInt(900_000); // always 6 digits
        return String.valueOf(code);
    }
}
