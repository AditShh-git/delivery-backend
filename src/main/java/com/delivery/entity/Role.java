package com.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // ── Role name constants ────────────────────────────────────────────────
    // Use these instead of raw string literals everywhere in the codebase.
    // A typo like Role.COMPNAY fails at compile time, not silently at runtime.
    public static final String ADMIN = "ADMIN";
    public static final String COMPANY = "COMPANY";
    public static final String RIDER = "RIDER";
    public static final String CUSTOMER = "CUSTOMER";

    /** True if this role entity matches the given name constant. */
    public boolean is(String roleName) {
        return roleName.equals(this.name);
    }

}
