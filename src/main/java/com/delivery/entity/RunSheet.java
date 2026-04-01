package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "run_sheets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_run_sheet_rider_date", columnNames = { "rider_id", "slot_date" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Who is running this sheet today ────────────────────────────────────

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private Rider rider;

    @Column(nullable = false, length = 50)
    private String zone;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunSheetStatus status = RunSheetStatus.DRAFT;

    // ── Audit ─────────────────────────────────────────────────────────────

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Orders (sorted by sequenceNum after /sort is called) ──────────────

    @OneToMany(mappedBy = "runSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNum ASC, id ASC")
    private List<RunSheetOrder> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
