package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "zones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_zone_city_name", columnNames = { "city", "name" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String name;

    // ── GPS centroid (set by admin, used for RunSheet nearest-neighbor sort) ──
    // Nullable — fallback to insertion order when missing.
    private Double lat;
    private Double lng;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
