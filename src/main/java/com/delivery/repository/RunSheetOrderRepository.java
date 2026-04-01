package com.delivery.repository;

import com.delivery.entity.RunSheetOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RunSheetOrderRepository extends JpaRepository<RunSheetOrder, Long> {

    List<RunSheetOrder> findByRunSheetIdOrderBySequenceNumAscIdAsc(Long runSheetId);
}
