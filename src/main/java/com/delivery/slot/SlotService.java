package com.delivery.slot;

import java.time.LocalDate;
import java.util.List;

public interface SlotService {

    SlotCapacity createSlot(CreateSlotRequest request);

    List<SlotCapacity> getSlots(Long companyId, LocalDate date);
}
