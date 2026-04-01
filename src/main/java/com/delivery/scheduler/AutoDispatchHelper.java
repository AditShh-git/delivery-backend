package com.delivery.scheduler;

import com.delivery.dto.request.AssignRiderRequest;
import com.delivery.entity.Order;
import com.delivery.entity.Rider;
import com.delivery.repository.RiderRepository;
import com.delivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDispatchHelper {

    private final OrderService orderService;
    private final RiderRepository riderRepository;

    @Transactional
    public void dispatchSingleOrder(Order order) {

        List<Rider> riders = riderRepository
                .findByZoneAndIsOnDutyTrue(order.getZone());

        riders.stream()
                .filter(Rider::canAcceptOrder)
                .min(Comparator.comparing(Rider::getActiveOrderCount))
                .ifPresentOrElse(
                        rider -> {
                            orderService.autoAssignRider(order, rider);
                            log.info("AutoDispatch — assigned rider {} to order {}",
                                    rider.getId(), order.getId());
                        },
                        () -> log.debug("AutoDispatch — no available rider for order {} in zone {}",
                                order.getId(), order.getZone())
                );
    }
}
