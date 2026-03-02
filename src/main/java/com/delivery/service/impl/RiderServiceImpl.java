package com.delivery.service.impl;

import com.delivery.dto.request.CreateRiderRequest;
import com.delivery.dto.request.UpdatePasswordRequest;
import com.delivery.dto.request.UpdateRiderRequest;
import com.delivery.dto.response.RiderResponse;
import com.delivery.entity.Company;
import com.delivery.entity.Rider;
import com.delivery.entity.Role;
import com.delivery.entity.User;
import com.delivery.exception.ApiException;
import com.delivery.exception.EmailAlreadyExistsException;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.CompanyRepository;
import com.delivery.repository.RiderRepository;
import com.delivery.repository.RoleRepository;
import com.delivery.repository.UserRepository;
import com.delivery.service.RiderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiderServiceImpl implements RiderService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RiderRepository riderRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── CREATE RIDER ──────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public RiderResponse createRider(CreateRiderRequest request,
            Long creatorUserId,
            String creatorRole) {

        log.info("Creating rider. CreatorUserId={}, CreatorRole={}, Email={}",
                creatorUserId, creatorRole, request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Email already exists: {}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        if (userRepository.existsByPhone(request.phone())) {
            log.warn("Phone already registered: {}", request.phone());
            throw new ApiException("Phone already registered");
        }

        Company company;

        if ("COMPANY".equals(creatorRole)) {

            log.debug("Creator is COMPANY user. Fetching company profile.");

            User companyUser = userRepository.findById(creatorUserId)
                    .orElseThrow(() -> {
                        log.error("Company user not found: {}", creatorUserId);
                        return new ResourceNotFoundException("User", creatorUserId);
                    });

            company = companyUser.getCompany();

            if (company == null) {
                log.error("Company user has no company profile. UserId={}", creatorUserId);
                throw new ApiException("Company user has no company profile");
            }

        } else { // ADMIN

            log.debug("Creator is ADMIN. companyId={}", request.companyId());

            if (request.companyId() == null) {
                log.error("ADMIN tried creating rider without companyId");
                throw new ApiException("companyId is required for ADMIN");
            }

            company = companyRepository.findById(request.companyId())
                    .orElseThrow(() -> {
                        log.error("Company not found: {}", request.companyId());
                        return new ResourceNotFoundException("Company", request.companyId());
                    });
        }

        log.debug("Assigning RIDER role");

        Role riderRole = roleRepository.findByName("RIDER")
                .orElseThrow(() -> {
                    log.error("RIDER role not found in DB");
                    return new ApiException("RIDER role not found");
                });

        User riderUser = new User();
        riderUser.setFullName(request.fullName());
        riderUser.setEmail(request.email());
        riderUser.setPhone(request.phone());
        riderUser.setPassword(passwordEncoder.encode(request.password()));
        riderUser.setCompany(company);
        riderUser.getRoles().add(riderRole);

        userRepository.save(riderUser);

        log.info("User entity created for rider. UserId={}", riderUser.getId());

        Rider rider = new Rider();
        rider.setUser(riderUser);
        rider.setCompany(company);
        rider.setVehicleType(request.vehicleType());
        rider.setLicensePlate(request.licensePlate());
        rider.setZone(request.zone());

        // ── New capacity model — rider starts OFF duty ────────────────────
        // Admin must call PATCH /riders/{id}/duty?onDuty=true to activate
        rider.setIsOnDuty(false);
        rider.setActiveOrderCount(0);
        rider.setMaxConcurrentOrders(1); // default 1; admin changes for PARCEL riders
        rider.setIsAvailable(false); // false until admin marks on duty

        riderRepository.save(rider);

        log.info("Rider profile created successfully. RiderId={}", rider.getId());

        return new RiderResponse(
                rider.getId(),
                riderUser.getFullName(),
                riderUser.getEmail(),
                company.getName(),
                rider.getIsAvailable());
    }

    // ─── UPDATE RIDER ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public RiderResponse updateRider(Long riderId,
            UpdateRiderRequest request,
            Long userId,
            String role) {

        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", riderId));

        // Rider can only update their own profile
        if ("RIDER".equals(role)) {
            if (!rider.getUser().getId().equals(userId)) {
                throw new ApiException("You can only update your own profile.");
            }
        }

        // COMPANY can only update riders in their company
        if ("COMPANY".equals(role)) {
            Company company = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId))
                    .getCompany();

            if (company == null || !rider.getCompany().getId().equals(company.getId())) {
                throw new ApiException("You cannot update another company rider.");
            }
        }

        if (request.vehicleType() != null)
            rider.setVehicleType(request.vehicleType());
        if (request.licensePlate() != null)
            rider.setLicensePlate(request.licensePlate());
        if (request.zone() != null)
            rider.setZone(request.zone());

        // NOTE: isAvailable is no longer set directly — use /duty endpoint instead
        // Kept here for backward compatibility only
        if (request.isAvailable() != null) {
            log.warn("Direct isAvailable update on rider {} — prefer /duty endpoint", riderId);
            rider.setIsAvailable(request.isAvailable());
        }

        return new RiderResponse(
                rider.getId(),
                rider.getUser().getFullName(),
                rider.getUser().getEmail(),
                rider.getCompany().getName(),
                rider.getIsAvailable());
    }

    // ─── UPDATE PASSWORD ───────────────────────────────────────────────────

    @Override
    @Transactional
    public void updatePassword(Long riderId,
            UpdatePasswordRequest request,
            Long userId,
            String role) {

        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", riderId));

        if ("RIDER".equals(role)) {
            if (!rider.getUser().getId().equals(userId)) {
                throw new ApiException("You can only update your own password.");
            }
        }

        User user = rider.getUser();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new ApiException("Old password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        log.info("Password updated for riderId={}", riderId);
    }

    // ─── SET DUTY STATUS ───────────────────────────────────────────────────
    // Called by PATCH /api/company/riders/{id}/duty?onDuty=true&maxOrders=1
    // This is the ONLY thing admin needs to do per rider per day.
    // Everything else (capacity, availability) is managed automatically.

    @Override
    @Transactional
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public RiderResponse setDutyStatus(Long riderId,
            boolean onDuty,
            int maxConcurrentOrders) {

        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", riderId));

        rider.setIsOnDuty(onDuty);
        rider.setMaxConcurrentOrders(maxConcurrentOrders);

        if (!onDuty) {
            // Rider going off duty — reset counters, mark unavailable
            rider.setActiveOrderCount(0);
            rider.setIsAvailable(false);
            log.info("Rider {} is now OFF DUTY", riderId);
        } else {
            // Rider going on duty — availability depends on current active orders
            rider.setIsAvailable(rider.canAcceptOrder());
            log.info("Rider {} is now ON DUTY (maxOrders: {})", riderId, maxConcurrentOrders);
        }

        return new RiderResponse(
                rider.getId(),
                rider.getUser().getFullName(),
                rider.getUser().getEmail(),
                rider.getCompany().getName(),
                rider.getIsAvailable());
    }
}