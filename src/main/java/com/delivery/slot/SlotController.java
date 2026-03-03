package com.delivery.slot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

        private final SlotService slotService;

        @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
        @PostMapping
        public ResponseEntity<SlotCapacity> createSlot(
                        @Valid @RequestBody CreateSlotRequest request) {

                log.info("POST /api/slots — companyId={}, zone={}, date={}, label={}",
                                request.companyId(), request.zone(), request.slotDate(), request.slotLabel());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(slotService.createSlot(request));
        }

        @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
        @GetMapping
        public ResponseEntity<List<SlotCapacity>> getSlots(
                        @RequestParam Long companyId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                log.info("GET /api/slots — companyId={}, date={}", companyId, date);
                return ResponseEntity.ok(slotService.getSlots(companyId, date));
        }
}
