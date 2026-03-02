package com.delivery.service.impl;

import com.delivery.dto.request.*;
import com.delivery.dto.response.AttemptHistoryResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.entity.*;
import com.delivery.exception.*;
import com.delivery.repository.*;
import com.delivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository   orderRepository;
    private final UserRepository    userRepository;
    private final RiderRepository   riderRepository;
    private final CompanyRepository companyRepository;
    private final CompanyPolicyRepository companyPolicyRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final ZoneRepository     zoneRepository;

    // ─── CREATE ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public OrderResponse createOrder(Long customerId, CreateOrderRequest request) {

        log.info("Creating order — customerId: {}, companyId: {}",
                customerId, request.companyId());

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", customerId));

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));

        // ── Zone (Pincode) Validation ─────────────────────────────────────────
        Zone zone = zoneRepository
                .findByNameAndIsActiveTrue(request.pincode())
                .orElseThrow(() ->
                        new ApiException("Service not available for pincode: " + request.pincode()));

        // ── Slot Rules ─────────────────────────────────────────────────────────
        DeliveryModel model = company.getDeliveryModel();

        switch (model) {
            case INSTANT -> {
                if (request.slotLabel() != null || request.slotDate() != null) {
                    throw new ApiException(
                            "INSTANT orders do not use slot booking.");
                }
            }

            case PARCEL -> {
                if (request.slotLabel() != null) {
                    throw new ApiException(
                            "PARCEL orders do not use time-window slots.");
                }
            }

            case SCHEDULED -> {
                if (request.slotDate() != null && request.slotLabel() == null) {
                    throw new ApiException(
                            "SCHEDULED orders require slotLabel when slotDate is provided.");
                }
            }

            case PICKUP_RETURN -> {
                if (request.slotDate() != null && request.slotLabel() == null) {
                    throw new ApiException(
                            "PICKUP_RETURN requires slotLabel when slotDate is provided.");
                }
            }
        }

        // ── Build Order ───────────────────────────────────────────────────────
        Order order = new Order();
        order.setCustomer(customer);
        order.setCompany(company);
        order.setDeliveryAddress(request.deliveryAddress());

        // Zone system
        order.setZone(zone.getName());           // pincode
        order.setLandmark(request.landmark());   // optional

        order.setItems(request.items());
        order.setOrderType(request.orderType());
        order.setDeliveryType(request.deliveryType());
        order.setSlotLabel(request.slotLabel());
        order.setSlotDate(request.slotDate());
        order.setExternalOrderId(request.externalOrderId());
        order.setProductCategory(request.productCategory());
        order.setCallBeforeArrival(
                request.callBeforeArrival() != null && request.callBeforeArrival());

        order.setStatus(OrderStatus.CREATED);
        order.setAttemptCount(0);

        // ── SLA ───────────────────────────────────────────────────────────────
        OffsetDateTime deadline = switch (model) {
            case INSTANT       -> OffsetDateTime.now().plusMinutes(30);
            case PARCEL        -> OffsetDateTime.now().plusHours(48);
            case SCHEDULED     -> OffsetDateTime.now().plusHours(24);
            case PICKUP_RETURN -> OffsetDateTime.now().plusHours(48);
        };

        order.setSlaDeadline(deadline);
        order.setSlaBreached(false);

        Order saved = orderRepository.save(order);

        log.info("Order created — id: {}, deliveryModel: {}, zone: {}",
                saved.getId(), model, saved.getZone());

        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public OrderResponse assignRider(Long orderId,
                                     AssignRiderRequest request,
                                     Long adminId) {

        log.info("Assigning rider — orderId: {}, riderId: {}, adminId: {}",
                orderId, request.riderId(), adminId);

        Order order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.FAILED
                && order.getStatus() != OrderStatus.CREATED) {

            throw new InvalidStatusTransitionException(
                    order.getStatus(), OrderStatus.ASSIGNED);
        }

        Rider rider = riderRepository.findById(request.riderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rider", request.riderId()));

        // Capacity check
        if (!rider.canAcceptOrder()) {
            throw new ApiException("Rider is not available: " + request.riderId());
        }

        // Multi-tenant safety
        if (rider.getCompany() == null
                || !rider.getCompany().getId()
                .equals(order.getCompany().getId())) {

            throw new ApiException(
                    "Rider " + request.riderId()
                            + " does not belong to company "
                            + order.getCompany().getId());
        }

        // ── Zone Enforcement (Hard Boundary) ─────────────────────────────
        if (!order.getZone().equals(rider.getZone())) {
            throw new ApiException(
                    "Rider " + rider.getId()
                            + " belongs to zone " + rider.getZone()
                            + " and cannot be assigned to order in zone "
                            + order.getZone());
        }

        // Reset attempt count if reassigning
        if (order.getStatus() == OrderStatus.FAILED) {
            log.info("Resetting attempt count for order {}", orderId);
            order.setAttemptCount(0);
        }

        // Release old rider
        if (order.getRider() != null) {
            order.getRider().decrementActiveOrders();
        }

        // Assign new rider
        order.setRider(rider);
        order.setStatus(OrderStatus.ASSIGNED);

        rider.incrementActiveOrders();

        log.info("Rider {} assigned to order {}", request.riderId(), orderId);

        return OrderResponse.from(order);
    }

    // ─── UPDATE STATUS ─────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public OrderResponse updateStatus(Long orderId,
                                      UpdateStatusRequest request,
                                      Long userId,
                                      String role) {

        log.info("Status update — orderId: {}, newStatus: {}, userId: {}, role: {}",
                orderId, request.status(), userId, role);

        Order order = findOrderById(orderId);
        OrderStatus newStatus = request.status();

        // ── SLA CHECK ───────────────────────────────────────────────
        if (Boolean.FALSE.equals(order.getSlaBreached())
                && order.getSlaDeadline() != null
                && OffsetDateTime.now().isAfter(order.getSlaDeadline())
                && order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.COLLECTED
                && order.getStatus() != OrderStatus.CANCELLED) {

            order.setSlaBreached(true);
            log.warn("Order {} breached SLA!", orderId);
        }

        // ── POLICY LOOKUP ───────────────────────────────────────────
        CompanyPolicy policy;

        if (order.getOrderType() == OrderType.PICKUP) {
            policy = companyPolicyRepository
                    .findByCompanyIdAndProductCategory(
                            order.getCompany().getId(),
                            order.getProductCategory()
                    )
                    .orElseThrow(() -> new ApiException(
                            "Policy not configured for company "
                                    + order.getCompany().getId()
                                    + " | product: " + order.getProductCategory()
                    ));
        } else {
            policy = companyPolicyRepository
                    .findByCompanyIdAndProductCategoryAndDeliveryType(
                            order.getCompany().getId(),
                            order.getProductCategory(),
                            order.getDeliveryType()
                    )
                    .orElseThrow(() -> new ApiException(
                            "Policy not configured for company "
                                    + order.getCompany().getId()
                                    + " | product: " + order.getProductCategory()
                                    + " | deliveryType: " + order.getDeliveryType()
                    ));
        }

        int maxAttempts = policy.getMaxReschedules();

        // ── RIDER RULES ─────────────────────────────────────────────
        if ("RIDER".equals(role)) {

            validateRiderOwnership(order, userId);

            if (order.getAttemptCount() >= maxAttempts
                    && order.getStatus() == OrderStatus.FAILED) {

                throw new ApiException(
                        "Maximum delivery attempts reached. Admin must reassign this order.");
            }

            if (newStatus != OrderStatus.IN_TRANSIT
                    && newStatus != OrderStatus.DELIVERED
                    && newStatus != OrderStatus.COLLECTED
                    && newStatus != OrderStatus.FAILED
                    && newStatus != OrderStatus.DISPUTED) {

                throw new ApiException("Rider cannot set status: " + newStatus);
            }
        }

        // ── STATE MACHINE VALIDATION ────────────────────────────────
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(order.getStatus(), newStatus);
        }

        validateOrderTypeTransition(order, newStatus);

        // ── HANDLE FAILED ───────────────────────────────────────────
        if (newStatus == OrderStatus.FAILED) {

            if (order.getStatus() == OrderStatus.FAILED) {
                throw new ApiException("Order already failed. Awaiting admin reassignment.");
            }

            if (request.failureReason() == null || request.failureReason().isBlank()) {
                throw new ApiException("Failure reason is required when marking order as FAILED.");
            }

            int attempts = order.getAttemptCount() + 1;
            order.setAttemptCount(attempts);

            int missedSlots = order.getMissedSlotCount() + 1;
            order.setMissedSlotCount(missedSlots);

            log.warn("Order {} missed slot {}/{}", orderId, missedSlots, maxAttempts);

            AttemptHistory history = new AttemptHistory();
            history.setOrder(order);
            history.setRider(order.getRider());
            history.setAttemptNumber(attempts);
            history.setFailureReason(request.failureReason());
            history.setRecordedBy(role);

            attemptHistoryRepository.save(history);

            if (policy.getMissedSlotAction() == MissedSlotAction.CHARGE_FEE
                    && !Boolean.TRUE.equals(order.getPenaltyApplied())) {

                log.error("Applying penalty {} for order {}",
                        policy.getPenaltyAmount(), orderId);

                order.setPenaltyApplied(true);
            }

            order.setStatus(OrderStatus.FAILED);


            if (missedSlots >= maxAttempts) {

                log.error("Max missed slots reached. Auto-unassigning rider.");

                Rider rider = order.getRider();

                if (rider != null) {
                    rider.decrementActiveOrders();
                }

                order.setRider(null);
            }

            return OrderResponse.from(order);
        }

        // ── NORMAL STATUS UPDATE ────────────────────────────────────
        order.setStatus(newStatus);


        if (newStatus == OrderStatus.DELIVERED
                || newStatus == OrderStatus.COLLECTED
                || newStatus == OrderStatus.DISPUTED
                || newStatus == OrderStatus.CANCELLED) {

            Rider rider = order.getRider();

            if (rider != null) {
                rider.decrementActiveOrders();
                log.info("Rider {} freed — order {} reached terminal status {}",
                        rider.getId(), orderId, newStatus);
            }
        }

        return OrderResponse.from(order);
    }

    @Override
    public Page<AttemptHistoryResponse> getAttemptHistory(
            Long orderId,
            Long riderId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        Specification<AttemptHistory> spec = (root, query, cb) -> cb.conjunction();

        if (orderId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("order").get("id"), orderId));
        }

        if (riderId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("rider").get("id"), riderId));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"),
                            startDate.atStartOfDay().atOffset(ZoneOffset.UTC)));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"),
                            endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)));
        }

        return attemptHistoryRepository
                .findAll(spec, pageable)
                .map(AttemptHistoryResponse::from);
    }

    @Override
    public Page<OrderResponse> getSlaBreachedOrders(Pageable pageable) {

        return orderRepository
                .findBySlaBreachedTrue(pageable)
                .map(OrderResponse::from);
    }

    @Override
    @Transactional
    public OrderResponse forceCancel(Long orderId,
                                     String reason,
                                     Long adminId) {

        Order order = findOrderById(orderId);

        if (reason == null || reason.isBlank()) {
            throw new ApiException("Cancellation reason required.");
        }

        log.error("Admin {} force-cancelled order {} — reason: {}",
                adminId, orderId, reason);

        // Release rider if assigned
        if (order.getRider() != null) {
            Rider rider = order.getRider();
            rider.decrementActiveOrders();
            order.setRider(null);
        }

        order.setStatus(OrderStatus.CANCELLED);

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse adminReassign(Long orderId,
                                       Long riderId,
                                       String reason,
                                       Long adminId) {

        Order order = findOrderById(orderId);

        Rider newRider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", riderId));

        // ── Company Safety Check ───────────────────────────────
        if (!newRider.getCompany().getId()
                .equals(order.getCompany().getId())) {
            throw new ApiException("Rider belongs to different company.");
        }

        Rider currentRider = order.getRider();

        // ── Case 1: Reassigning to SAME rider (retry case) ─────
        if (currentRider != null
                && currentRider.getId().equals(newRider.getId())) {

            // Do NOT change capacity
            order.setStatus(OrderStatus.ASSIGNED);

            log.warn("Admin {} reassigned order {} to SAME rider {} — reason: {}",
                    adminId, orderId, riderId, reason);

            return OrderResponse.from(order);
        }

        // ── Case 2: Assigning to DIFFERENT rider ───────────────

        // Check capacity properly
        if (!newRider.canAcceptOrder()) {
            throw new ApiException("Rider not available.");
        }

        // Release old rider capacity correctly
        if (currentRider != null) {
            currentRider.decrementActiveOrders();
        }

        // Assign new rider
        newRider.incrementActiveOrders();
        order.setRider(newRider);
        order.setStatus(OrderStatus.ASSIGNED);

        log.warn("Admin {} reassigned order {} to rider {} — reason: {}",
                adminId, orderId, riderId, reason);

        return OrderResponse.from(order);
    }

    // ─── ORDER-TYPE TRANSITION GUARD ───────────────────────────────────────
    //
    // The state machine allows IN_TRANSIT → DELIVERED | COLLECTED | FAILED | DISPUTED
    // but not every order type can reach every terminal state:
    //
    //   PICKUP    → must end in COLLECTED, never DELIVERED
    //   DELIVERY  → must end in DELIVERED, never COLLECTED
    //   OPEN_BOX  → can be DISPUTED (customer rejects item on inspection)
    //   STANDARD  → cannot be DISPUTED (no inspection step)

    private void validateOrderTypeTransition(Order order, OrderStatus newStatus) {

        OrderType     type         = order.getOrderType();
        DeliveryType  deliveryType = order.getDeliveryType();

        // PICKUP orders end in COLLECTED — DELIVERED is not valid
        if (newStatus == OrderStatus.DELIVERED && type == OrderType.PICKUP) {
            throw new ApiException(
                    "PICKUP orders must be marked COLLECTED, not DELIVERED.");
        }

        // DELIVERY orders end in DELIVERED — COLLECTED is not valid
        if (newStatus == OrderStatus.COLLECTED && type == OrderType.DELIVERY) {
            throw new ApiException(
                    "DELIVERY orders must be marked DELIVERED, not COLLECTED.");
        }

        // Only OPEN_BOX deliveries can be DISPUTED
        if (newStatus == OrderStatus.DISPUTED) {
            if (type != OrderType.DELIVERY || deliveryType != DeliveryType.OPEN_BOX) {
                throw new ApiException(
                        "DISPUTED status is only valid for OPEN_BOX delivery orders.");
            }
        }
    }

    // ─── GET ORDERS ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(
            Long userId,
            String role,
            OrderStatus status,
            Long companyId,
            Long riderId,
            String zone,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        Specification<Order> spec = (root, query, cb) -> cb.conjunction();

        // FIX: role arrives WITHOUT "ROLE_" prefix — cases must match that.
        // Previous switch had "RIDER", "CUSTOMER", etc. — those were already correct.
        // Documenting explicitly so it stays consistent if someone adds a new case.
        switch (role) {

            case "ADMIN" -> { /* no restriction */ }

            case "CUSTOMER" ->
                    spec = spec.and((root, query, cb) ->
                            cb.equal(root.get("customer").get("id"), userId));

            case "RIDER" -> {
                Rider rider = riderRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Rider", userId));
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("rider").get("id"), rider.getId()));
            }

            case "COMPANY" -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("company").get("id"), user.getCompany().getId()));
            }

            default -> throw new ApiException("Unknown role: " + role);
        }

        if (status != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));

        if (companyId != null)
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("company").get("id"), companyId));

        if (riderId != null)
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("rider").get("id"), riderId));

        if (zone != null && !zone.isBlank())
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("zone"), zone));

        if (startDate != null)
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay().atOffset(ZoneOffset.UTC)));

        if (endDate != null)
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)));


        return orderRepository.findAll(spec, pageable).map(OrderResponse::from);
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────

    @Override
    public OrderResponse getOrderById(Long orderId, Long userId, String role) {
        log.debug("getOrderById — orderId: {}, userId: {}, role: {}", orderId, userId, role);
        Order order = findOrderById(orderId);
        validateAccess(order, userId, role);
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId,
                                     Long userId,
                                     String role) {

        Order order = findOrderById(orderId);

        // ADMIN can cancel anytime
        // COMPANY can cancel only their own orders
        if ("COMPANY".equals(role)) {
            if (!order.getCompany().getId()
                    .equals(getCompanyFromUser(userId).getId())) {

                throw new ApiException("You cannot cancel another company's order.");
            }
        }

        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COLLECTED) {

            throw new ApiException("Delivered/Collected orders cannot be cancelled.");
        }

        //  Auto free rider if assigned
        if (order.getRider() != null) {
            Rider rider = order.getRider();
            rider.decrementActiveOrders();
            order.setRider(null);
        }

        order.setStatus(OrderStatus.CANCELLED);

        return OrderResponse.from(order);
    }

    // ─── PRIVATE HELPERS ───────────────────────────────────────────────────

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private void validateRiderOwnership(Order order, Long userId) {
        if (order.getRider() == null) {
            throw new ApiException("No rider assigned to order: " + order.getId());
        }
        if (!order.getRider().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not assigned to order: " + order.getId());
        }
    }

    private Company getCompanyFromUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getCompany() == null) {
            throw new ApiException("User is not associated with any company.");
        }

        return user.getCompany();
    }

    private void validateAccess(Order order, Long userId, String role) {
        // FIX: role arrives WITHOUT "ROLE_" prefix from extractRole().
        // Previous version used "ROLE_ADMIN", "ROLE_CUSTOMER" etc — those never matched.
        boolean allowed = switch (role) {

            case "ADMIN" ->
                    true;

            case "CUSTOMER" ->
                    order.getCustomer().getId().equals(userId);

            case "COMPANY" -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                yield user.getCompany() != null
                        && user.getCompany().getId().equals(order.getCompany().getId());
            }

            case "RIDER" ->
                    order.getRider() != null
                            && order.getRider().getUser().getId().equals(userId);

            default -> false;
        };

        if (!allowed) {
            log.warn("Access denied — orderId: {}, userId: {}, role: {}",
                    order.getId(), userId, role);
            throw new AccessDeniedException("You do not have access to order: " + order.getId());
        }
    }
}