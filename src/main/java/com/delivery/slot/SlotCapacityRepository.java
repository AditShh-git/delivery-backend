package com.delivery.slot;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
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

    boolean existsByCompanyIdAndZoneAndSlotDateAndSlotLabel(
            Long companyId,
            String zone,
            LocalDate slotDate,
            String slotLabel
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT s FROM SlotCapacity s WHERE s.company.id = :companyId " +
            "AND s.zone = :zone AND s.slotDate = :slotDate AND s.slotLabel = :slotLabel")
    Optional<SlotCapacity> findByCompanyIdAndZoneAndSlotDateAndSlotLabelWithLock(
            @Param("companyId") Long companyId,
            @Param("zone") String zone,
            @Param("slotDate") LocalDate slotDate,
            @Param("slotLabel") String slotLabel);
}
