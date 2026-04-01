package com.delivery.scheduler;

import com.delivery.entity.DeliveryModel;
import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class ConfirmationScheduler {

    private final OrderRepository orderRepository;

    // ─────────────────────────────────────────────
    // 6:00 AM — Send confirmation for today's deliveries
    // (orders prepared on previous day)
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void sendMorningConfirmations() {

        LocalDate today = LocalDate.now();

        List<Order> orders = orderRepository.findByStatusAndSlotDateAndCustomerConfirmedFalse(
                OrderStatus.OUT_FOR_DELIVERY_TOMORROW,
                today
        );

        if (orders.isEmpty()) {
            log.debug("ConfirmationScheduler — no orders for today {}", today);
            return;
        }

        log.info("ConfirmationScheduler — processing {} orders for {}", orders.size(), today);

        for (Order order : orders) {
            try {

                //  Only PARCEL orders
                if (order.getDeliveryModel() != DeliveryModel.PARCEL) {
                    continue;
                }

                //  Idempotency safety
                if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY_TOMORROW) {
                    continue;
                }

                //  Move to confirmation stage
                order.setStatus(OrderStatus.CONFIRMATION_PENDING);
                order.setConfirmationSentAt(OffsetDateTime.now());
                order.setReminderSent(false);

                log.info("Order {} moved to CONFIRMATION_PENDING", order.getId());

                // TODO: send WhatsApp confirmation message

            } catch (Exception e) {
                log.warn("ConfirmationScheduler — failed for order {} — {}",
                        order.getId(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────
    // Every 1 Hour — Handle Pending Confirmations
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void processPendingConfirmations() {

        List<Order> pending = orderRepository.findByStatusAndCustomerConfirmedFalse(
                OrderStatus.CONFIRMATION_PENDING);

        if (pending.isEmpty()) {
            log.debug("ConfirmationScheduler — no pending confirmations");
            return;
        }

        for (Order order : pending) {
            try {

                //  Only PARCEL orders
                if (order.getDeliveryModel() != DeliveryModel.PARCEL) {
                    continue;
                }

                //  Safety check
                if (order.getStatus() != OrderStatus.CONFIRMATION_PENDING) {
                    continue;
                }

                if (order.getConfirmationSentAt() == null) {
                    continue;
                }

                boolean twelveHoursPassed = OffsetDateTime.now()
                        .isAfter(order.getConfirmationSentAt().plusHours(12));

                if (!twelveHoursPassed) {
                    continue;
                }

                // ─────────────────────────────
                // FIRST REMINDER
                // ─────────────────────────────
                if (!order.getReminderSent()) {

                    log.info("Reminder sent for order {}", order.getId());

                    order.setReminderSent(true);
                    continue;
                }

                // ─────────────────────────────
                // RESCHEDULE (ONLY ONCE)
                // ─────────────────────────────
                int ignoredDays = order.getConfirmationAttempts() + 1;
                order.setConfirmationAttempts(ignoredDays);

                if (ignoredDays <= 1) {

                    //  7-day limit check
                    if (order.getSlotDate().isAfter(LocalDate.now().plusDays(7))) {
                        log.warn("Order {} exceeded max reschedule window, cancelling",
                                order.getId());
                        order.setStatus(OrderStatus.CANCELLED);
                        continue;
                    }

                    order.setSlotDate(order.getSlotDate().plusDays(1));
                    order.setStatus(OrderStatus.CREATED);
                    order.setConfirmationSentAt(null);
                    order.setReminderSent(false);

                    log.warn("Order {} rescheduled to next day (attempt {})",
                            order.getId(), ignoredDays);

                    continue;
                }

                // ─────────────────────────────
                // AUTO-CANCEL
                // ─────────────────────────────
                log.error("Order {} auto-cancelled after ignored confirmation",
                        order.getId());

                order.setStatus(OrderStatus.CANCELLED);
                order.setAutoCancelled(true);

            } catch (Exception e) {
                log.warn("ConfirmationScheduler — failed for order {} — {}",
                        order.getId(), e.getMessage());
            }
        }
    }
}