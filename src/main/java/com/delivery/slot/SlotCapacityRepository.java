package com.delivery.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotCapacityRepository
        extends JpaRepository<SlotCapacity, Long> {

    Optional<SlotCapacity> findByCompanyIdAndZoneAndSlotDateAndSlotLabel(
            Long companyId,
            String zone,
            LocalDate slotDate,
            String slotLabel
    );

    List<SlotCapacity> findByCompanyIdAndSlotDate(Long companyId, LocalDate slotDate);
}
