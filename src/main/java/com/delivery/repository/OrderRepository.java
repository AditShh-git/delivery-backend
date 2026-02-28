package com.delivery.repository;

import com.delivery.entity.Order;
import com.delivery.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // CUSTOMER — their own orders
    List<Order> findByCustomerId(Long customerId);

    // COMPANY — all orders for their company
    List<Order> findByCompanyId(Long companyId);

    // RIDER — ALL orders ever assigned (past + present)
    // previous version filtered by status — now removed per your answer
    List<Order> findByRiderId(Long riderId);

    Page<Order> findBySlaBreachedTrue(Pageable pageable);

    List<Order> findByStatusAndSlotDateAndCustomerConfirmedFalse(
            OrderStatus status,
            LocalDate slotDate);

    List<Order> findByStatusAndCustomerConfirmedFalse(
            OrderStatus status);
}
