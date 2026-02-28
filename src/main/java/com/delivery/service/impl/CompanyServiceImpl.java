package com.delivery.service.impl;

import com.delivery.dto.request.CreateCompanyRequest;
import com.delivery.dto.response.CompanyResponse;
import com.delivery.dto.response.PolicyResponse;
import com.delivery.entity.Company;
import com.delivery.entity.CompanyPolicy;
import com.delivery.entity.DeliveryType;
import com.delivery.entity.MissedSlotAction;
import com.delivery.exception.ApiException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.CompanyPolicyRepository;
import com.delivery.repository.CompanyRepository;
import com.delivery.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyPolicyRepository policyRepository;

    @Override
    public CompanyResponse createCompany(CreateCompanyRequest request) {

        log.info("Creating company: {}", request.name());

        // ─────────────────────────────
        // 1. Normalize inputs first
        // ─────────────────────────────
        String normalizedName = request.name().trim();
        String normalizedContact = request.contact().trim();
        String normalizedEmail = request.email().trim().toLowerCase();

        // ─────────────────────────────
        // 2. Uniqueness validation
        // ─────────────────────────────
        if (companyRepository.existsByName(normalizedName)) {
            throw new ApiException("Company already exists with that name");
        }

        if (companyRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException("Email already registered");
        }

        // ─────────────────────────────
        // 3. Create Company
        // ─────────────────────────────
        Company company = new Company();
        company.setName(normalizedName);
        company.setContact(normalizedContact);
        company.setEmail(normalizedEmail);
        // deliveryModel defaults to SCHEDULED

        companyRepository.save(company);
        log.info("Company created with id={}", company.getId());

        // ─────────────────────────────
        // 4. Normalize product category
        // ─────────────────────────────
        String productCategory = request.productCategory() != null
                ? request.productCategory().trim().toUpperCase()
                : "DEFAULT";

        // ─────────────────────────────
        // 5. Parse MissedSlotAction safely
        // ─────────────────────────────
        MissedSlotAction missedSlotAction;
        try {
            missedSlotAction = request.missedSlotAction() != null
                    ? MissedSlotAction.valueOf(request.missedSlotAction().trim().toUpperCase())
                    : MissedSlotAction.RESCHEDULE;
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid missedSlotAction value");
        }

        // ─────────────────────────────
        // 6. Enforce penalty invariant
        // ─────────────────────────────
        if (missedSlotAction == MissedSlotAction.CHARGE_FEE
                && request.penaltyAmount() == null) {
            throw new ApiException("Penalty amount required when action is CHARGE_FEE");
        }

        // ─────────────────────────────
        // 7. Create policies for ALL delivery types
        // ─────────────────────────────
        List<CompanyPolicy> policies = new ArrayList<>();

        for (DeliveryType type : DeliveryType.values()) {

            CompanyPolicy policy = buildPolicy(
                    company,
                    productCategory,
                    type,
                    missedSlotAction,
                    request
            );

            policies.add(policy);

            log.info("Policy prepared for companyId={}, category={}, deliveryType={}",
                    company.getId(), productCategory, type);
        }

        policyRepository.saveAll(policies);

        // ─────────────────────────────
        // 8. Build response
        // ─────────────────────────────
        List<PolicyResponse> policyResponses = policies.stream()
                .map(p -> new PolicyResponse(
                        p.getProductCategory(),
                        p.getDeliveryType(),
                        p.getMissedSlotAction().name(),
                        p.getMaxReschedules(),
                        p.getPenaltyAmount()))
                .toList();

        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getContact(),
                policyResponses
        );
    }

    // ── Helper — builds a CompanyPolicy row ───────────────────────────────
    private CompanyPolicy buildPolicy(
            Company company,
            String productCategory,
            DeliveryType deliveryType,
            MissedSlotAction missedSlotAction,
            CreateCompanyRequest request) {

        CompanyPolicy policy = new CompanyPolicy();
        policy.setCompany(company);
        policy.setProductCategory(productCategory);
        policy.setDeliveryType(deliveryType);
        policy.setMissedSlotAction(missedSlotAction);
        policy.setMaxReschedules(
                request.maxReschedules() != null ? request.maxReschedules() : 3
        );
        policy.setPenaltyAmount(request.penaltyAmount());
        policy.setPickupChecklist(request.pickupChecklist());

        return policy;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(company -> {
                    List<CompanyPolicy> policies = policyRepository.findByCompanyId(company.getId());
                    List<PolicyResponse> policyResponses = policies.stream()
                            .map(p -> new PolicyResponse(p.getProductCategory(), p.getDeliveryType(),
                                    p.getMissedSlotAction().name(), p.getMaxReschedules(), p.getPenaltyAmount()))
                            .toList();
                    return new CompanyResponse(company.getId(), company.getName(), company.getContact(), policyResponses);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        List<CompanyPolicy> policies = policyRepository.findByCompanyId(company.getId());
        List<PolicyResponse> policyResponses = policies.stream()
                .map(p -> new PolicyResponse(p.getProductCategory(), p.getDeliveryType(),
                        p.getMissedSlotAction().name(), p.getMaxReschedules(), p.getPenaltyAmount()))
                .toList();
        return new CompanyResponse(company.getId(), company.getName(), company.getContact(), policyResponses);
    }
}