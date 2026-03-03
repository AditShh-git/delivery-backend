package com.delivery.scheduler;

import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static com.delivery.entity.OrderStatus.CANCELLED;
import static com.delivery.entity.OrderStatus.COLLECTED;
import static com.delivery.entity.OrderStatus.DELIVERED;

/**
 * Runs every 5 minutes and automatically marks orders as SLA-breached
 * when their deadline has passed and they are still in a non-terminal state.
 *
 * Before Week 5: SLA breach was only checked reactively (during rider status
 * updates).
 * After Week 5: SLA breach is proactive and guaranteed, regardless of rider
 * activity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaBreachScheduler {

    private static final List<OrderStatus> TERMINAL_STATUSES = List.of(
            DELIVERED,
            COLLECTED,
            CANCELLED);

    private final OrderRepository orderRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Every 5 minutes — auto-detect and mark SLA breaches
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public void detectAndMarkSlaBreaches() {

        OffsetDateTime now = OffsetDateTime.now();

        List<Order> candidates = orderRepository.findSlaBreachCandidates(now, TERMINAL_STATUSES);

        if (candidates.isEmpty()) {
            log.debug("SLA breach scan — no breaches detected at {}", now);
            return;
        }

        log.warn("SLA breach scan — marking {} order(s) as breached", candidates.size());

        for (Order order : candidates) {
            order.setSlaBreached(true);
            log.warn("SLA breached — orderId: {}, deadline: {}, status: {}",
                    order.getId(), order.getSlaDeadline(), order.getStatus());
        }

        // Orders are already managed by JPA session — dirty-checking will persist all
        // changes.
        // Explicit saveAll for clarity and to ensure flush.
        orderRepository.saveAll(candidates);

        log.info("SLA breach scan complete — {} order(s) updated", candidates.size());
    }
}
