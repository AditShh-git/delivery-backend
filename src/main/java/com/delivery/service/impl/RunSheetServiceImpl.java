package com.delivery.service.impl;

import com.delivery.dto.request.CreateRunSheetRequest;
import com.delivery.dto.response.RunSheetResponse;
import com.delivery.entity.*;
import com.delivery.exception.ApiException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.*;
import com.delivery.service.RunSheetService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunSheetServiceImpl implements RunSheetService {

    private final RunSheetRepository runSheetRepository;
    private final RunSheetOrderRepository runSheetOrderRepository;
    private final RiderRepository riderRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;

    // ─── CREATE ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RunSheetResponse create(CreateRunSheetRequest request,
                                   Long callerUserId,
                                   String callerRole) {

        Rider rider = riderRepository.findById(request.riderId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider", request.riderId()));

        // ── Company scoping: COMPANY users may only create sheets for their riders ──
        if ("COMPANY".equals(callerRole)) {
            User caller = userRepository.findById(callerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", callerUserId));

            if (caller.getCompany() == null
                    || !rider.getCompany().getId().equals(caller.getCompany().getId())) {
                throw new ApiException(
                        "Rider " + request.riderId() + " does not belong to your company.");
            }
        }

        // ── Guard: one sheet per rider per day ────────────────────────────────
        if (runSheetRepository.findByRiderIdAndSlotDate(request.riderId(), request.slotDate()).isPresent()) {
            throw new ApiException(
                    "A RunSheet already exists for rider " + request.riderId()
                    + " on " + request.slotDate());
        }

        User createdBy = userRepository.findById(callerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", callerUserId));

        RunSheet sheet = new RunSheet();
        sheet.setRider(rider);
        sheet.setZone(request.zone());
        sheet.setSlotDate(request.slotDate());
        sheet.setStatus(RunSheetStatus.DRAFT);
        sheet.setCreatedBy(createdBy);

        runSheetRepository.save(sheet);

        log.info("RunSheet created — id: {}, riderId: {}, zone: {}, date: {}",
                sheet.getId(), request.riderId(), request.zone(), request.slotDate());

        return RunSheetResponse.from(sheet);
    }

    // ─── ADD ORDERS ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RunSheetResponse addOrders(Long runSheetId,
                                      List<Long> orderIds,
                                      Long callerUserId,
                                      String callerRole) {

        RunSheet sheet = findSheet(runSheetId);

        guardNotLocked(sheet);
        guardCompanyScope(sheet, callerUserId, callerRole);

        // Resolve the company from the sheet's rider for ownership checks
        Long sheetCompanyId = sheet.getRider().getCompany().getId();

        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

            // ── Order must belong to the same company as the run sheet's rider ──
            if (!order.getCompany().getId().equals(sheetCompanyId)) {
                throw new ApiException(
                        "Order " + orderId + " does not belong to the same company as this RunSheet.");
            }

            // ── Skip duplicates silently (UNIQUE constraint also guards this) ──
            boolean alreadyAdded = sheet.getOrders().stream()
                    .anyMatch(rso -> rso.getOrder().getId().equals(orderId));
            if (alreadyAdded) {
                log.debug("RunSheet {} — order {} already present, skipping", runSheetId, orderId);
                continue;
            }

            RunSheetOrder rso = new RunSheetOrder();
            rso.setRunSheet(sheet);
            rso.setOrder(order);
            rso.setSequenceNum(0);
            sheet.getOrders().add(rso);
        }

        runSheetRepository.save(sheet);

        log.info("RunSheet {} — added {} order(s)", runSheetId, orderIds.size());

        return RunSheetResponse.from(sheet);
    }

    // ─── SORT ROUTE ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RunSheetResponse sortRoute(Long runSheetId,
                                      Long callerUserId,
                                      String callerRole) {

        RunSheet sheet = findSheet(runSheetId);

        guardNotLocked(sheet);
        guardCompanyScope(sheet, callerUserId, callerRole);

        List<RunSheetOrder> orders = sheet.getOrders();

        if (orders.isEmpty()) {
            log.warn("RunSheet {} has no orders — nothing to sort", runSheetId);
            return RunSheetResponse.from(sheet);
        }

        // ── Start point: zone centroid (fallback to null → first-insertion start) ──
        Zone zoneCentroid = zoneRepository.findByNameAndIsActiveTrue(sheet.getZone()).orElse(null);

        double startLat = (zoneCentroid != null && zoneCentroid.getLat() != null)
                ? zoneCentroid.getLat() : Double.NaN;
        double startLng = (zoneCentroid != null && zoneCentroid.getLng() != null)
                ? zoneCentroid.getLng() : Double.NaN;

        nearestNeighborSort(orders, startLat, startLng);

        runSheetRepository.save(sheet);

        log.info("RunSheet {} — route sorted ({} stops)", runSheetId, orders.size());

        return RunSheetResponse.from(sheet);
    }

    // ─── EXPORT CSV ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void exportCsv(Long runSheetId, HttpServletResponse response) {

        RunSheet sheet = findSheet(runSheetId);
        List<RunSheetOrder> orders = runSheetOrderRepository
                .findByRunSheetIdOrderBySequenceNumAscIdAsc(runSheetId);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"runsheet-" + runSheetId + ".csv\"");

        try (PrintWriter writer = response.getWriter()) {
            // Header row
            writer.println("seq,orderId,deliveryAddress,zone,deliveryLat,deliveryLng");

            for (RunSheetOrder rso : orders) {
                Order o = rso.getOrder();
                writer.printf("%d,%d,\"%s\",%s,%s,%s%n",
                        rso.getSequenceNum(),
                        o.getId(),
                        escapeCsv(o.getDeliveryAddress()),
                        o.getZone(),
                        o.getDeliveryLat() != null ? o.getDeliveryLat().toString() : "",
                        o.getDeliveryLng() != null ? o.getDeliveryLng().toString() : ""
                );
            }

            writer.flush();

        } catch (IOException e) {
            log.error("CSV export failed for runSheetId={}", runSheetId, e);
            throw new ApiException("Failed to write CSV export: " + e.getMessage());
        }

        log.info("RunSheet {} exported as CSV ({} rows)", runSheetId, orders.size());
    }

    // ─── RIDER TODAY ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RunSheetResponse getRiderToday(Long userId) {

        Rider rider = riderRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider for user", userId));

        RunSheet sheet = runSheetRepository
                .findByRiderIdAndSlotDate(rider.getId(), LocalDate.now())
                .orElseThrow(() -> new ApiException(
                        "No RunSheet found for today (" + LocalDate.now() + ")"));

        return RunSheetResponse.from(sheet);
    }

    // ─── NEAREST-NEIGHBOR SORT ─────────────────────────────────────────────
    //
    // Algorithm:
    //  1. Start from the zone centroid (lat/lng). If unknown, use NaN → first unvisited
    //     order is trivially "closest" and becomes the starting point.
    //  2. Repeatedly find the unvisited RunSheetOrder with the smallest Haversine
    //     distance from the current position.
    //  3. Assign sequenceNum 1..N and update current position to that order's coords.
    //  4. If an order has no coords, it's placed at the end of the sequence (distance = MAX).

    private void nearestNeighborSort(List<RunSheetOrder> orders, double startLat, double startLng) {

        List<RunSheetOrder> remaining = new ArrayList<>(orders);
        int seq = 1;
        double curLat = startLat;
        double curLng = startLng;

        while (!remaining.isEmpty()) {

            RunSheetOrder nearest = null;
            double minDist = Double.MAX_VALUE;

            for (RunSheetOrder rso : remaining) {
                Double oLat = rso.getOrder().getDeliveryLat();
                Double oLng = rso.getOrder().getDeliveryLng();

                if (oLat == null || oLng == null) {
                    // No coords: treat as very far — lands at end of list
                    continue;
                }

                double dist = (Double.isNaN(curLat) || Double.isNaN(curLng))
                        ? 0.0 // first stop: treat all as equidistant → pick first
                        : haversine(curLat, curLng, oLat, oLng);

                if (dist < minDist) {
                    minDist = dist;
                    nearest = rso;
                }
            }

            // If no order with coords: pick the first remaining one
            if (nearest == null) {
                nearest = remaining.get(0);
            }

            nearest.setSequenceNum(seq++);
            curLat = nearest.getOrder().getDeliveryLat() != null
                    ? nearest.getOrder().getDeliveryLat() : curLat;
            curLng = nearest.getOrder().getDeliveryLng() != null
                    ? nearest.getOrder().getDeliveryLng() : curLng;

            remaining.remove(nearest);
        }
    }

    // ─── HAVERSINE FORMULA ─────────────────────────────────────────────────
    // Returns distance in kilometres between two lat/lng points.

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────

    private RunSheet findSheet(Long runSheetId) {
        return runSheetRepository.findById(runSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("RunSheet", runSheetId));
    }

    private void guardNotLocked(RunSheet sheet) {
        if (sheet.getStatus() == RunSheetStatus.LOCKED) {
            throw new ApiException(
                    "RunSheet " + sheet.getId() + " is LOCKED — cannot be modified.");
        }
    }

    /** COMPANY callers may only manage RunSheets belonging to their company's riders */
    private void guardCompanyScope(RunSheet sheet, Long callerUserId, String callerRole) {
        if ("COMPANY".equals(callerRole)) {
            User caller = userRepository.findById(callerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", callerUserId));

            if (caller.getCompany() == null
                    || !sheet.getRider().getCompany().getId().equals(caller.getCompany().getId())) {
                throw new ApiException("You do not have permission to modify this RunSheet.");
            }
        }
    }

    /** Escape double-quotes inside CSV strings */
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
