package com.delivery.scheduler;

import com.delivery.entity.Order;
import com.delivery.entity.Rider;
import com.delivery.entity.Zone;
import com.delivery.repository.RiderRepository;
import com.delivery.repository.ZoneRepository;
import com.delivery.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDispatchHelper {

    private final OrderService    orderService;
    private final RiderRepository riderRepository;
    private final ZoneRepository  zoneRepository;
    private final MeterRegistry   meterRegistry;

    // Fix 2: LIMIT 1 — only the single best eligible rider is locked per attempt.
    // Prevents locking the entire zone's rider table under concurrent dispatch.
    private static final PageRequest SINGLE = PageRequest.of(0, 1);

    /**
     * Dispatches a single order to the best available rider.
     *
     * <p><b>Fix 2 — Race condition closed:</b><br>
     * {@code findBestEligibleRiderInZoneWithLock} issues a {@code SELECT FOR UPDATE}
     * with {@code WHERE activeOrderCount < maxConcurrentOrders AND isOnDuty = true},
     * so only genuinely eligible riders are locked. A second concurrent thread
     * waiting on the lock will re-read the updated count and skip if now full.
     *
     * <p><b>Fix 3 — Distance-aware fallback:</b><br>
     * When no eligible rider exists in the order's zone, fallback zones in the same
     * city are sorted by Haversine distance (order GPS preferred, zone centroid fallback)
     * and tried nearest-first — each with its own locked, LIMIT-1 query.
     *
     * <p><b>Fix 5 — Metrics:</b><br>
     * {@code dispatch.attempts / .success / .failure} counters tagged by
     * {@code zone}, {@code fallback}, {@code companyId}.
     * {@code rider.utilization} gauge registered per rider on assignment.
     */
    @Transactional
    public void dispatchSingleOrder(Order order) {

        final String zone       = order.getZone();
        final String companyTag = order.getCompany() != null
                ? String.valueOf(order.getCompany().getId()) : "unknown";

        // Fix 5: total attempt counter
        meterRegistry.counter("dispatch.attempts",
                "zone", zone, "companyId", companyTag).increment();

        // ── Step 1: Exact zone — locked, LIMIT 1 ─────────────────────────────
        List<Rider> primary = riderRepository
                .findBestEligibleRiderInZoneWithLock(zone, SINGLE);

        if (!primary.isEmpty() && primary.get(0).canAcceptOrder()) {
            assign(order, primary.get(0), false, companyTag);
            return;
        }

        log.debug("AutoDispatch — no eligible rider in '{}' for order {} — distance fallback",
                zone, order.getId());

        // ── Step 2: Distance-sorted city fallback ─────────────────────────────
        Optional<Zone> primaryZoneOpt = zoneRepository.findByNameAndIsActiveTrue(zone);
        if (primaryZoneOpt.isEmpty()) {
            log.warn("AutoDispatch — zone '{}' not found; cannot fallback for order {}", zone, order.getId());
            fail("zone_not_found", zone, companyTag);
            return;
        }

        Zone   pz   = primaryZoneOpt.get();
        String city = pz.getCity();

        // Fix 3: origin = order GPS (preferred) → zone centroid → (0,0)
        double originLat = firstNonNull(order.getDeliveryLat(), pz.getLat(), 0.0);
        double originLng = firstNonNull(order.getDeliveryLng(), pz.getLng(), 0.0);

        // Fix 3: sort other active zones in city by Haversine distance ascending
        List<Zone> fallbackZones = zoneRepository.findByCityAndIsActiveTrue(city).stream()
                .filter(z -> !z.getName().equals(zone))
                .sorted(Comparator.comparingDouble(z -> haversineKm(
                        originLat, originLng,
                        z.getLat() != null ? z.getLat() : originLat,
                        z.getLng() != null ? z.getLng() : originLng)))
                .toList();

        if (fallbackZones.isEmpty()) {
            log.debug("AutoDispatch — no fallback zones in city '{}'; order {} retry next cycle",
                    city, order.getId());
            fail("no_fallback_zones", zone, companyTag);
            return;
        }

        // Iterate nearest-first; each call: locked, LIMIT 1 — minimal contention
        for (Zone fz : fallbackZones) {
            List<Rider> candidates = riderRepository
                    .findBestEligibleRiderInZoneWithLock(fz.getName(), SINGLE);

            if (!candidates.isEmpty() && candidates.get(0).canAcceptOrder()) {
                assign(order, candidates.get(0), true, companyTag);
                return;
            }
        }

        log.debug("AutoDispatch — no rider city-wide (city='{}') for order {}; retry next cycle",
                city, order.getId());
        fail("no_rider_city_wide", zone, companyTag);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void assign(Order order, Rider rider, boolean isFallback, String companyTag) {
        orderService.autoAssignRider(order, rider);

        // Fix 5: success counter (tagged with fallback flag for ratio tracking)
        meterRegistry.counter("dispatch.success",
                "zone",      order.getZone(),
                "fallback",  String.valueOf(isFallback),
                "companyId", companyTag).increment();

        // Fix 5: per-rider utilization gauge — live ratio, Micrometer deduplicates
        // by id+tags so repeated calls here are safe and idempotent.
        meterRegistry.gauge("rider.utilization",
                Tags.of("riderId",   String.valueOf(rider.getId()),
                        "zone",      rider.getZone(),
                        "companyId", companyTag),
                rider,
                r -> r.getMaxConcurrentOrders() > 0
                        ? (double) r.getActiveOrderCount() / r.getMaxConcurrentOrders()
                        : 0.0);

        if (isFallback) {
            log.info("AutoDispatch [FALLBACK] — rider {} (zone='{}') assigned to order {} (zone='{}') [cross-zone]",
                    rider.getId(), rider.getZone(), order.getId(), order.getZone());
        } else {
            log.info("AutoDispatch — rider {} assigned to order {}", rider.getId(), order.getId());
        }
    }

    /** Fix 5: failure counter with reason tag — one call per bail-out point. */
    private void fail(String reason, String zone, String companyTag) {
        meterRegistry.counter("dispatch.failure",
                "zone",      zone,
                "reason",    reason,
                "companyId", companyTag).increment();
    }

    /**
     * Fix 3: Haversine great-circle distance in kilometres.
     * d = 2R * arcsin( sqrt( sin²(dLat/2) + cos(lat1)*cos(lat2)*sin²(dLon/2) ) )
     * Accurate to ~0.5% for intra-city distances.
     */
    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R    = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Returns the first non-null Double, defaulting to {@code def}. */
    private static double firstNonNull(Double a, Double b, double def) {
        if (a != null) return a;
        if (b != null) return b;
        return def;
    }
}
