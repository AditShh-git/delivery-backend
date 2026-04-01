package com.delivery.scheduler;

import com.delivery.dto.request.AssignRiderRequest;
import com.delivery.entity.DeliveryModel;
import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.entity.Rider;
import com.delivery.repository.OrderRepository;
import com.delivery.repository.RiderRepository;
import com.delivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Runs every 5 seconds and auto-assigns INSTANT+CREATED orders to the best
 * available rider in the same zone.
 *
 * Design decisions:
 * - Each order is dispatched in its own transaction (@Transactional on
 *   dispatchSingleOrder) so a failure on one order never rolls back the rest.
 * - Each assignment is wrapped in try-catch so an ApiException
 *   (zone mismatch, capacity full, etc.) logs a warning and continues.
 * - adminId is passed as null — OrderServiceImpl uses it only for logging;
 *   no NPE risk.
 * - @EnableScheduling is on ConfirmationScheduler — one annotation is enough
 *   per Spring context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoDispatchScheduler {

    private final OrderRepository orderRepository;
    private final AutoDispatchHelper autoDispatchHelper;

    @Scheduled(fixedRate = 5_000)
    public void dispatch() {

        List<Order> candidates = orderRepository
                .findByStatus(OrderStatus.CONFIRMED);

        if (candidates.isEmpty()) {
            log.debug("AutoDispatch — no CONFIRMED orders found");
            return;
        }

        log.info("AutoDispatch — processing {} candidate order(s)", candidates.size());

        for (Order order : candidates) {
            try {
                switch (order.getDeliveryModel()) {

                    case INSTANT ->
                        // Assign immediately — no slot check needed
                            autoDispatchHelper.dispatchSingleOrder(order);

                    case PARCEL ->
                        // Customer confirmed + chose slot via WhatsApp
                        // Safe to dispatch — slot is already locked
                            autoDispatchHelper.dispatchSingleOrder(order);


                    case PICKUP_RETURN ->
                        // No rider dispatch — customer comes to store
                            log.debug("AutoDispatch — skipping PICKUP_RETURN order {}",
                                    order.getId());
                }

            } catch (Exception e) {
                log.warn("AutoDispatch — skipped order {} — {}",
                        order.getId(), e.getMessage());
            }
        }
    }

    private boolean isNearSlot(Order order) {
        if (order.getSlotDate() == null || order.getSlotLabel() == null) {
            log.warn("AutoDispatch — SCHEDULED order {} missing slot fields, skipping",
                    order.getId());
            return false;
        }

        LocalTime slotStart = switch (order.getSlotLabel()) {
            case "SLOT_9_12"  -> LocalTime.of(9,  0);
            case "SLOT_12_3"  -> LocalTime.of(12, 0);
            case "SLOT_3_6"   -> LocalTime.of(15, 0);
            default -> {
                log.warn("AutoDispatch — unknown slotLabel '{}' on order {}, skipping",
                        order.getSlotLabel(), order.getId());
                yield LocalTime.MAX; // never near — safely skips
            }
        };

        LocalDateTime slotDateTime = order.getSlotDate().atTime(slotStart);
        LocalDateTime now = LocalDateTime.now();

        // Dispatch window: 30 min before slot starts
        return now.isAfter(slotDateTime.minusMinutes(30))
                && now.isBefore(slotDateTime.plusHours(3)); // upper bound — don't dispatch stale orders
    }
}