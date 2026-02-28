package com.delivery.slot;

import com.delivery.entity.Company;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.CompanyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotCapacityRepository slotRepository;
    private final CompanyRepository      companyRepository;

    // Admin or Company creates a slot for a given date/zone
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    @PostMapping
    public ResponseEntity<SlotCapacity> createSlot(
            @Valid @RequestBody CreateSlotRequest request) {

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Company", request.companyId()));

        SlotCapacity slot = new SlotCapacity();
        slot.setCompany(company);
        slot.setZone(request.zone());
        slot.setSlotDate(request.slotDate());
        slot.setSlotLabel(request.slotLabel());
        slot.setCapacity(request.capacity());
        slot.setBookedCount(0);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slotRepository.save(slot));
    }

    // View all slots for a company on a given date
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    @GetMapping
    public ResponseEntity<List<SlotCapacity>> getSlots(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(
                slotRepository.findByCompanyIdAndSlotDate(companyId, date));
    }
}
