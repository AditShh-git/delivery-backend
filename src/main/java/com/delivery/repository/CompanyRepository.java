package com.delivery.repository;

import com.delivery.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findById(Long id);
    boolean existsByName(String name);
    boolean existsByEmail(String email);
}
