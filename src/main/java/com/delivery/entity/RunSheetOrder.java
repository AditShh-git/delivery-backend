package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "run_sheet_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rso_sheet_order", columnNames = { "run_sheet_id", "order_id" })
}, indexes = {
        @Index(name = "idx_rso_run_sheet_id", columnList = "run_sheet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunSheetOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "run_sheet_id")
    private RunSheet runSheet;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Order order;

    // Populated by /sort; defaults to 0 before sort is called.
    // Export and rider-today view order by sequenceNum ASC, id ASC.
    @Column(name = "sequence_num", nullable = false)
    private Integer sequenceNum = 0;
}
