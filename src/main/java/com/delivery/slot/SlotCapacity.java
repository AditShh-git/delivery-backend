package com.delivery.slot;

import com.delivery.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slot_capacities",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"company_id", "zone", "slot_date", "slot_label"}
        ))
public class SlotCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Multi-tenant isolation ─────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    // ── Slot metadata ──────────────────────────────────

    @Column(nullable = false)
    private String zone; // e.g. "Bangalore-East"

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_label", nullable = false)
    private String slotLabel; // 9AM-12PM

    // ── Capacity control ───────────────────────────────

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    // ── Audit ──────────────────────────────────────────

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
