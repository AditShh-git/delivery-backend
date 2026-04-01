package com.delivery.service;

import com.delivery.dto.request.CreateRunSheetRequest;
import com.delivery.dto.response.RunSheetResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface RunSheetService {

    /** Admin/Company creates a RunSheet for a rider on a given date */
    RunSheetResponse create(CreateRunSheetRequest request, Long callerUserId, String callerRole);

    /** Add one or more orderId(s) to a RunSheet */
    RunSheetResponse addOrders(Long runSheetId, List<Long> orderIds, Long callerUserId, String callerRole);

    /** Trigger nearest-neighbor sort on a DRAFT RunSheet */
    RunSheetResponse sortRoute(Long runSheetId, Long callerUserId, String callerRole);

    /** Write CSV directly to the HTTP response stream */
    void exportCsv(Long runSheetId, HttpServletResponse response);

    /** Called by GET /api/rider/today — returns today's RunSheet for the rider */
    RunSheetResponse getRiderToday(Long userId);
}
