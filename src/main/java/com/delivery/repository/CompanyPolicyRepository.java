package com.delivery.repository;

import com.delivery.entity.CompanyPolicy;
import com.delivery.entity.DeliveryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyPolicyRepository extends JpaRepository<CompanyPolicy, Long> {

    // DELIVERY orders — three-field lookup (existing, keep as is)
    Optional<CompanyPolicy> findByCompanyIdAndProductCategoryAndDeliveryType(
            Long companyId,
            String productCategory,
            DeliveryType deliveryType
    );

    // PICKUP orders — two-field lookup (deliveryType is null for PICKUP)
    Optional<CompanyPolicy> findByCompanyIdAndProductCategory(
            Long companyId,
            String productCategory
    );

    List<CompanyPolicy> findByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
