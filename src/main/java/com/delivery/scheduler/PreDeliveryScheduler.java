package com.delivery.scheduler;

import com.delivery.entity.DeliveryModel;
import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import com.delivery.repository.OrderRepository;
import com.delivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreDeliveryScheduler {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *") // every day at 9 AM
    @Transactional
    public void processPreDeliveryNotifications() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Order> orders = orderRepository
                .findByStatusAndSlotDate(OrderStatus.CREATED, tomorrow);

        if (orders.isEmpty()) {
            log.debug("PreDelivery — no orders for tomorrow");
            return;
        }

        log.info("PreDelivery — processing {} orders for {}", orders.size(), tomorrow);

        for (Order order : orders) {
            try {

                //  Only PARCEL orders
                if (order.getDeliveryModel() != DeliveryModel.PARCEL) {
                    continue;
                }

                //  Idempotency safety
                if (order.getStatus() != OrderStatus.CREATED) {
                    continue;
                }

                //  Correct status
                order.setStatus(OrderStatus.OUT_FOR_DELIVERY_TOMORROW);

                log.info("Order {} moved to OUT_FOR_DELIVERY_TOMORROW", order.getId());

                //  Send notification
                notificationService.sendOutForDelivery(order);

                log.info("PreDelivery — notified order {}", order.getId());

            } catch (Exception e) {
                log.warn("PreDelivery — failed for order {} — {}",
                        order.getId(), e.getMessage());
            }
        }
    }
}