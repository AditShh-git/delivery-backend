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
import com.delivery.slot.SlotCapacity;
import com.delivery.slot.SlotCapacityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RiderRepository riderRepository;
    private final CompanyPolicyRepository policyRepository;
    private final SlotCapacityRepository slotCapacityRepository;

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

        OrderKpiProjection orderKpis = companyRepository.getCompanyOrderKpis(companyId);

        long totalOrders = safe(orderKpis.getTotalOrders());
        long totalDelivered = safe(orderKpis.getTotalDelivered());
        long totalFailed = safe(orderKpis.getTotalFailed());
        long slaBreached = safe(orderKpis.getSlaBreached());

        double successRate = totalOrders == 0
                ? 0
                : (totalDelivered * 100.0) / totalOrders;

        long activeRiders = riderRepository.countByCompanyIdAndIsOnDutyTrue(companyId);

        return new CompanyDashboardResponse(
                totalOrders,
                totalDelivered,
                totalFailed,
                Math.round(successRate * 100.0) / 100.0,
                slaBreached,
                activeRiders);
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
        company.setZone(request.zone());
        company.setDeliveryModel(request.deliveryModel());
        company.setStatus(CompanyStatus.ACTIVE);
        company.setOnboarded(true);

        companyRepository.save(company);

        createDefaultSlots(company);

        createPolicies(
                company,
                request.productCategories(),
                request.missedSlotAction(),
                request.maxReschedules(),
                request.penaltyAmount(),
                request.pickupChecklist()
        );

        log.info("Enterprise company created with id={}", company.getId());

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
                request.maxReschedules() != null ? request.maxReschedules() : 3);
        policy.setPenaltyAmount(request.penaltyAmount());
        policy.setPickupChecklist(request.pickupChecklist());

        return policy;
    }

    private void createDefaultSlots(Company company) {

        int defaultCapacity = 5; // you can later take from request

        List<String> slots = List.of("9AM-12PM", "12PM-3PM", "3PM-6PM");

        String zone = company.getZone();

        LocalDate today = LocalDate.now();

        List<SlotCapacity> slotCapacities = new ArrayList<>();

        for (int i = 0; i < 7; i++) {

            LocalDate date = today.plusDays(i);

            for (String slot : slots) {

                boolean exists = slotCapacityRepository
                        .existsByCompanyIdAndZoneAndSlotDateAndSlotLabel(
                                company.getId(),
                                zone,
                                date,
                                slot
                        );

                if (!exists) {
                    SlotCapacity sc = new SlotCapacity();
                    sc.setCompany(company);
                    sc.setZone(zone);
                    sc.setSlotDate(date);
                    sc.setSlotLabel(slot);
                    sc.setCapacity(defaultCapacity);
                    sc.setBookedCount(0);
                    sc.setCreatedAt(OffsetDateTime.now());
                    sc.setUpdatedAt(OffsetDateTime.now());

                    slotCapacities.add(sc);
                }
            }
        }

        slotCapacityRepository.saveAll(slotCapacities);

        log.info("Auto slots created for companyId={} zone={}", company.getId(), zone);
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

    private void createPolicies(
            Company company,
            List<String> categories,
            String missedSlotActionStr,
            Integer maxReschedules,
            BigDecimal penaltyAmount,
            Map<String, List<String>> pickupChecklist
    ) {

        MissedSlotAction missedSlotAction;

        try {
            missedSlotAction = missedSlotActionStr != null
                    ? MissedSlotAction.valueOf(missedSlotActionStr.trim().toUpperCase())
                    : MissedSlotAction.RESCHEDULE;
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid missedSlotAction value");
        }

        if (missedSlotAction == MissedSlotAction.CHARGE_FEE && penaltyAmount == null) {
            throw new ApiException("Penalty amount required when action is CHARGE_FEE");
        }

        List<CompanyPolicy> policies = new ArrayList<>();

        for (String category : categories) {

            String normalizedCategory = category.trim().toUpperCase();

            for (DeliveryType type : DeliveryType.values()) {

                CompanyPolicy policy = new CompanyPolicy();
                policy.setCompany(company);
                policy.setProductCategory(normalizedCategory);
                policy.setDeliveryType(type);
                policy.setMissedSlotAction(missedSlotAction);
                policy.setMaxReschedules(maxReschedules != null ? maxReschedules : 3);
                policy.setPenaltyAmount(penaltyAmount);
                policy.setPickupChecklist(pickupChecklist);

                policies.add(policy);
            }
        }

        policyRepository.saveAll(policies);
    }

    private CompanyResponse buildCompanyResponse(Company company) {

        List<CompanyPolicy> policies = policyRepository.findByCompanyId(company.getId());

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
                policyResponses);
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

        if (company.isOnboarded()) {
            throw new ApiException("Company already onboarded");
        }

        // Remove old policies if re-onboarding
        policyRepository.deleteByCompanyId(companyId);

        // Create new policies
        createPolicies(
                company,
                request.productCategories(),
                request.missedSlotAction(),
                request.maxReschedules(),
                request.penaltyAmount(),
                request.pickupChecklist()
        );

        // Set delivery model
        company.setDeliveryModel(request.deliveryModel());

        // Activate company
        company.setOnboarded(true);
        company.setStatus(CompanyStatus.ACTIVE);

        return buildCompanyResponse(company);
    }

    @Override
    public CompanyResponse onboardCompanyByEmail(String email, CompanyOnboardRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (user.getCompany() == null) {
            throw new ApiException("Authenticated user is not associated with any company.");
        }

        return onboardCompany(user.getCompany().getId(), request);
    }
}