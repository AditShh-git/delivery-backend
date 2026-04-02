package com.delivery.repository;

import com.delivery.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByCityAndNameAndIsActiveTrue(String city, String name);

    boolean existsByCityAndName(String city, String name);

    Optional<Zone> findByNameAndIsActiveTrue(String name);

    // Used by AutoDispatchHelper fallback: fetch all zone names in the same city
    List<Zone> findByCityAndIsActiveTrue(String city);
}
