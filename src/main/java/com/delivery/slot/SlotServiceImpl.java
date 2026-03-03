package com.delivery.slot;

import com.delivery.entity.Company;
import com.delivery.exception.ResourceNotFoundException;
import com.delivery.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotCapacityRepository slotRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public SlotCapacity createSlot(CreateSlotRequest request) {

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));

        SlotCapacity slot = new SlotCapacity();
        slot.setCompany(company);
        slot.setZone(request.zone());
        slot.setSlotDate(request.slotDate());
        slot.setSlotLabel(request.slotLabel());
        slot.setCapacity(request.capacity());
        slot.setBookedCount(0);

        SlotCapacity saved = slotRepository.save(slot);
        log.info("Slot created — company={}, zone={}, date={}, label={}",
                request.companyId(), request.zone(), request.slotDate(), request.slotLabel());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotCapacity> getSlots(Long companyId, LocalDate date) {
        return slotRepository.findByCompanyIdAndSlotDate(companyId, date);
    }
}
