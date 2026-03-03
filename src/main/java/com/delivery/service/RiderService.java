package com.delivery.service;

import com.delivery.dto.request.CreateRiderRequest;
import com.delivery.dto.request.UpdatePasswordRequest;
import com.delivery.dto.request.UpdateRiderRequest;
import com.delivery.dto.response.RiderResponse;

public interface RiderService {
    RiderResponse createRider(CreateRiderRequest request,
            Long creatorUserId,
            String creatorRole);

    RiderResponse updateRider(Long riderId,
            UpdateRiderRequest request,
            Long userId,
            String role);

    void updatePassword(Long riderId,
            UpdatePasswordRequest request,
            Long userId,
            String role);

    RiderResponse setDutyStatus(Long riderId, boolean onDuty, int maxConcurrentOrders,
            Long callerUserId, String callerRole);

}
