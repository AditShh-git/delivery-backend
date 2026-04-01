package com.delivery.repository;

import com.delivery.entity.RunSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunSheetRepository extends JpaRepository<RunSheet, Long> {

    Optional<RunSheet> findByRiderIdAndSlotDate(Long riderId, LocalDate slotDate);

    List<RunSheet> findByRiderId(Long riderId);
}
