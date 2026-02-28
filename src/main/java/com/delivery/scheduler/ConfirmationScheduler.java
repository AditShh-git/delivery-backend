package com.delivery.scheduler;

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
    // 6:00 AM — Send Confirmation
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void sendMorningConfirmations() {

        LocalDate today = LocalDate.now();

        List<Order> orders =
                orderRepository.findByStatusAndSlotDateAndCustomerConfirmedFalse(
                        OrderStatus.CREATED,
                        today
                );

        for (Order order : orders) {

            order.setStatus(OrderStatus.CONFIRMATION_PENDING);
            order.setConfirmationSentAt(OffsetDateTime.now());
            order.setReminderSent(false);

            log.info("6AM confirmation sent for order {}", order.getId());

            // simulate WhatsApp send here later
        }
    }

    // ─────────────────────────────────────────────
    // Every 1 Hour — Handle Pending Confirmations
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void processPendingConfirmations() {

        List<Order> pending =
                orderRepository.findByStatusAndCustomerConfirmedFalse(
                        OrderStatus.CONFIRMATION_PENDING);

        for (Order order : pending) {

            if (order.getConfirmationSentAt() == null)
                continue;

            boolean twelveHoursPassed =
                    OffsetDateTime.now()
                            .isAfter(order.getConfirmationSentAt().plusHours(12));

            if (!twelveHoursPassed)
                continue;

            // ─────────────────────────────
            // FIRST REMINDER (same day)
            // ─────────────────────────────
            if (!order.getReminderSent()) {

                log.info("12-hour reminder sent for order {}", order.getId());

                order.setReminderSent(true);
                continue;
            }

            // ─────────────────────────────
            // DAY IGNORED → Auto-Reschedule
            // ─────────────────────────────
            int ignoredDays = order.getConfirmationAttempts() + 1;
            order.setConfirmationAttempts(ignoredDays);

            if (ignoredDays < 3) {

                log.warn("Rescheduling order {} to next day (attempt {})",
                        order.getId(), ignoredDays);

                order.setSlotDate(order.getSlotDate().plusDays(1));
                order.setStatus(OrderStatus.CREATED);
                order.setConfirmationSentAt(null);
                order.setReminderSent(false);

                continue;
            }

            // ─────────────────────────────
            // AUTO-CANCEL AFTER 3 IGNORED DAYS
            // ─────────────────────────────
            log.error("Auto-cancelling order {} after 3 ignored confirmations",
                    order.getId());

            order.setStatus(OrderStatus.CANCELLED);
            order.setAutoCancelled(true);
        }
    }
}