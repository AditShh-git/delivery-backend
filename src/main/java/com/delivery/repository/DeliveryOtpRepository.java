package com.delivery.repository;

import com.delivery.entity.DeliveryOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryOtpRepository extends JpaRepository<DeliveryOtp, Long> {

    /**
     * Returns the most-recently created OTP for the given order.
     * OTP send uses Option-B (always inserts a new row); this guard always
     * picks the latest one, so stale OTPs are automatically ignored.
     */
    Optional<DeliveryOtp> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
}
