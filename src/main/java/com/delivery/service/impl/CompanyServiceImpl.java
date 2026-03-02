package com.delivery.service.impl;

import com.delivery.dto.request.CompanyOnboardRequest;
import com.delivery.dto.request.CreateCompanyRequest;
import com.delivery.dto.response.CompanyDashboardResponse;
import com.delivery.dto.response.CompanyResponse;
import com.delivery.dto.response.PolicyResponse;
import com.delivery.entity.*;
import com.delivery.exception.ApiException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.projection.OrderKpiProjection;
import com.delivery.repository.CompanyPolicyRepository;
import com.delivery.repository.CompanyRepository;
import com.delivery.repository.RiderRepository;
import com.delivery.repository.UserRepository;
import com.delivery.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final UserRepository userRepository;
    private final RiderRepository riderRepository;
    private final CompanyPolicyRepository policyRepository;

    @Override
    @Transactional(readOnly = true)
    public CompanyDashboardResponse getDashboardByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (user.getCompany() == null) {
            throw new ApiException("Authenticated user is not associated with any company.");
        }

        Long companyId = user.getCompany().getId();

        return getDashboard(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDashboardResponse getDashboard(Long companyId) {

        OrderKpiProjection orderKpis =
                companyRepository.getCompanyOrderKpis(companyId);

        long totalOrders = safe(orderKpis.getTotalOrders());
        long totalDelivered = safe(orderKpis.getTotalDelivered());
        long totalFailed = safe(orderKpis.getTotalFailed());
        long slaBreached = safe(orderKpis.getSlaBreached());

        double successRate = totalOrders == 0
                ? 0
                : (totalDelivered * 100.0) / totalOrders;

        long activeRiders =
                riderRepository.countByCompanyIdAndIsOnDutyTrue(companyId);

        return new CompanyDashboardResponse(
                totalOrders,
                totalDelivered,
                totalFailed,
                Math.round(successRate * 100.0) / 100.0,
                slaBreached,
                activeRiders
        );
    }
    private long safe(Long value) {
        return value == null ? 0L : value;
    }



    @Override
    public CompanyResponse createEnterpriseCompany(CreateCompanyRequest request) {

        log.info("Creating enterprise company: {}", request.name());

        String normalizedName = request.name().trim();
        String normalizedContact = request.contact().trim();
        String normalizedEmail = request.email().trim().toLowerCase();

        if (companyRepository.existsByName(normalizedName)) {
            throw new ApiException("Company already exists with that name");
        }

        if (companyRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException("Email already registered");
        }

        Company company = new Company();
        company.setName(normalizedName);
        company.setContact(normalizedContact);
        company.setEmail(normalizedEmail);
        company.setDeliveryModel(request.deliveryModel());  // REQUIRED in request
        company.setStatus(CompanyStatus.ACTIVE);            // Enterprise auto-active

        companyRepository.save(company);

        log.info("Enterprise company created with id={}", company.getId());

        createPolicies(company, request);

        return buildCompanyResponse(company);
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
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public CompanyResponse updateCompanyStatus(Long id, CompanyStatus status) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));

        company.setStatus(status);

        log.info("Company {} status updated to {}", id, status);

        return buildCompanyResponse(company);
    }

    private void createPolicies(Company company, CreateCompanyRequest request) {

        String productCategory = request.productCategory() != null
                ? request.productCategory().trim().toUpperCase()
                : "DEFAULT";

        MissedSlotAction missedSlotAction;
        try {
            missedSlotAction = request.missedSlotAction() != null
                    ? MissedSlotAction.valueOf(request.missedSlotAction().trim().toUpperCase())
                    : MissedSlotAction.RESCHEDULE;
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid missedSlotAction value");
        }

        if (missedSlotAction == MissedSlotAction.CHARGE_FEE
                && request.penaltyAmount() == null) {
            throw new ApiException("Penalty amount required when action is CHARGE_FEE");
        }

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
        }

        policyRepository.saveAll(policies);
    }

    private CompanyResponse buildCompanyResponse(Company company) {

        List<CompanyPolicy> policies =
                policyRepository.findByCompanyId(company.getId());

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

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponse> getCompanies(int page,
                                              int size,
                                              CompanyStatus status,
                                              String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Company> companies;

        if (status != null && search != null && !search.isBlank()) {
            companies = companyRepository
                    .findByStatusAndNameContainingIgnoreCase(status, search, pageable);

        } else if (status != null) {
            companies = companyRepository
                    .findByStatus(status, pageable);

        } else if (search != null && !search.isBlank()) {
            companies = companyRepository
                    .findByNameContainingIgnoreCase(search, pageable);

        } else {
            companies = companyRepository.findAll(pageable);
        }

        return companies.map(this::buildCompanyResponse);
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

    @Override
    public CompanyResponse onboardCompany(Long companyId,
                                          CompanyOnboardRequest request) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new ApiException("Company must be ACTIVE before onboarding.");
        }

        // Set delivery model
        company.setDeliveryModel(request.deliveryModel());

        // Remove old policies if re-onboarding
        policyRepository.deleteByCompanyId(companyId);

        for (String category : request.productCategories()) {

            CreateCompanyRequest tempRequest = new CreateCompanyRequest(
                    company.getName(),
                    company.getContact(),
                    company.getEmail(),
                    request.deliveryModel(),
                    request.missedSlotAction(),
                    request.maxReschedules(),
                    request.penaltyAmount(),
                    request.pickupChecklist(),
                    category
            );

            createPolicies(company, tempRequest);
        }

        return buildCompanyResponse(company);
    }
}